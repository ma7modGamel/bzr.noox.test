<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ProviderMarketplaceApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
        $this->setOperatingFlags(offersEnabled: true, inspectionFeeEnabled: true);
        app(SettingsRepository::class)->set(Cfg::DefaultCommissionRate, '0.1000');
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function تفاصيل_الطلب_المتاح_محدودة_وتعيد_شروط_العرض_والوسائط(): void
    {
        [$order, $provider] = $this->availableOrder();
        $media = OrderMedia::query()->create([
            'order_id' => $order->getKey(),
            'uploaded_by' => $order->customer_id,
            'type' => 'IMAGE',
            'path' => 'orders/example.jpg',
            'size_bytes' => 1200,
        ]);

        $response = $this->providerGet($provider, "/api/v1/provider/requests/{$order->getKey()}")
            ->assertOk()
            ->assertJsonPath('data.order.id', $order->getKey())
            ->assertJsonPath('data.order.customer.phone', null)
            ->assertJsonPath('data.media.0.id', $media->getKey())
            ->assertJsonPath('data.offer_context.min_amount', '50.00')
            ->assertJsonPath('data.offer_context.commission_rate', '0.1000')
            ->assertJsonPath('data.offer_context.default_includes_text', 'المصنعية فقط')
            ->assertJsonPath('data.order.available_actions.0', 'submit_offer');

        $this->assertArrayNotHasKey('address_text', $response->json('data.order.location'));
    }

    #[Test]
    public function الفني_يقدم_عرضه_ويراه_ويسحبه_عبر_العقد_الحقيقي(): void
    {
        [$order, $provider] = $this->availableOrder();

        $offerId = $this->actingAs($provider->user, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->postJson("/api/v1/provider/requests/{$order->getKey()}/offers", [
                'price' => 350,
                'eta_minutes' => 30,
                'inspection_fee_deductible' => true,
                'includes_text' => 'المصنعية وقطع الغيار الصغيرة',
                'note' => 'أصل خلال نصف ساعة',
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'SUBMITTED')
            ->assertJsonPath('data.net_amount', '315.00')
            ->json('data.id');

        $this->providerGet($provider, '/api/v1/provider/offers')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $offerId)
            ->assertJsonPath('data.0.display_status', 'مقدَّم')
            ->assertJsonPath('data.0.available_actions.0', 'open_available_request')
            ->assertJsonPath('data.0.available_actions.1', 'withdraw_offer');

        $this->providerGet($provider, "/api/v1/provider/requests/{$order->getKey()}")
            ->assertOk()
            ->assertJsonPath('data.own_offer.id', $offerId)
            ->assertJsonPath('data.order.available_actions.0', 'withdraw_offer');

        $this->actingAs($provider->user, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->postJson("/api/v1/provider/offers/{$offerId}/withdraw")
            ->assertOk()
            ->assertJsonPath('data.status', 'WITHDRAWN')
            ->assertJsonMissing(['withdraw_offer']);
    }

    #[Test]
    public function الطلب_غير_المؤهل_مخفي_وميزة_السوق_تحمي_كل_المسارات(): void
    {
        [$order, $provider] = $this->availableOrder();
        $outsider = ProviderProfile::factory()->create();

        $this->providerGet($outsider, "/api/v1/provider/requests/{$order->getKey()}")
            ->assertNotFound();

        $this->setOperatingFlags(offersEnabled: false);

        $this->providerGet($provider, "/api/v1/provider/requests/{$order->getKey()}")
            ->assertStatus(409)
            ->assertJsonPath('error.code', 'FEATURE_DISABLED');
        $this->providerGet($provider, '/api/v1/provider/offers')
            ->assertStatus(409)
            ->assertJsonPath('error.code', 'FEATURE_DISABLED');
    }

    /** @return array{Order, ProviderProfile} */
    private function availableOrder(): array
    {
        $order = Order::factory()->marketplace()->create();
        $provider = ProviderProfile::factory()
            ->independent()
            ->servingFor($order->category_id, $order->area_id)
            ->create();

        return [$order, $provider];
    }

    private function providerGet(ProviderProfile $provider, string $uri): TestResponse
    {
        return $this->actingAs($provider->user, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->getJson($uri);
    }
}
