<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Identity\Models\User;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\Support\RecordsMobileResponses;
use Tests\TestCase;

/**
 * 9أ المرحلة 3 (DEC-061) — ردود العناوين والمدن والمناطق والفترات (C14–C16) كما يقرؤها التطبيقان.
 * ومع RECORD_API_RESPONSES=1 تُكتب إلى androidapp/app/src/test/resources/api/customer/.
 */
final class MobileCustomerAddressResponsesTest extends TestCase
{
    use RecordsMobileResponses;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function ردود_العناوين_والفترات(): void
    {
        $customer = User::factory()->create();
        $city = City::query()->where('name', 'دمياط الجديدة')->sole();
        $area = Area::query()->where('city_id', $city->id)->where('is_active', true)->orderBy('sort')->firstOrFail();

        $this->recordResponse('customer', 'cities', $this->getJson('/api/v1/cities')->assertOk()
            ->assertJsonStructure(['data' => [['id', 'name']]]));
        $this->recordResponse('customer', 'areas', $this->getJson("/api/v1/cities/{$city->id}/areas")->assertOk()
            ->assertJsonStructure(['data' => [['id', 'name']]]));

        $body = [
            'label' => 'البيت', 'city_id' => $city->id, 'area_id' => $area->id, 'address_text' => 'شارع النيل، عمارة 7',
            'building' => '7', 'floor' => '3', 'apartment' => '12', 'landmark' => '',
            'lat' => 31.4368, 'lng' => 31.667, 'is_default' => true,
        ];
        $created = $this->as($customer)->postJson('/api/v1/addresses', $body)->assertCreated()
            ->assertJsonStructure(['data' => ['id', 'label', 'city' => ['id', 'name'], 'area' => ['id', 'name'], 'address_text', 'lat', 'lng', 'is_default']]);
        $this->recordResponse('customer', 'address_saved', $created);
        $this->as($customer)->patchJson('/api/v1/addresses/'.$created->json('data.id'), [...$body, 'label' => 'الشغل'])
            ->assertOk()->assertJsonPath('data.label', 'الشغل');

        $this->recordResponse('customer', 'slots', $this->getJson("/api/v1/slots?city_id={$city->id}&date=2026-10-06")->assertOk()
            ->assertJsonStructure(['data' => [['start', 'end']]]));
    }

    private function as(User $user): static
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER']);
    }
}
