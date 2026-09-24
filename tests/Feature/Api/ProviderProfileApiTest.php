<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Identity\Models\User;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ProviderProfileApiTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function ملف_الفني_يتطلب_المصادقة(): void
    {
        $provider = ProviderProfile::factory()->create();

        $this->getJson("/api/v1/providers/{$provider->id}?order_id=1")
            ->assertUnauthorized();
    }

    #[Test]
    public function العميل_يرى_ملف_فني_قدم_عرضا_من_دون_بيانات_خاصة(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->setOperatingFlags(offersEnabled: true);
        $customer = User::factory()->create();
        $order = Order::factory()->marketplace()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()->independent()->create([
            'bio' => 'فني متخصص في أعمال السباكة.',
            'rating_avg' => '4.90',
        ]);
        Offer::query()->create([
            'order_id' => $order->id,
            'provider_profile_id' => $provider->id,
            'source' => 'PROVIDER',
            'price' => '450.00',
            'status' => 'SUBMITTED',
            'submitted_at' => now(),
        ]);

        $response = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/providers/{$provider->id}?order_id={$order->id}")
            ->assertOk()
            ->assertJsonPath('data.id', $provider->id)
            ->assertJsonPath('data.bio', 'فني متخصص في أعمال السباكة.')
            ->assertJsonStructure(['data' => ['rating_breakdown', 'categories', 'specialties', 'portfolio', 'reviews']]);

        $this->assertEqualsCanonicalizing(
            ['accept_offer', 'chat', 'report_provider'],
            $response->json('data.available_actions'),
        );
        $this->assertArrayNotHasKey('phone', $response->json('data'));
        $this->assertArrayNotHasKey('payout_details', $response->json('data'));
    }

    #[Test]
    public function طلب_عميل_آخر_أو_فني_غير_مرتبط_يعيد_404(): void
    {
        $owner = User::factory()->create();
        $order = Order::factory()->marketplace()->create(['customer_id' => $owner->id]);
        $provider = ProviderProfile::factory()->create();

        $this->actingAs(User::factory()->create(), 'sanctum')
            ->getJson("/api/v1/providers/{$provider->id}?order_id={$order->id}")
            ->assertNotFound();

        $this->actingAs($owner, 'sanctum')
            ->getJson("/api/v1/providers/{$provider->id}?order_id={$order->id}")
            ->assertNotFound();
    }
}
