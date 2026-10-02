<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ProviderHomeApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function get_returns_401_when_no_token_is_provided(): void
    {
        $this->getJson('/api/v1/provider/home')->assertUnauthorized();
    }

    #[Test]
    public function put_returns_401_when_no_token_is_provided(): void
    {
        $this->putJson('/api/v1/provider/availability', ['available_now' => true])
            ->assertUnauthorized();
    }

    #[Test]
    public function get_returns_422_when_provider_is_not_active(): void
    {
        $profile = ProviderProfile::factory()->create([
            'status' => ProviderStatus::PendingReview,
        ]);

        $this->actingAs($profile->user, 'sanctum')
            ->getJson('/api/v1/provider/home')
            ->assertUnprocessable()
            ->assertJsonPath('error.code', 'BUSINESS_RULE_VIOLATION')
            ->assertJsonPath('error.rule', 'BR-022');
    }

    #[Test]
    public function get_returns_employee_home_without_marketplace_content(): void
    {
        $profile = ProviderProfile::factory()->create([
            'available_now' => false,
            'dues_blocked_at' => now(),
        ]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/home')
            ->assertOk()
            ->assertJsonPath('data.operating_mode', 'EMPLOYEE')
            ->assertJsonPath('data.available_now', false)
            ->assertJsonPath('data.active_order', null)
            ->assertJsonPath('data.show_available_requests', false)
            ->assertJsonPath('data.dues_blocked', false)
            ->assertJsonPath('data.available_actions', [
                'set_availability',
                'open_notifications',
                'open_messages',
                'open_earnings',
                'open_provider_profile',
            ]);
    }

    #[Test]
    public function get_returns_assigned_active_order_and_server_owned_action(): void
    {
        $profile = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Confirmed)->create([
            'provider_profile_id' => $profile->getKey(),
        ]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/home')
            ->assertOk()
            ->assertJsonPath('data.active_order.id', $order->getKey())
            ->assertJsonPath('data.active_order.number', $order->number)
            ->assertJsonPath('data.active_order.status', 'CONFIRMED')
            ->assertJsonPath('data.active_order.display_status', 'order.status.provider.CONFIRMED')
            ->assertJsonPath('data.available_actions', [
                'set_availability',
                'open_notifications',
                'open_messages',
                'open_earnings',
                'open_provider_profile',
                'open_assigned_order',
            ]);
    }

    #[Test]
    public function put_returns_422_when_available_now_is_missing(): void
    {
        $profile = ProviderProfile::factory()->create();

        $this->actingAs($profile->user, 'sanctum')
            ->putJson('/api/v1/provider/availability')
            ->assertUnprocessable()
            ->assertJsonPath('error.code', 'VALIDATION_FAILED')
            ->assertJsonStructure(['error' => ['fields' => ['available_now']]]);
    }

    #[Test]
    public function put_updates_availability_and_returns_refreshed_home(): void
    {
        $profile = ProviderProfile::factory()->create(['available_now' => false]);

        $this->actingAs($profile->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->putJson('/api/v1/provider/availability', ['available_now' => true])
            ->assertOk()
            ->assertJsonPath('data.available_now', true)
            ->assertJsonPath('data.available_actions', [
                'set_availability',
                'open_notifications',
                'open_messages',
                'open_earnings',
                'open_provider_profile',
            ]);

        $this->assertTrue($profile->fresh()->available_now);
    }

    #[Test]
    public function put_returns_422_and_does_not_change_a_blocked_account(): void
    {
        $user = User::factory()->blocked()->create();
        $profile = ProviderProfile::factory()->for($user)->create(['available_now' => false]);

        $this->actingAs($user, 'sanctum')
            ->putJson('/api/v1/provider/availability', ['available_now' => true])
            ->assertUnprocessable()
            ->assertJsonPath('error.rule', 'BR-022');

        $this->assertFalse($profile->fresh()->available_now);
    }

    #[Test]
    public function marketplace_home_exposes_requests_only_when_available_and_not_blocked_by_dues(): void
    {
        app(SettingsRepository::class)->set(Cfg::OffersEnabled, true);
        $available = ProviderProfile::factory()->create(['available_now' => true]);
        $blocked = ProviderProfile::factory()->create([
            'available_now' => true,
            'dues_blocked_at' => now(),
        ]);

        $this->actingAs($available->user, 'sanctum')
            ->getJson('/api/v1/provider/home')
            ->assertOk()
            ->assertJsonPath('data.operating_mode', 'MARKETPLACE')
            ->assertJsonPath('data.show_available_requests', true)
            ->assertJsonPath('data.available_actions', [
                'set_availability',
                'open_notifications',
                'open_messages',
                'open_earnings',
                'open_provider_profile',
                'open_available_requests',
                'open_my_offers',
            ]);

        $this->actingAs($blocked->user, 'sanctum')
            ->getJson('/api/v1/provider/home')
            ->assertOk()
            ->assertJsonPath('data.dues_blocked', true)
            ->assertJsonPath('data.show_available_requests', false)
            ->assertJsonPath('data.available_actions', [
                'set_availability',
                'open_notifications',
                'open_messages',
                'open_earnings',
                'open_provider_profile',
                'open_my_offers',
            ]);
    }
}
