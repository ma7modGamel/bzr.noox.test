<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Communication\Enums\ConversationStatus;
use App\Modules\Communication\Models\Conversation;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use App\Modules\Settlements\Models\ProviderPayout;
use App\Modules\Settlements\Models\ProviderRemittance;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ProviderWorkspaceApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function provider_workspace_requires_authentication(): void
    {
        $this->getJson('/api/v1/provider/profile')->assertUnauthorized();
        $this->getJson('/api/v1/provider/earnings')->assertUnauthorized();
        $this->postJson('/api/v1/provider/portfolio')->assertUnauthorized();
    }

    #[Test]
    public function provider_can_update_only_editable_profile_fields_from_backend_catalog(): void
    {
        $order = Order::factory()->create();
        $profile = ProviderProfile::factory()->create([
            'rating_avg' => '4.80',
            'completed_orders_count' => 86,
            'avg_response_minutes' => 12,
        ]);
        $profile->categories()->sync([$order->category_id]);
        $profile->specialties()->sync([$order->problem_type_id]);
        $profile->areas()->sync([$order->area_id]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->patchJson('/api/v1/provider/profile', [
                'experience_years' => 12,
                'bio' => 'خبرة في إصلاح التسريبات والصيانة المنزلية.',
                'specialty_ids' => [$order->problem_type_id],
                'area_ids' => [$order->area_id],
                'category_ids' => [],
                'payout_method' => 'BANK',
            ])
            ->assertOk()
            ->assertJsonPath('data.experience_years', 12)
            ->assertJsonPath('data.specialty_ids.0', $order->problem_type_id)
            ->assertJsonPath('data.area_ids.0', $order->area_id)
            ->assertJsonPath('data.rating_avg', '4.80')
            ->assertJsonPath('data.completed_orders', 86)
            ->assertJsonPath('data.avg_response_minutes', 12)
            ->assertJsonPath('data.available_actions.0', 'update_provider_profile');

        $this->assertSame('INSTAPAY', $profile->refresh()->payout_method);
        $this->assertSame([$order->category_id], $profile->categories()->pluck('categories.id')->all());
    }

    #[Test]
    public function provider_profile_rejects_specialty_outside_existing_categories(): void
    {
        $order = Order::factory()->create();
        $profile = ProviderProfile::factory()->create();
        $profile->categories()->sync([$order->category_id]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->patchJson('/api/v1/provider/profile', [
                'experience_years' => 8,
                'bio' => null,
                'specialty_ids' => [$order->problem_type_id + 1000],
                'area_ids' => [$order->area_id],
            ])
            ->assertUnprocessable()
            ->assertJsonPath('error.fields.specialty_ids.0', 'أحد التخصصات لا يتبع فئات الفني.');
    }

    #[Test]
    public function provider_can_claim_owned_image_for_portfolio_and_only_owner_can_delete_it(): void
    {
        Storage::fake('local');
        $profile = ProviderProfile::factory()->create();
        $other = ProviderProfile::factory()->create();
        Storage::disk('local')->put('order-media/work.jpg', 'image');
        $media = OrderMedia::query()->create([
            'uploaded_by' => $profile->user_id,
            'expires_at' => now()->addDay(),
            'type' => 'IMAGE',
            'path' => 'order-media/work.jpg',
            'size_bytes' => 128,
        ]);

        $itemId = $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson('/api/v1/provider/portfolio', ['media_id' => $media->getKey(), 'caption' => 'إصلاح حمام'])
            ->assertCreated()
            ->assertJsonPath('data.caption', 'إصلاح حمام')
            ->json('data.id');

        $this->assertDatabaseMissing('order_media', ['id' => $media->getKey()]);
        $this->actingAs($other->user, 'sanctum')
            ->deleteJson("/api/v1/provider/portfolio/{$itemId}")
            ->assertNotFound();
        $this->actingAs($profile->user, 'sanctum')
            ->deleteJson("/api/v1/provider/portfolio/{$itemId}")
            ->assertNoContent();
        $this->assertDatabaseMissing('portfolio_items', ['id' => $itemId]);
        Storage::disk('local')->assertMissing('order-media/work.jpg');
    }

    #[Test]
    public function employee_earnings_return_calculated_balance_available_pending_and_transactions(): void
    {
        $profile = ProviderProfile::factory()->create();
        Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $profile->getKey(), 'payment_method' => PaymentMethod::Cash,
            'labor_total' => '650.00', 'materials_total' => '200.00', 'final_amount' => '850.00',
            'commission_amount' => '650.00', 'closed_at' => now()->subDays(2),
            'settlement_eligible_at' => now()->subDay(),
        ]);
        Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $profile->getKey(), 'payment_method' => PaymentMethod::Electronic,
            'labor_total' => '450.00', 'materials_total' => '100.00', 'final_amount' => '550.00',
            'commission_amount' => '450.00', 'closed_at' => now()->subHour(),
            'settlement_eligible_at' => now()->addDay(),
        ]);
        $admin = Admin::factory()->create();
        ProviderPayout::query()->create([
            'provider_profile_id' => $profile->getKey(), 'amount' => '50.00', 'method' => 'INSTAPAY',
            'reference' => 'PAY-1', 'paid_at' => now()->subMinutes(20), 'admin_id' => $admin->getKey(),
        ]);
        ProviderRemittance::query()->create([
            'provider_profile_id' => $profile->getKey(), 'amount' => '300.00', 'method' => 'CASH',
            'reference' => 'REM-1', 'received_at' => now()->subMinutes(10), 'admin_id' => $admin->getKey(),
        ]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/earnings')
            ->assertOk()
            ->assertJsonPath('data.operating_mode', 'EMPLOYEE')
            ->assertJsonPath('data.summary.balance', '-300.00')
            ->assertJsonPath('data.summary.available', '-400.00')
            ->assertJsonPath('data.summary.pending', '100.00')
            ->assertJsonCount(4, 'data.transactions')
            ->assertJsonPath('data.transactions.0.type', 'REMITTANCE');
    }

    #[Test]
    public function marketplace_earnings_use_net_electronic_amount_and_cash_commission(): void
    {
        app(SettingsRepository::class)->set(Cfg::OffersEnabled, true);
        $profile = ProviderProfile::factory()->create();
        Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $profile->getKey(),
            'operating_mode' => OperatingMode::Marketplace,
            'payment_method' => PaymentMethod::Cash,
            'labor_total' => '700.00',
            'materials_total' => '100.00',
            'final_amount' => '800.00',
            'commission_amount' => '80.00',
            'closed_at' => now()->subDays(2),
            'settlement_eligible_at' => now()->subDay(),
        ]);
        Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $profile->getKey(),
            'operating_mode' => OperatingMode::Marketplace,
            'payment_method' => PaymentMethod::Electronic,
            'labor_total' => '900.00',
            'materials_total' => '100.00',
            'final_amount' => '1000.00',
            'commission_amount' => '100.00',
            'refunded_total' => '100.00',
            'closed_at' => now()->subDay(),
            'settlement_eligible_at' => now()->subHour(),
        ]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/earnings')
            ->assertOk()
            ->assertJsonPath('data.operating_mode', 'MARKETPLACE')
            ->assertJsonPath('data.summary.balance', '720.00')
            ->assertJsonPath('data.summary.available', '720.00')
            ->assertJsonPath('data.summary.pending', '0.00')
            ->assertJsonCount(2, 'data.transactions');
    }

    #[Test]
    public function provider_conversation_exposes_customer_as_counterpart(): void
    {
        $profile = ProviderProfile::factory()->create();
        $customer = User::factory()->create(['name' => 'أحمد محمود', 'customer_rating_avg' => '4.80']);
        $order = Order::factory()->status(OrderStatus::Confirmed)->create([
            'customer_id' => $customer->getKey(), 'provider_profile_id' => $profile->getKey(),
        ]);
        Conversation::query()->create([
            'order_id' => $order->getKey(), 'customer_id' => $customer->getKey(),
            'provider_profile_id' => $profile->getKey(), 'status' => ConversationStatus::Open,
        ]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/conversations')
            ->assertOk()
            ->assertJsonPath('data.0.customer.name', 'أحمد محمود')
            ->assertJsonPath('data.0.customer.rating_avg', '4.80');
    }
}
