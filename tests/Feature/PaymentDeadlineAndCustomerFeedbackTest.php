<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Resources\Orders\OrderResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Actions\ConfirmCompletionAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Jobs\AutoCloseConfirmedOrders;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Pricing\Actions\SubmitProposalAction;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Enums\CustomerRatingTag;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Tests\TestCase;

final class PaymentDeadlineAndCustomerFeedbackTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
    }

    #[Test]
    public function delayed_cash_payment_keeps_the_full_confirmation_window_and_closes_at_the_displayed_deadline(): void
    {
        $order = $this->cashOrder();
        $this->payment($order, now()->subDays(2));
        $paidAt = now()->subHour();
        $this->payment($order, $paidAt);
        $this->payment($order, now(), PaymentStatus::Failed);
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->getJson("/api/v1/orders/{$order->id}")->assertOk()
            ->assertJsonPath('data.deadlines.auto_close_at', $paidAt->addHours(24)->toIso8601String());
        $this->assertSame(0, $this->runAutoClose());
        $this->assertSame(OrderStatus::AwaitingConfirmation, $order->refresh()->status);
        $this->travel(23)->hours();
        $this->assertSame(1, $this->runAutoClose());
        $this->assertSame(OrderStatus::Closed, $order->refresh()->status);
        $this->assertSame(0, $this->runAutoClose());
    }

    #[Test]
    public function system_rechecks_payment_time_under_the_order_lock(): void
    {
        $order = $this->cashOrder();
        $this->payment($order, now()->subHour());
        try {
            app(ConfirmCompletionAction::class)->execute($order, ActorType::System);
            $this->fail('The confirmation window must remain open.');
        } catch (BusinessRuleViolationException) {
            $this->assertSame(OrderStatus::AwaitingConfirmation, $order->refresh()->status);
            $this->assertNull($order->closed_at);
        }
        app(ConfirmCompletionAction::class)->execute($order, ActorType::Customer, $order->customer_id);
        $this->assertSame(OrderStatus::Closed, $order->refresh()->status);
    }

    #[Test]
    public function missing_or_failed_payment_is_not_a_reason_to_auto_close(): void
    {
        $missing = $this->cashOrder();
        $failed = $this->cashOrder();
        $this->payment($failed, now()->subDays(2), PaymentStatus::Failed);
        $this->assertSame(0, $this->runAutoClose());
        foreach ([$missing, $failed] as $order) {
            $this->assertSame(OrderStatus::AwaitingConfirmation, $order->refresh()->status);
        }
    }

    #[Test]
    public function provider_feedback_persists_stable_tags_and_comment_once_without_showing_it_to_the_customer(): void
    {
        [$order, $provider] = $this->ratedOrder();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER');
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()
            ->assertJsonPath('data.customer_rating_tag_options.0.code', CustomerRatingTag::Cooperative->value)
            ->assertJsonPath('data.customer_rating_tag_options.0.label', CustomerRatingTag::Cooperative->getLabel());
        $this->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", [
            'stars' => 4, 'tags' => ['COOPERATIVE', 'PUNCTUAL'], 'comment' => '  العميل متعاون وملتزم بالموعد  ',
        ])->assertCreated()->assertJsonPath('data.tags', ['COOPERATIVE', 'PUNCTUAL'])
            ->assertJsonPath('data.comment', 'العميل متعاون وملتزم بالموعد');
        $rating = $order->customerRating()->sole();
        $this->assertSame(['COOPERATIVE', 'PUNCTUAL'], $rating->tags);
        $this->assertSame('العميل متعاون وملتزم بالموعد', $rating->comment);
        $this->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", ['stars' => 1])->assertUnprocessable();
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER');
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonMissingPath('data.customer_rating')
            ->assertDontSee('العميل متعاون وملتزم بالموعد');
        $this->getJson('/api/v1/me')->assertOk()->assertJsonMissingPath('user.rating_avg')->assertDontSee('العميل متعاون وملتزم بالموعد');
        $this->actingAs(Admin::factory()->super()->create(), 'admin')
            ->get(OrderResource::getUrl('view', ['record' => $order]))
            ->assertOk()->assertSee('العميل متعاون وملتزم بالموعد')->assertSee('تعاون أثناء الزيارة');
    }

    #[Test]
    public function stars_only_remains_supported_and_tag_options_come_from_the_localized_server_config(): void
    {
        [$order, $provider] = $this->ratedOrder();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", ['stars' => 5])->assertCreated()
            ->assertJsonPath('data.tags', [])->assertJsonPath('data.comment', null);
        $this->getJson('/api/v1/config')->assertOk()
            ->assertJsonPath('option_lists.customer_rating_tags.0.code', 'COOPERATIVE');
    }

    #[Test]
    #[TestWith([['UNKNOWN'], null, 'tags.0'])]
    #[TestWith([['COOPERATIVE', 'COOPERATIVE'], null, 'tags.0'])]
    #[TestWith([[], 'LONG', 'comment'])]
    public function invalid_feedback_does_not_create_a_partial_rating(array $tags, ?string $comment, string $field): void
    {
        [$order, $provider] = $this->ratedOrder();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", [
                'stars' => 4, 'tags' => $tags, 'comment' => $comment === 'LONG' ? str_repeat('س', 501) : $comment,
            ])->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => [$field]]]);
        $this->assertDatabaseCount('customer_ratings', 0);
    }

    #[Test]
    public function another_provider_and_a_late_review_cannot_submit_feedback(): void
    {
        [$order, $provider] = $this->ratedOrder();
        $other = ProviderProfile::factory()->create();
        $this->actingAs($other->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", ['stars' => 4, 'comment' => 'غير مسموح'])->assertUnprocessable()->assertJsonPath('error.rule', 'BR-091');
        $order->update(['closed_at' => now()->subDays(8)]);
        $this->actingAs($provider->user, 'sanctum')
            ->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", ['stars' => 4, 'comment' => 'انتهت المهلة'])->assertUnprocessable();
        $this->assertDatabaseCount('customer_ratings', 0);
    }

    #[Test]
    #[TestWith([false])]
    #[TestWith([true])]
    public function execution_quote_deadline_explains_the_real_inspection_outcome(bool $paid): void
    {
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Arrived)->create([
            'provider_profile_id' => $provider->id,
        ]);
        if ($paid) {
            $offer = Offer::query()->create(['order_id' => $order->id, 'provider_profile_id' => $provider->id, 'price' => '75.00', 'source' => 'PROVIDER', 'status' => 'ACCEPTED', 'submitted_at' => now()]);
            $order->update(['accepted_offer_id' => $offer->id]);
        }
        $order->problemType->update(['employee_price_min' => 300, 'employee_price_max' => 700]);
        $proposal = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::ExecutionQuote, '500.00', 'إصلاح تسريب أسفل الحوض');
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->getJson("/api/v1/orders/{$order->id}/proposals")->assertOk()
            ->assertJsonPath('data.0.expires_at', $proposal->expires_at->toIso8601String())
            ->assertJsonPath('data.0.server_time', now()->toIso8601String())
            ->assertJsonPath('data.0.expiry_message', Localization::text($paid ? 'copy.proposal_paid_inspection_expiry' : 'copy.proposal_free_inspection_expiry'));
    }

    private function cashOrder(): Order
    {
        return Order::factory()->status(OrderStatus::AwaitingConfirmation)->create([
            'provider_profile_id' => ProviderProfile::factory(), 'completed_at' => now()->subDays(3),
            'payment_method' => PaymentMethod::Cash, 'payment_status' => OrderPaymentStatus::Paid,
            'labor_total' => '500.00', 'final_amount' => '500.00',
        ]);
    }

    private function payment(Order $order, \DateTimeInterface $paidAt, PaymentStatus $status = PaymentStatus::Succeeded): void
    {
        $order->payments()->create(['merchant_ref' => fake()->uuid(), 'method' => PaymentMethod::Cash, 'status' => $status, 'amount' => '500.00', 'paid_at' => $paidAt]);
    }

    private function runAutoClose(): int
    {
        return app(AutoCloseConfirmedOrders::class)->handle(app(ConfirmCompletionAction::class), app(SettingsRepository::class));
    }

    /** @return array{Order, ProviderProfile} */
    private function ratedOrder(): array
    {
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Closed)->create(['provider_profile_id' => $provider->id, 'closed_at' => now()]);

        return [$order, $provider];
    }
}
