<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\RecordLocationAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Geo\Distance;
use Carbon\CarbonImmutable;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\Client\Request;
use Illuminate\Support\Facades\Http;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class OrderEtaTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        CarbonImmutable::setTestNow('2026-10-05 09:00:00');
        config()->set('services.google.routes_key', 'test-server-key');
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function بدء_التحرك_يحسب_وقت_القيادة_من_google_عبر_الخادم(): void
    {
        Http::fake(['routes.googleapis.com/*' => Http::response(['routes' => [['duration' => '601s']]])]);
        [$order, $provider] = $this->assignedOrder();

        $response = $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/start-trip", [
                'lat' => 30.0500,
                'lng' => 31.3300,
            ])
            ->assertOk();

        $this->assertSame(11, $order->refresh()->eta_minutes);
        $this->assertFalse($order->eta_approximate);
        $this->assertCount(1, $order->trackingPoints);
        $response->assertJsonPath('data.status', 'ON_THE_WAY');

        Http::assertSent(function (Request $request): bool {
            return $request->hasHeader('X-Goog-Api-Key', 'test-server-key')
                && $request->hasHeader('X-Goog-FieldMask', 'routes.duration')
                && $request['travelMode'] === 'DRIVE'
                && $request['routingPreference'] === 'TRAFFIC_AWARE';
        });
    }

    #[Test]
    public function eta_يعاد_حسابه_بعد_120_ثانية_أو_أكثر_من_300_متر(): void
    {
        Http::fake(['routes.googleapis.com/*' => Http::response(['routes' => [['duration' => '600s']]])]);
        [$order, $provider] = $this->assignedOrder();
        $order = app(StartTripAction::class)->execute($order, $provider, 30.0500, 31.3300);

        CarbonImmutable::setTestNow(now()->addSeconds(31));
        app(RecordLocationAction::class)->execute($order, $provider, 30.0501, 31.3300);
        Http::assertSentCount(1);

        CarbonImmutable::setTestNow(now()->addSeconds(31));
        app(RecordLocationAction::class)->execute($order->refresh(), $provider, 30.0530, 31.3300);
        Http::assertSentCount(2);

        CarbonImmutable::setTestNow(now()->addSeconds(121));
        app(RecordLocationAction::class)->execute($order->refresh(), $provider, 30.0531, 31.3300);
        Http::assertSentCount(3);
    }

    #[Test]
    public function فشل_google_يستخدم_المسافة_المستقيمة_بسرعة_20_كم_ساعة(): void
    {
        Http::fake(['routes.googleapis.com/*' => Http::response([], 503)]);
        [$order, $provider, $customer] = $this->assignedOrder();

        app(StartTripAction::class)->execute($order, $provider, 30.0500, 31.3300);

        $distance = Distance::meters(30.0500, 31.3300, (float) $order->lat, (float) $order->lng);
        $expected = (int) ceil($distance / (20_000 / 60));

        $this->assertSame($expected, $order->refresh()->eta_minutes);
        $this->assertTrue($order->eta_approximate);

        $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}/tracking")
            ->assertOk()
            ->assertJsonPath('eta_minutes', $expected)
            ->assertJsonPath('eta_approximate', true)
            ->assertJsonStructure(['eta_calculated_at']);
    }

    /** @return array{Order, ProviderProfile, User} */
    private function assignedOrder(): array
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        return [$order, $provider, $customer];
    }
}
