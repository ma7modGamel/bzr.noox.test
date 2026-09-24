<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Resources\SupportReasons\SupportReasonResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Enums\SupportReasonType;
use App\Modules\Support\Models\ProviderReport;
use App\Modules\Support\Models\SupportReason;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\SupportReasonSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** عقد القوائم المدارة من الخادم وواجهتا C27/C28. */
final class SupportReasonsApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, SupportReasonSeeder::class]);
    }

    #[Test]
    public function config_يعيد_قوائم_الأعمال_بأكوادها_وتسمياتها_من_الخادم(): void
    {
        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonPath('option_lists.dispute_reasons.0.code', 'WORK_QUALITY')
            ->assertJsonPath('option_lists.dispute_reasons.0.label', 'جودة التنفيذ غير مرضية')
            ->assertJsonPath('option_lists.provider_report_reasons.0.code', 'INAPPROPRIATE_BEHAVIOR')
            ->assertJsonPath('option_lists.customer_cancellation_reasons.0.code', 'FOUND_ANOTHER')
            ->assertJsonPath('option_lists.payment_methods.0.code', 'CASH');

        SupportReason::query()
            ->where('type', SupportReasonType::Dispute)
            ->where('code', 'WORK_QUALITY')
            ->update(['label' => 'جودة العمل تحتاج مراجعة']);

        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonPath('option_lists.dispute_reasons.0.label', 'جودة العمل تحتاج مراجعة');
    }

    #[Test]
    public function مدير_التشغيل_يفتح_صفحات_إضافة_وتعديل_أسباب_الدعم(): void
    {
        $this->actingAs(Admin::factory()->super()->create(), 'admin');
        $reason = SupportReason::query()->firstOrFail();

        $this->get(SupportReasonResource::getUrl('index'))->assertOk();
        $this->get(SupportReasonResource::getUrl('create'))->assertOk();
        $this->get(SupportReasonResource::getUrl('edit', ['record' => $reason]))->assertOk();
    }

    #[Test]
    public function السبب_المعطل_يختفي_من_config_ولا_يقبل_في_فتح_مشكلة(): void
    {
        $reason = SupportReason::query()
            ->where('type', SupportReasonType::Dispute)
            ->where('code', 'WORK_QUALITY')
            ->sole();
        $reason->update(['is_active' => false]);

        $customer = User::factory()->create();
        $order = Order::factory()->status(OrderStatus::Arrived)->create(['customer_id' => $customer->id]);

        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonMissing(['code' => 'WORK_QUALITY']);

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/disputes", [
                'reason_code' => 'WORK_QUALITY',
                'description' => 'وصف كاف للمشكلة.',
            ])
            ->assertUnprocessable()
            ->assertJsonPath('error.code', 'VALIDATION_FAILED')
            ->assertJsonPath('error.fields.reason_code.0', fn (string $message): bool => $message !== '');
    }

    #[Test]
    public function العميل_يفتح_مشكلة_بسبب_فعال_وصور_مملوكة_له(): void
    {
        Storage::fake('local');
        $customer = User::factory()->create();
        $order = Order::factory()->status(OrderStatus::Arrived)->create(['customer_id' => $customer->id]);

        $mediaId = $this->actingAs($customer, 'sanctum')
            ->post('/api/v1/media', [
                'file' => UploadedFile::fake()->image('damage.jpg', 400, 300),
            ], ['Accept' => 'application/json'])
            ->assertCreated()
            ->json('media.id');

        $response = $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/disputes", [
                'reason_code' => 'PROPERTY_DAMAGE',
                'description' => 'حدث تلف ظاهر بعد تنفيذ الخدمة.',
                'media_ids' => [$mediaId],
            ])
            ->assertCreated()
            ->assertJsonPath('data.reason_label', 'تلف في الممتلكات')
            ->assertJsonPath('data.status', 'OPEN')
            ->assertJsonPath('data.attachments.0.media_id', $mediaId);

        $this->assertSame(OrderStatus::Disputed, $order->fresh()->status);
        $this->assertDatabaseHas('dispute_attachments', [
            'dispute_id' => $response->json('data.id'),
            'order_media_id' => $mediaId,
        ]);

        $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}/disputes")
            ->assertOk()
            ->assertJsonPath('data.0.reason_code', 'PROPERTY_DAMAGE');
    }

    #[Test]
    public function بلاغ_الفني_يقبل_سببًا_فعالًا_ولا_يغير_الطلب_أو_حالة_الفني(): void
    {
        $customer = User::factory()->create();
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Confirmed)->create([
            'customer_id' => $customer->id,
            'provider_profile_id' => $provider->id,
        ]);
        $originalProviderStatus = $provider->status;

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/providers/{$provider->id}/reports", [
                'order_id' => $order->id,
                'reason_code' => 'OFF_PLATFORM_REQUEST',
                'description' => 'طلب تحويل المبلغ خارج التطبيق.',
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'NEW');

        $this->assertDatabaseHas(ProviderReport::class, [
            'reporter_user_id' => $customer->id,
            'order_id' => $order->id,
            'provider_profile_id' => $provider->id,
            'reason_code' => 'OFF_PLATFORM_REQUEST',
        ]);
        $this->assertSame(OrderStatus::Confirmed, $order->fresh()->status);
        $this->assertSame($originalProviderStatus, $provider->fresh()->status);
    }

    #[Test]
    public function العميل_لا_يبلغ_عن_فني_خارج_سياق_طلبه(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $unrelatedProvider = ProviderProfile::factory()->create();

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/providers/{$unrelatedProvider->id}/reports", [
                'order_id' => $order->id,
                'reason_code' => 'INAPPROPRIATE_BEHAVIOR',
            ])
            ->assertNotFound();
    }
}
