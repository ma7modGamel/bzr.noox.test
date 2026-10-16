<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class AddressBookWorkflowTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function post_method_override_deletes_owned_primary_address_and_returns_real_replacement(): void
    {
        $user = User::factory()->create();
        $primary = CustomerAddress::factory()->create(['user_id' => $user->id, 'is_default' => true]);
        $other = CustomerAddress::factory()->create(['user_id' => $user->id, 'is_default' => false]);
        $this->actingAs($user, 'sanctum')->postJson("/api/v1/addresses/{$primary->id}", [], ['X-HTTP-Method-Override' => 'DELETE'])
            ->assertOk()->assertJsonPath('data.deleted_id', $primary->id)
            ->assertJsonPath('data.default_address.id', $other->id)
            ->assertJsonPath('data.default_address.is_default', true)
            ->assertJsonPath('data.default_address.actions.0.code', 'edit')
            ->assertJsonPath('data.default_address.actions.1.code', 'delete');
        $this->assertSoftDeleted($primary);
        $this->assertTrue($other->fresh()->is_default);
    }

    #[Test]
    public function override_does_not_skip_authentication_or_address_ownership(): void
    {
        $address = CustomerAddress::factory()->create();
        $this->postJson("/api/v1/addresses/{$address->id}", [], ['X-HTTP-Method-Override' => 'DELETE'])->assertUnauthorized();
        $this->actingAs(User::factory()->create(), 'sanctum')
            ->postJson("/api/v1/addresses/{$address->id}", [], ['X-HTTP-Method-Override' => 'DELETE'])->assertNotFound();
        $this->assertNotSoftDeleted($address);
    }

    #[Test]
    public function selecting_primary_returns_authoritative_list_and_actions_for_every_address(): void
    {
        $user = User::factory()->create();
        $old = CustomerAddress::factory()->create(['user_id' => $user->id, 'is_default' => true]);
        $next = CustomerAddress::factory()->create(['user_id' => $user->id, 'is_default' => false]);
        $this->actingAs($user, 'sanctum')->postJson("/api/v1/addresses/{$next->id}/default")
            ->assertOk()->assertJsonPath('data.id', $next->id)->assertJsonPath('data.is_default', true)
            ->assertJsonPath('addresses.0.id', $next->id)->assertJsonPath('addresses.1.id', $old->id)
            ->assertJsonPath('addresses.1.actions.1.code', 'set_default')
            ->assertJsonPath('addresses.1.actions.1.label', 'تعيين كعنوان أساسي');
        $this->assertFalse($old->fresh()->is_default);
        $this->assertSame(1, $user->addresses()->where('is_default', true)->count());
        $this->getJson('/api/v1/addresses')->assertOk()->assertJsonPath('data.0.id', $next->id);
    }

    #[Test]
    public function primary_selection_cannot_change_another_customers_addresses(): void
    {
        $address = CustomerAddress::factory()->create(['is_default' => false]);
        $this->postJson("/api/v1/addresses/{$address->id}/default")->assertUnauthorized();
        $this->actingAs(User::factory()->create(), 'sanctum')->postJson("/api/v1/addresses/{$address->id}/default")->assertNotFound();
        $this->assertFalse($address->fresh()->is_default);
    }

    #[Test]
    public function first_saved_address_becomes_primary_and_the_next_one_does_not_replace_it_implicitly(): void
    {
        $user = User::factory()->create();
        $location = CustomerAddress::factory()->create();
        $body = [
            'label' => 'المنزل', 'city_id' => $location->city_id, 'area_id' => $location->area_id,
            'address_text' => 'شارع الاختبار', 'lat' => 31.4, 'lng' => 31.6, 'is_default' => false,
        ];
        $this->actingAs($user, 'sanctum')->postJson('/api/v1/addresses', $body)->assertCreated()->assertJsonPath('data.is_default', true);
        $this->postJson('/api/v1/addresses', [...$body, 'label' => 'العمل'])->assertCreated()->assertJsonPath('data.is_default', false);
        $this->assertSame(1, $user->addresses()->where('is_default', true)->count());
    }

    #[Test]
    public function address_display_includes_real_building_floor_zero_and_apartment(): void
    {
        $address = CustomerAddress::factory()->create(['building' => '12', 'floor' => '0', 'apartment' => '7']);
        $response = $this->actingAs($address->user, 'sanctum')->getJson('/api/v1/addresses')->assertOk();
        $display = $response->json('data.0.display_address');
        $this->assertStringContainsString('مبنى 12', $display);
        $this->assertStringContainsString('الدور 0', $display);
        $this->assertStringContainsString('شقة 7', $display);
    }

    #[Test]
    public function patch_override_keeps_the_existing_validation_and_updates_owned_address(): void
    {
        $address = CustomerAddress::factory()->create();
        $this->actingAs($address->user, 'sanctum')->postJson("/api/v1/addresses/{$address->id}", [
            'label' => 'عنوان محدث', 'city_id' => $address->city_id, 'area_id' => $address->area_id,
            'address_text' => 'شارع الاختبار', 'lat' => 31.4, 'lng' => 31.6,
        ], ['X-HTTP-Method-Override' => 'PATCH'])->assertOk()->assertJsonPath('data.label', 'عنوان محدث');
        $this->assertSame('عنوان محدث', $address->fresh()->label);
    }
}
