<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Content\Actions\PublishLegalPageVersionAction;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Enums\NotificationCode;
use App\Modules\Notifications\Services\Notifier;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\LegalPagesSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\SupportReasonSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\Support\RecordsMobileResponses;
use Tests\TestCase;

/**
 * 9أ المرحلة 3 (DEC-061) — ردود الدفع الإلكتروني (C22) والتقييم (C24) والنزاع والبلاغ (C27/C28) والمساعدة (C29)
 * والإشعارات (C32) والحساب (C02/C33) والشروط (C34) كما يقرؤها التطبيقان.
 * ومع RECORD_API_RESPONSES=1 تُكتب إلى androidapp/app/src/test/resources/api/customer/.
 */
final class MobileCustomerAccountResponsesTest extends TestCase
{
    use RecordsMobileResponses;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, SupportReasonSeeder::class, TermsSeeder::class]);
        app(SettingsRepository::class)->set(Cfg::ChannelFawryEnabled, true);
    }

    #[Test]
    public function ردود_الدفع_والدعم_والحساب(): void
    {
        $customer = User::factory()->create(['name' => 'منى حسن', 'phone' => '01011111111']);
        $provider = ProviderProfile::factory()->create();

        $awaiting = $this->order($customer, $provider, OrderStatus::AwaitingPayment, [
            'payment_method' => PaymentMethod::Electronic, 'payment_status' => OrderPaymentStatus::Unpaid,
        ]);
        $this->recordResponse('customer', 'payment_created', $this->as($customer)->postJson("/api/v1/orders/{$awaiting->id}/payments", [
            'channel' => 'KIOSK', 'simulation' => 'PENDING', 'expected_version' => $awaiting->version,
        ])->assertCreated()->assertJsonStructure(['data' => ['id', 'status', 'amount', 'channel', 'reference_number', 'expires_at']]));

        $closed = $this->order($customer, $provider, OrderStatus::Closed, ['closed_at' => now()->subDay()]);
        $this->recordResponse('customer', 'review_submitted', $this->as($customer)->postJson("/api/v1/orders/{$closed->id}/review", [
            'quality' => 5, 'punctuality' => 4, 'conduct' => 5, 'comment' => null,
        ])->assertCreated());

        $arrived = $this->order($customer, $provider, OrderStatus::Arrived);
        $this->recordResponse('customer', 'dispute_opened', $this->as($customer)->postJson("/api/v1/orders/{$arrived->id}/disputes", [
            'reason_code' => 'WORK_QUALITY', 'description' => 'التنفيذ لم يكتمل كما اتفقنا.', 'media_ids' => [],
        ])->assertCreated()->assertJsonStructure(['data' => ['id', 'reason_code', 'reason_label', 'description', 'status', 'status_label', 'attachments']]));
        $this->recordResponse('customer', 'disputes', $this->as($customer)->getJson("/api/v1/orders/{$arrived->id}/disputes")->assertOk());

        $confirmed = $this->order($customer, $provider, OrderStatus::Confirmed);
        $this->recordResponse('customer', 'provider_report', $this->as($customer)->postJson("/api/v1/providers/{$provider->id}/reports", [
            'order_id' => $confirmed->id, 'reason_code' => 'OFF_PLATFORM_REQUEST', 'description' => null,
        ])->assertCreated());

        $this->recordResponse('customer', 'faqs', $this->getJson('/api/v1/support/faqs')->assertOk()
            ->assertJsonStructure(['data' => [['code', 'title', 'body']]]));
        $this->as($customer)->postJson('/api/v1/support/messages', ['subject' => 'استفسار', 'message' => 'كيف أغير رقم الهاتف؟'])->assertSuccessful();

        app(Notifier::class)->send($customer, new NotificationContent(
            NotificationCode::ProviderArrived, 'وصل الفني', 'الفني عند العنوان الآن.', "orders/{$arrived->id}", NotificationContent::CUSTOMER, $arrived->id,
        ));
        $notifications = $this->as($customer)->getJson('/api/v1/notifications')->assertOk()
            ->assertJsonStructure(['data' => [['id', 'title', 'body', 'deep_link', 'read_at', 'created_at']]]);
        $this->recordResponse('customer', 'notifications', $notifications);
        $this->as($customer)->postJson('/api/v1/notifications/read', ['notification_ids' => [$notifications->json('data.0.id')]])->assertSuccessful();

        $this->recordResponse('customer', 'me_updated', $this->as($customer)->patchJson('/api/v1/me', ['rating_reminders_enabled' => false])
            ->assertOk()->assertJsonPath('user.rating_reminders_enabled', false));
        $this->seed(LegalPagesSeeder::class);
        app(PublishLegalPageVersionAction::class)->execute(
            LegalPage::query()->where('slug', 'terms')->sole()->versions()->sole(), Admin::factory()->super()->create(),
        );
        $this->recordResponse('customer', 'terms', $this->getJson('/api/v1/terms/current')->assertOk()
            ->assertJsonStructure(['data' => ['version', 'body', 'effective_at']]));
    }

    /** @param array<string, mixed> $attributes */
    private function order(User $customer, ProviderProfile $provider, OrderStatus $status, array $attributes = []): Order
    {
        return Order::factory()->status($status)->create([
            'customer_id' => $customer->id, 'provider_profile_id' => $provider->id,
            'labor_total' => '500.00', 'materials_total' => '200.00', 'final_amount' => '700.00', 'commission_rate' => '1.0000',
            ...$attributes,
        ]);
    }

    private function as(User $user): static
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER']);
    }
}
