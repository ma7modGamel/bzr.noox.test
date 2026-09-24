<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * الرحلة الكاملة عبر HTTP — 31-API-CONTRACT.
 * هذا الاختبار هو **عقد التطبيقين**: كسره يعني كسر androidapp وiosapp (41 §البوابة 1.9).
 */
final class ApiJourneyTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    /** DEC-041 — التطبيق يعرف الوضع من الخادم لا من نكهة بناء. */
    #[Test]
    public function config_يعلن_وضع_التشغيل_للتطبيقات(): void
    {
        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonPath('operating_mode', 'EMPLOYEE')
            ->assertJsonPath('offers_enabled', false)
            ->assertJsonPath('inspection_fee_enabled', false)
            ->assertJsonPath('service_hours.from', '08:00')
            ->assertJsonStructure(['slots', 'media_limits', 'terms_version']);

        $this->setOperatingFlags(offersEnabled: true);

        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonPath('operating_mode', 'MARKETPLACE')
            ->assertJsonPath('offers_enabled', true);
    }

    #[Test]
    public function التسجيل_والدخول_يعيدان_توكنًا(): void
    {
        $this->postJson('/api/v1/auth/register', [
            'name' => 'أحمد محمود',
            'email' => 'ahmed@test.local',
            'phone' => '01012345678',
            'password' => 'secret-password',
        ])->assertCreated()->assertJsonStructure(['user' => ['id', 'name', 'is_verified'], 'token']);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'ahmed@test.local',
            'password' => 'secret-password',
        ])->assertOk()->assertJsonStructure(['token']);

        $this->postJson('/api/v1/auth/login', [
            'email' => 'ahmed@test.local',
            'password' => 'wrong',
        ])->assertStatus(422)->assertJsonPath('error.code', 'VALIDATION_FAILED');
    }

    /** الرحلة كاملة: العميل ينشر، الإدارة تعيّن، الفني ينفذ، العميل يوافق ويؤكد. */
    #[Test]
    public function الرحلة_الكاملة_عبر_الـ_api(): void
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();

        // 1) العميل ينشر الطلب
        $publish = $this->actingAs($customer, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'CUSTOMER', 'Idempotency-Key' => (string) Str::uuid()])
            ->postJson('/api/v1/orders', [
                'customer_address_id' => $address->id,
                'category_id' => $category->id,
                'problem_type_id' => $category->problemTypes()->where('is_other', false)->value('id'),
                'timing_type' => 'NOW',
                'materials_responsibility' => 'UNSURE',
                'terms_accepted' => true,
                'description' => 'تسريب أسفل الحوض منذ يومين.',
            ])
            ->assertCreated()
            ->assertJsonPath('data.operating_mode', 'EMPLOYEE')
            ->assertJsonPath('data.pricing_mode', 'INSPECTION')       // BR-008
            ->assertJsonPath('data.deadlines.offers_close_at', null); // لا نافذة عروض

        $orderId = $publish->json('data.id');
        $order = Order::query()->findOrFail($orderId);

        // العروض فارغة في وضع الموظفين (BR-007)
        $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$orderId}/offers")
            ->assertOk()
            ->assertJsonCount(0, 'data');

        // 2) الإدارة تعيّن (خارج الـ API العام — Filament فقط، 31 §لوحة الإدارة)
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        $providerUser = $provider->user;

        // 3) الفني: تحرك ← موقع ← وصول
        $this->providerPost($providerUser, "/api/v1/provider/orders/{$orderId}/start-trip", [
            'lat' => 30.0580, 'lng' => 31.3380,
        ])
            ->assertOk()
            ->assertJsonPath('data.status', 'ON_THE_WAY');

        CarbonImmutable::setTestNow(now()->addSeconds(31));

        $this->providerPost($providerUser, "/api/v1/provider/orders/{$orderId}/location", [
            'lat' => 30.0590, 'lng' => 31.3390,
        ])->assertNoContent();

        $this->providerPost($providerUser, "/api/v1/provider/orders/{$orderId}/arrived", [
            'lat' => 30.0600, 'lng' => 31.3400,
        ])->assertOk()->assertJsonPath('data.status', 'ARRIVED');

        // 4) عرض تنفيذ من الموقع
        $quoteId = $this->providerPost($providerUser, "/api/v1/provider/orders/{$orderId}/proposals", [
            'type' => 'EXECUTION_QUOTE',
            'amount' => 450,
            'reason' => 'تغيير خرطوم وصيانة الخلاط',
        ])->assertCreated()->json('data.id');

        // 5) العميل يوافق
        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$orderId}/proposals/{$quoteId}/decide", ['approve' => true])
            ->assertOk()
            ->assertJsonPath('data.status', 'IN_PROGRESS');

        // 6) الفني ينهي ويستلم نقدًا
        $this->providerPost($providerUser, "/api/v1/provider/orders/{$orderId}/complete")
            ->assertOk()
            ->assertJsonPath('data.amounts.final_amount', '450.00');

        $this->providerPost($providerUser, "/api/v1/provider/orders/{$orderId}/cash-received", ['amount' => 450])
            ->assertOk()
            ->assertJsonPath('data.status', 'AWAITING_CONFIRMATION');

        // 7) العميل يؤكد الإنهاء
        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$orderId}/confirm-completion")
            ->assertOk()
            ->assertJsonPath('data.status', 'CLOSED');
    }

    /** 23 §قاعدة الملكية — المورد غير المملوك يعيد 404 لا 403. */
    #[Test]
    public function طلب_مستخدم_آخر_يعيد_404_لا_403(): void
    {
        $owner = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $owner->id]);

        $this->actingAs(User::factory()->create(), 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}")
            ->assertNotFound()
            ->assertJsonPath('error.code', 'NOT_FOUND');
    }

    /** 31 — الميزة المعطّلة تُرفض بـ FEATURE_DISABLED مهما كانت الصلاحية. */
    #[Test]
    public function قبول_عرض_في_وضع_الموظفين_يعيد_feature_disabled(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();

        $offer = Offer::query()->create([
            'order_id' => $order->id,
            'provider_profile_id' => $provider->id,
            'source' => 'PROVIDER',
            'price' => '350.00',
            'status' => 'SUBMITTED',
            'submitted_at' => now(),
        ]);

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/offers/{$offer->id}/accept", ['payment_method' => 'CASH'])
            ->assertStatus(409)
            ->assertJsonPath('error.code', 'FEATURE_DISABLED');
    }

    /** 31 — القفل المتفائل: نسخة قديمة ← 409 CONFLICT. */
    #[Test]
    public function نسخة_قديمة_تعيد_conflict(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/cancel", [
                'reason_code' => 'NO_LONGER_NEEDED',
                'expected_version' => 99,
            ])
            ->assertStatus(409)
            ->assertJsonPath('error.code', 'CONFLICT');
    }

    /** 31 — نفس Idempotency-Key لا يُنشئ طلبًا ثانيًا (EC-25). */
    #[Test]
    public function نفس_مفتاح_عدم_التكرار_لا_ينشئ_طلبين(): void
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();
        $key = (string) Str::uuid();

        $payload = [
            'customer_address_id' => $address->id,
            'category_id' => $category->id,
            'problem_type_id' => $category->problemTypes()->where('is_other', false)->value('id'),
            'timing_type' => 'NOW',
            'materials_responsibility' => 'UNSURE',
            'terms_accepted' => true,
        ];

        $first = $this->actingAs($customer, 'sanctum')
            ->withHeaders(['Idempotency-Key' => $key])
            ->postJson('/api/v1/orders', $payload)->assertCreated();

        $second = $this->actingAs($customer, 'sanctum')
            ->withHeaders(['Idempotency-Key' => $key])
            ->postJson('/api/v1/orders', $payload)->assertCreated();

        $this->assertSame($first->json('data.id'), $second->json('data.id'));
        $this->assertSame(1, Order::query()->where('customer_id', $customer->id)->count());
    }

    /** BR-024 — الفني لا يرى العنوان التفصيلي ولا الهاتف قبل التأكيد. */
    #[Test]
    public function الفني_لا_يرى_العنوان_التفصيلي_قبل_التأكيد(): void
    {
        $this->setOperatingFlags(offersEnabled: true);

        $order = Order::factory()->marketplace()->create();
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();

        $response = $this->actingAs($provider->user, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->getJson('/api/v1/provider/requests')
            ->assertOk();

        $first = $response->json('data.0');

        $this->assertNotNull($first['location']['area']);
        $this->assertArrayNotHasKey('address_text', $first['location']);
        $this->assertNull($first['customer']['phone']);
    }

    /** @param array<string, mixed> $payload */
    private function providerPost(User $user, string $uri, array $payload = []): TestResponse
    {
        return $this->actingAs($user, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->postJson($uri, $payload);
    }
}
