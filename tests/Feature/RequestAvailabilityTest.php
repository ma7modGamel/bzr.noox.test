<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\DataProvider;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class RequestAvailabilityTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        $this->travelTo(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    #[Test]
    public function returns_availability_from_eligible_employees_and_rechecks_at_publish(): void
    {
        [$customer, $address, $category, $body] = $this->draft();
        $provider = ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create();
        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', true);

        $provider->update(['available_now' => false]);
        $this->postJson('/api/v1/orders', $body)->assertUnprocessable()
            ->assertJsonPath('error.rule', 'BR-017');

        $this->assertDatabaseCount('orders', 0);
        $this->assertDatabaseCount('order_events', 0);
    }

    #[Test]
    public function publishes_now_when_an_eligible_employee_is_available(): void
    {
        [$customer, $address, $category, $body] = $this->draft();
        ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create();

        $this->actingAs($customer, 'sanctum')->postJson('/api/v1/orders', $body)->assertCreated();

        $this->assertDatabaseHas('orders', ['customer_id' => $customer->id, 'timing_type' => 'NOW', 'status' => 'OPEN']);
    }

    #[Test]
    public function allows_a_scheduled_request_without_a_currently_available_employee(): void
    {
        [$customer, , , $body] = $this->draft();
        $body['timing_type'] = 'SCHEDULED';
        $body['slot_start'] = CarbonImmutable::parse('2026-10-06 09:00', 'Africa/Cairo')->utc()->toIso8601String();

        $this->actingAs($customer, 'sanctum')->postJson('/api/v1/orders', $body)->assertCreated();

        $this->assertDatabaseHas('orders', ['customer_id' => $customer->id, 'timing_type' => 'SCHEDULED']);
    }

    /** @return array<string, array{array<string, mixed>}> */
    public static function unavailableProfiles(): array
    {
        return [
            'not available' => [['available_now' => false]],
            'suspended' => [['status' => ProviderStatus::Suspended]],
            'independent' => [['employment_type' => EmploymentType::Independent]],
        ];
    }

    #[Test]
    #[DataProvider('unavailableProfiles')]
    public function excludes_ineligible_profiles(array $attributes): void
    {
        [$customer, $address, $category] = $this->draft();
        ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create($attributes);

        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', false);
    }

    #[Test]
    public function excludes_a_category_that_was_deactivated(): void
    {
        [$customer, $address, $category] = $this->draft();
        ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create();
        $category->update(['is_active' => false]);

        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', false);
    }

    #[Test]
    public function refuses_switching_a_scheduled_request_to_now_without_an_employee(): void
    {
        [$customer, , , $body] = $this->draft();
        $body['timing_type'] = 'SCHEDULED';
        $body['slot_start'] = CarbonImmutable::parse('2026-10-06 09:00', 'Africa/Cairo')->utc()->toIso8601String();
        $response = $this->actingAs($customer, 'sanctum')->postJson('/api/v1/orders', $body)->assertCreated();
        $id = $response->json('data.id');
        $body['timing_type'] = 'NOW';
        $body['slot_start'] = null;

        $this->patchJson('/api/v1/orders/'.$id, $body)->assertUnprocessable()->assertJsonPath('error.rule', 'BR-017');

        $this->assertDatabaseHas('orders', ['id' => $id, 'timing_type' => 'SCHEDULED']);
    }

    #[Test]
    public function excludes_employees_with_an_active_now_order(): void
    {
        [$customer, $address, $category] = $this->draft();
        $provider = ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create();
        Order::factory()->status(OrderStatus::Confirmed)->create(['provider_profile_id' => $provider->id]);

        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', false);
    }

    #[Test]
    public function excludes_employees_without_the_matching_category_and_area(): void
    {
        [$customer, $address, $category] = $this->draft();
        ProviderProfile::factory()->create();

        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', false);
    }

    #[Test]
    public function does_not_require_employee_capacity_in_marketplace_mode(): void
    {
        $this->setOperatingFlags(offersEnabled: true);
        [$customer, $address, $category] = $this->draft();

        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', true);
    }

    #[Test]
    public function refuses_availability_outside_the_city_service_hours(): void
    {
        [$customer, $address, $category] = $this->draft();
        ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create();
        $this->travelTo(CarbonImmutable::parse('2026-10-05 23:00', 'Africa/Cairo')->utc());

        $this->actingAs($customer, 'sanctum')->getJson($this->url($address, $category))
            ->assertOk()->assertJsonPath('now_available', false);
    }

    #[Test]
    public function hides_addresses_belonging_to_another_customer(): void
    {
        [, $address, $category] = $this->draft();

        $this->actingAs(User::factory()->create(), 'sanctum')->getJson($this->url($address, $category))->assertNotFound();
    }

    #[Test]
    public function requires_authentication_and_valid_input(): void
    {
        $this->getJson('/api/v1/orders/availability')->assertUnauthorized();
        $this->actingAs(User::factory()->create(), 'sanctum')->getJson('/api/v1/orders/availability')
            ->assertUnprocessable()->assertJsonValidationErrors(['customer_address_id', 'category_id'], 'error.fields');
    }

    /** @return array{User, CustomerAddress, Category, array<string, mixed>} */
    private function draft(): array
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();

        return [$customer, $address, $category, [
            'customer_address_id' => $address->id,
            'category_id' => $category->id,
            'problem_type_id' => $category->problemTypes()->where('is_other', false)->value('id'),
            'timing_type' => 'NOW', 'materials_responsibility' => 'UNSURE',
            'description' => 'تسريب أسفل الحوض.', 'terms_accepted' => true,
        ]];
    }

    private function url(CustomerAddress $address, Category $category): string
    {
        return '/api/v1/orders/availability?customer_address_id='.$address->id.'&category_id='.$category->id;
    }
}
