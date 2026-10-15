<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Pages\OperatingSettings;
use App\Filament\Resources\Catalog\Pages\EditCategory;
use App\Filament\Resources\Catalog\RelationManagers\ProblemTypesRelationManager;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\SettingsSeeder;
use Filament\Actions\Testing\TestAction;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class StaticDataSourcesTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function catalog_uses_the_separate_admin_customer_price_guide(): void
    {
        $order = Order::factory()->create();
        $order->problemType->update(['employee_price_min' => 900, 'employee_price_max' => 950, 'customer_price_min' => 125, 'customer_price_max' => 325]);
        $this->getJson('/api/v1/catalog')->assertOk()->assertJsonPath('data.0.problem_types.0.customer_price_guide.minimum', '125.00')
            ->assertJsonPath('data.0.problem_types.0.customer_price_guide.maximum', '325.00')
            ->assertJsonPath('data.0.problem_types.0.customer_price_guide.suggested_amounts', ['125.00', '225.00', '325.00']);
    }

    #[Test]
    public function employee_guide_does_not_create_a_fictional_customer_guide(): void
    {
        $order = Order::factory()->create();
        $order->problemType->update(['employee_price_min' => 100, 'employee_price_max' => 300]);
        $this->getJson('/api/v1/catalog')->assertOk()->assertJsonPath('data.0.problem_types.0.customer_price_guide', null);
    }

    #[Test]
    public function equal_guide_bounds_do_not_create_duplicate_quick_amounts(): void
    {
        $order = Order::factory()->create();
        $order->problemType->update(['customer_price_min' => 200, 'customer_price_max' => 200]);
        $this->getJson('/api/v1/catalog')->assertOk()->assertJsonPath('data.0.problem_types.0.customer_price_guide.suggested_amounts', ['200.00']);
    }

    #[Test]
    public function offer_availability_is_individual_and_outsiders_cannot_read_offers(): void
    {
        $order = Order::factory()->marketplace()->create();
        $first = $this->offer($order, 4.8, 300, 20, true);
        $second = $this->offer($order, 4.7, 350, 25, false);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}/offers?sort=price")->assertOk()
            ->assertJsonPath('data.0.provider.available_now', true)->assertJsonPath('data.1.provider.available_now', false);
        $this->actingAs(User::factory()->create(), 'sanctum')->getJson("/api/v1/orders/{$order->id}/offers")->assertNotFound();
    }

    #[Test]
    public function best_sort_uses_weights_and_changes_when_the_admin_changes_them(): void
    {
        $this->seed(SettingsSeeder::class);
        $order = Order::factory()->marketplace()->create();
        $rated = $this->offer($order, 5, 1000, 60);
        $balanced = $this->offer($order, 4.5, 100, 10);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}/offers?sort=best")->assertOk()->assertJsonPath('data.0.id', $balanced->id);
        app(SettingsRepository::class)->setMany([Cfg::BestOfferRatingWeight->value => 100, Cfg::BestOfferPriceWeight->value => 0, Cfg::BestOfferEtaWeight->value => 0]);
        $this->getJson("/api/v1/orders/{$order->id}/offers?sort=best")->assertOk()->assertJsonPath('data.0.id', $rated->id);
        $this->getJson("/api/v1/orders/{$order->id}/offers?sort=unknown")->assertUnprocessable();
    }

    #[Test]
    public function scheduled_best_sort_ignores_arrival_speed(): void
    {
        $order = Order::factory()->marketplace()->scheduled()->create();
        $first = $this->offer($order, 4.8, 200, 90);
        $this->offer($order, 4.8, 200, 1);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}/offers?sort=best")->assertOk()->assertJsonPath('data.0.id', $first->id);
    }

    #[Test]
    public function a_free_inspection_offer_is_not_penalized_as_an_unknown_price(): void
    {
        $order = Order::factory()->marketplace()->create();
        $paid = $this->offer($order, 4.8, 100, 20);
        $free = $this->offer($order, 4.8, 0, 20);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}/offers?sort=best")->assertOk()->assertJsonPath('data.0.id', $free->id);
    }

    #[Test]
    public function admin_can_set_the_customer_guide_and_cannot_save_inverted_bounds(): void
    {
        $order = Order::factory()->create();
        $this->actingAs(Admin::factory()->super()->create(), 'admin');
        $manager = Livewire::test(ProblemTypesRelationManager::class, [
            'ownerRecord' => $order->category, 'pageClass' => EditCategory::class,
        ]);
        $action = TestAction::make('edit')->table($order->problemType);
        $manager->callAction($action, data: ['customer_price_min' => 150, 'customer_price_max' => 450])->assertHasNoFormErrors();
        $this->assertSame('150.00', $order->problemType->refresh()->customer_price_min);
        $this->getJson('/api/v1/catalog')->assertOk()->assertJsonPath('data.0.problem_types.0.customer_price_guide.suggested_amounts', ['150.00', '300.00', '450.00']);
        $manager->callAction($action, data: ['customer_price_min' => 500, 'customer_price_max' => 400])->assertHasFormErrors(['customer_price_max']);
        $this->assertSame('150.00', $order->problemType->refresh()->customer_price_min);
    }

    #[Test]
    public function admin_weights_must_total_one_hundred_and_are_saved_as_settings(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->actingAs(Admin::factory()->super()->create(), 'admin');
        Livewire::test(OperatingSettings::class)
            ->fillForm(['offers__best_rating_weight' => 10, 'offers__best_price_weight' => 10, 'offers__best_eta_weight' => 10])
            ->call('save')->assertHasFormErrors(['offers__best_rating_weight']);
        $this->assertSame(50, app(SettingsRepository::class)->int(Cfg::BestOfferRatingWeight));
        Livewire::test(OperatingSettings::class)
            ->fillForm(['offers__best_rating_weight' => 30, 'offers__best_price_weight' => 50, 'offers__best_eta_weight' => 20])
            ->call('save')->assertHasNoFormErrors();
        $this->assertSame(50, app(SettingsRepository::class)->int(Cfg::BestOfferPriceWeight));
    }

    private function offer(Order $order, float $rating, int $price, ?int $eta, bool $available = true): Offer
    {
        $provider = ProviderProfile::factory()->create(['rating_avg' => $rating, 'available_now' => $available]);

        return Offer::query()->create(['order_id' => $order->id, 'provider_profile_id' => $provider->id, 'price' => $price, 'eta_minutes' => $eta, 'status' => 'SUBMITTED', 'submitted_at' => now()]);
    }
}
