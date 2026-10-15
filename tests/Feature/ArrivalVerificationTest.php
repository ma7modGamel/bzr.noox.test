<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Resources\Orders\Pages\ViewOrder;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\LazilyRefreshDatabase;
use Illuminate\Support\Facades\DB;
use Illuminate\Testing\TestResponse;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ArrivalVerificationTest extends TestCase
{
    use LazilyRefreshDatabase;

    #[Test]
    public function customer_code_is_private_encrypted_and_required_for_employee_arrival(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
        $order = $this->trip();
        $this->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertUnauthorized();
        $this->actingAs(User::factory()->create(), 'sanctum')->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertNotFound();
        $response = $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertOk();
        $code = $response->json('data.code');
        $this->assertMatchesRegularExpression('/^[0-9]{6}$/', $code);
        $this->assertNotSame($code, DB::table('orders')->where('id', $order->id)->value('arrival_code'));
        $this->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertJsonPath('data.code', $code);
        $this->getJson("/api/v1/orders/{$order->id}")->assertJsonMissingPath('data.arrival_code');
        $this->actingAs($order->providerProfile->user, 'sanctum')->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertNotFound();
        $this->arrive($order, '000000')->assertUnprocessable();
        $this->assertSame(OrderStatus::OnTheWay, $order->refresh()->status);
        $this->assertSame(1, $order->arrival_code_attempts);
        $this->arrive($order, $code)->assertOk()->assertJsonPath('data.status', 'ARRIVED')->assertJsonMissingPath('data.arrival_code');
        $this->assertNotNull($order->refresh()->arrival_verified_at);
        $this->assertNull($order->arrival_code);
        $this->assertSame('CUSTOMER_CODE', $order->events()->reorder()->latest('id')->first()->meta['arrival_verification']);
    }

    #[Test]
    public function expiry_lockout_and_new_trip_invalidate_old_codes_without_changing_order_state(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
        app(SettingsRepository::class)->set(Cfg::ArrivalCodeMaxAttempts, 2);
        $order = $this->trip();
        $code = $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/arrival-code")->json('data.code');
        $this->actingAs($order->providerProfile->user, 'sanctum');
        $this->arrive($order, '000000')->assertUnprocessable();
        $this->arrive($order, '000000')->assertUnprocessable();
        $this->assertTrue($order->refresh()->arrival_code_locked_until->isFuture());
        $this->arrive($order, $code)->assertUnprocessable();
        $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/arrival-code", ['refresh' => true])->assertUnprocessable();
        $this->travel(6)->minutes();
        $newCode = $this->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertOk()->json('data.code');
        $this->assertSame(0, $order->refresh()->arrival_code_attempts);
        $this->travel(11)->minutes();
        $this->actingAs($order->providerProfile->user, 'sanctum');
        $this->arrive($order, $newCode)->assertUnprocessable();
        $this->assertSame(OrderStatus::OnTheWay, $order->refresh()->status);
        $this->actingAs($order->customer, 'sanctum');
        $validNewCode = $this->postJson("/api/v1/orders/{$order->id}/arrival-code", ['refresh' => true])->assertOk()->json('data.code');
        $this->assertNotSame($newCode, $validNewCode);
        $this->travel(1)->seconds();
        $order->update(['trip_started_at' => now()]);
        $this->actingAs($order->providerProfile->user, 'sanctum');
        $this->arrive($order, $validNewCode)->assertUnprocessable();
        $this->arrive($order, null, $order->version - 1)->assertConflict();
    }

    #[Test]
    public function admin_exception_requires_a_provider_report_and_documented_contact_result(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
        $order = $this->trip();
        $this->actingAs($order->providerProfile->user, 'sanctum')->postJson("/api/v1/provider/orders/{$order->id}/arrival-exception",
            ['lat' => 30.06, 'lng' => 31.34, 'note' => 'العميل غير موجود ولا يمكنه إظهار الكود', 'expected_version' => $order->version])->assertOk()->assertJsonPath('data.arrival_exception_pending', true);
        $this->assertSame(OrderStatus::OnTheWay, $order->refresh()->status);
        app(SettingsRepository::class)->set(Cfg::OffersEnabled, true);
        $this->actingAs(Admin::factory()->super()->create(), 'admin');
        Livewire::test(ViewOrder::class, ['record' => $order->id])
            ->callAction('confirmArrivalException', ['contact_note' => 'اتصلت بالعميل وتحققت من وصول الموظف، وطلب انتظار حضوره'])
            ->assertHasNoActionErrors();
        $this->assertSame(OrderStatus::Arrived, $order->refresh()->status);
        $this->assertNotNull($order->arrived_at);
        $this->assertNull($order->arrival_exception_report);
        $event = $order->events()->reorder()->latest('id')->first();
        $this->assertSame('ADMIN_EXCEPTION', $event->meta['arrival_verification']);
        $this->assertStringContainsString('اتصلت', $event->meta['contact_note']);
        $this->assertSame('ADMIN', $event->actor_type->value);
    }

    #[Test]
    public function code_cannot_be_issued_before_travel_or_after_arrival_and_foreign_providers_cannot_report(): void
    {
        $this->seed(SettingsSeeder::class);
        $order = $this->trip();
        $order->update(['status' => OrderStatus::Confirmed]);
        $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertUnprocessable();
        $order->update(['status' => OrderStatus::Arrived]);
        $this->postJson("/api/v1/orders/{$order->id}/arrival-code")->assertUnprocessable();
        $order->update(['status' => OrderStatus::OnTheWay]);
        $other = ProviderProfile::factory()->create();
        $this->actingAs($other->user, 'sanctum')->postJson("/api/v1/provider/orders/{$order->id}/arrival-exception", ['expected_version' => $order->version])->assertNotFound();
    }

    #[Test]
    public function deferred_market_arrival_remains_location_only_without_claiming_code_verification(): void
    {
        $this->seed(SettingsSeeder::class);
        $order = Order::factory()->marketplace()->status(OrderStatus::OnTheWay)->create(['provider_profile_id' => ProviderProfile::factory()->create()->id, 'trip_started_at' => now(), 'version' => 3]);
        $this->actingAs($order->providerProfile->user, 'sanctum');
        $this->arrive($order, null)->assertOk()->assertJsonPath('data.arrival_code_required', false);
        $this->assertNull($order->refresh()->arrival_verified_at);
        $this->assertSame('LOCATION_ONLY', $order->events()->reorder()->latest('id')->first()->meta['arrival_verification']);
    }

    private function trip(): Order
    {
        return Order::factory()->status(OrderStatus::OnTheWay)->create(['provider_profile_id' => ProviderProfile::factory()->create()->id, 'trip_started_at' => now(), 'version' => 3])->refresh();
    }

    private function arrive(Order $order, ?string $code, ?int $version = null): TestResponse
    {
        return $this->postJson("/api/v1/provider/orders/{$order->id}/arrived", ['expected_version' => $version ?? $order->version, 'lat' => 30.06, 'lng' => 31.34, 'arrival_code' => $code]);
    }
}
