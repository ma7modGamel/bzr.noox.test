<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use Carbon\CarbonImmutable;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class OrderRepublishTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function طلب_مفتوح_بلا_عروض_يعرض_التعديل_وبعد_غلق_النافذة_يعرض_إعادة_النشر(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->marketplace()->for($customer, 'customer')->create([
            'offers_close_at' => now()->subMinute(),
        ]);

        $response = $this->actingAs($customer, 'sanctum')
            ->withHeader('X-App-Mode', 'CUSTOMER')
            ->getJson("/api/v1/orders/{$order->id}")
            ->assertOk();

        $this->assertSame(['edit_request', 'republish', 'cancel'], $response->json('data.available_actions'));

        $this->actingAs($customer, 'sanctum')
            ->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson("/api/v1/orders/{$order->id}/republish")
            ->assertOk()
            ->assertJsonPath('data.id', $order->id)
            ->assertJsonPath('data.status', OrderStatus::Open->value);

        $this->assertDatabaseHas('order_events', [
            'order_id' => $order->id,
            'event_code' => OrderEventCode::Republished->value,
        ]);
        $this->assertSame(1, $order->refresh()->reopen_count);
    }

    #[Test]
    public function إعادة_نشر_طلب_منتهي_تنشئ_طلبًا_جديدًا_مرتبطًا_بالأصل(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->marketplace()->status(OrderStatus::Expired)->for($customer, 'customer')->create([
            'expired_at' => now()->subMinute(),
            'offers_close_at' => now()->subHour(),
        ]);

        $newId = $this->actingAs($customer, 'sanctum')
            ->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson("/api/v1/orders/{$order->id}/republish")
            ->assertCreated()
            ->assertJsonPath('data.status', OrderStatus::Open->value)
            ->json('data.id');

        $this->assertNotSame($order->id, $newId);
        $this->assertDatabaseHas('orders', [
            'id' => $newId,
            'republished_from_id' => $order->id,
            'status' => OrderStatus::Open->value,
            'reopen_count' => 1,
        ]);
    }

    #[Test]
    public function تعديل_طلب_مفتوح_بلا_عروض_يحدث_نسخة_العنوان_ويسجل_evt_002(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->marketplace()->for($customer, 'customer')->create();
        $order->category->cities()->syncWithoutDetaching([$order->city_id => ['is_active' => true]]);
        $address = CustomerAddress::factory()->for($customer, 'user')->create([
            'city_id' => $order->city_id,
            'area_id' => $order->area_id,
            'address_text' => 'عنوان جديد ثابت داخل الطلب',
        ]);

        $this->actingAs($customer, 'sanctum')
            ->withHeader('X-App-Mode', 'CUSTOMER')
            ->patchJson("/api/v1/orders/{$order->id}", [
                'customer_address_id' => $address->id,
                'category_id' => $order->category_id,
                'problem_type_id' => $order->problem_type_id,
                'timing_type' => 'NOW',
                'materials_responsibility' => 'UNSURE',
                'description' => 'وصف محدث للطلب',
                'pricing_mode' => 'EXECUTION',
            ])
            ->assertOk()
            ->assertJsonPath('data.customer_address_id', $address->id)
            ->assertJsonPath('data.description', 'وصف محدث للطلب');

        $this->assertDatabaseHas('orders', [
            'id' => $order->id,
            'customer_address_id' => $address->id,
            'address_text' => 'عنوان جديد ثابت داخل الطلب',
        ]);
        $this->assertDatabaseHas('order_events', [
            'order_id' => $order->id,
            'event_code' => OrderEventCode::Updated->value,
        ]);
    }
}
