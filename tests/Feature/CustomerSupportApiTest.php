<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use Carbon\CarbonImmutable;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class CustomerSupportApiTest extends TestCase
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
    public function العميل_يدير_عناوينه_عبر_العقد_المستخدم_في_c14_و_c15(): void
    {
        $customer = User::factory()->create();
        [$city, $area] = $this->servedArea();

        $created = $this->actingAs($customer, 'sanctum')->postJson('/api/v1/addresses', [
            'label' => 'المنزل',
            'city_id' => $city->id,
            'area_id' => $area->id,
            'address_text' => 'شارع مصطفى النحاس',
            'building' => '12',
            'floor' => '3',
            'apartment' => '8',
            'landmark' => 'بجوار الحديقة',
            'lat' => 30.0566,
            'lng' => 31.3301,
            'is_default' => true,
        ])->assertCreated()
            ->assertJsonPath('data.area.name', 'الحي الأول')
            ->assertJsonPath('data.is_default', true);

        $addressId = $created->json('data.id');

        $this->actingAs($customer, 'sanctum')->getJson('/api/v1/addresses')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $addressId);

        $this->actingAs($customer, 'sanctum')->patchJson("/api/v1/addresses/{$addressId}", [
            'label' => 'العمل',
            'city_id' => $city->id,
            'area_id' => $area->id,
            'address_text' => 'شارع مصطفى النحاس',
            'lat' => 30.0566,
            'lng' => 31.3301,
            'is_default' => false,
        ])->assertOk()->assertJsonPath('data.label', 'العمل');

        $this->actingAs($customer, 'sanctum')->deleteJson("/api/v1/addresses/{$addressId}")
            ->assertOk()
            ->assertJsonPath('data.deleted_id', $addressId)
            ->assertJsonPath('data.default_address', null);

        $this->assertSoftDeleted(CustomerAddress::class, ['id' => $addressId]);
    }

    #[Test]
    public function حذف_العنوان_الافتراضي_يعين_آخر_عنوان_معدل_ويرجعه(): void
    {
        $customer = User::factory()->create();
        [$city, $area] = $this->servedArea();
        $default = CustomerAddress::factory()->create([
            'user_id' => $customer->id, 'city_id' => $city->id, 'area_id' => $area->id,
            'is_default' => true, 'updated_at' => now()->subDays(3),
        ]);
        $older = CustomerAddress::factory()->create([
            'user_id' => $customer->id, 'city_id' => $city->id, 'area_id' => $area->id,
            'label' => 'العمل', 'is_default' => false, 'updated_at' => now()->subDays(2),
        ]);
        $newest = CustomerAddress::factory()->create([
            'user_id' => $customer->id, 'city_id' => $city->id, 'area_id' => $area->id,
            'label' => 'منزل العائلة', 'is_default' => false, 'updated_at' => now()->subDay(),
        ]);

        $this->actingAs($customer, 'sanctum')->deleteJson("/api/v1/addresses/{$default->id}")
            ->assertOk()
            ->assertJsonPath('data.default_address.id', $newest->id)
            ->assertJsonPath('data.default_address.is_default', true);

        $this->assertSoftDeleted($default);
        $this->assertFalse($older->fresh()->is_default);
        $this->assertTrue($newest->fresh()->is_default);
    }

    #[Test]
    public function حذف_آخر_عنوان_لا_يترك_افتراضيا_ولا_يغير_نسخة_عنوان_الطلب(): void
    {
        $customer = User::factory()->create();
        [$city, $area] = $this->servedArea();
        $address = CustomerAddress::factory()->create([
            'user_id' => $customer->id, 'city_id' => $city->id, 'area_id' => $area->id,
            'address_text' => 'العنوان الثابت داخل الطلب', 'is_default' => true,
        ]);
        $order = Order::factory()->create([
            'customer_id' => $customer->id, 'city_id' => $city->id, 'area_id' => $area->id,
            'address_text' => $address->address_text,
        ]);

        $this->actingAs($customer, 'sanctum')->deleteJson("/api/v1/addresses/{$address->id}")
            ->assertOk()
            ->assertJsonPath('data.default_address', null);

        $this->assertSoftDeleted($address);
        $this->assertSame('العنوان الثابت داخل الطلب', $order->fresh()->address_text);
        $this->assertFalse($customer->addresses()->where('is_default', true)->exists());
    }

    #[Test]
    public function الفترات_تعيد_فقط_الفترات_المتاحة_بالتوقيت_المحلي(): void
    {
        [$city] = $this->servedArea();

        $this->getJson("/api/v1/slots?city_id={$city->id}&date=2026-10-05")
            ->assertOk()
            ->assertJsonStructure(['data' => [['start', 'end']]]);
    }

    /** @return array{City, Area} */
    private function servedArea(): array
    {
        $city = City::query()->create([
            'name' => 'دمياط الجديدة',
            'timezone' => 'Africa/Cairo',
            'is_active' => true,
        ]);
        $area = Area::query()->create([
            'city_id' => $city->id,
            'name' => 'الحي الأول',
            'is_active' => true,
            'sort' => 1,
        ]);

        return [$city, $area];
    }
}
