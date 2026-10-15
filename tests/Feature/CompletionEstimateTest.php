<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Models\WarrantyClaim;
use Carbon\CarbonImmutable;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Tests\TestCase;

final class CompletionEstimateTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function assigned_technician_can_set_update_and_clear_the_estimate_and_customer_reads_actual_time(): void
    {
        $this->travelTo(CarbonImmutable::parse('2026-10-14 10:00:00', 'UTC'));
        [$order, $provider] = $this->work();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER');
        $this->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => 45, 'expected_version' => $order->version])->assertOk()->assertJsonPath('data.completion_estimate.estimated_at', now()->addMinutes(45)->toIso8601String());
        $this->assertSame(1, $order->events()->where('event_code', 'EVT-031')->count());
        $this->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => 60, 'expected_version' => $order->refresh()->version])->assertOk();
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.completion_estimate.estimated_at', now()->addHour()->toIso8601String());
        $this->travel(61)->minutes();
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.completion_estimate.overdue', true);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => null, 'expected_version' => $order->refresh()->version])->assertOk()->assertJsonPath('data.completion_estimate', null);
    }

    #[Test]
    #[TestWith([0])]
    #[TestWith([1441])]
    #[TestWith([-1])]
    public function invalid_estimates_do_not_change_order_or_events(int $minutes): void
    {
        [$order, $provider] = $this->work();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => $minutes, 'expected_version' => $order->version])->assertUnprocessable();
        $this->assertNull($order->refresh()->estimated_completion_at);
        $this->assertSame(0, $order->events()->count());
    }

    #[Test]
    public function outsider_customer_wrong_status_and_stale_version_cannot_update_estimate(): void
    {
        [$order, $provider] = $this->work();
        $this->actingAs(User::factory()->create(), 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => 30, 'expected_version' => $order->version])->assertNotFound();
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => 30, 'expected_version' => $order->version])->assertNotFound();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => 30, 'expected_version' => 99])->assertConflict();
        $order->update(['status' => OrderStatus::Closed]);
        $this->postJson("/api/v1/provider/orders/{$order->id}/completion-estimate", ['minutes' => 30, 'expected_version' => $order->version])->assertUnprocessable();
        $this->assertNull($order->refresh()->estimated_completion_at);
    }

    #[Test]
    public function direct_execution_has_no_fictional_inspection_or_approval_and_no_default_estimate(): void
    {
        [$order] = $this->work();
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.completion_estimate', null)
            ->assertJsonPath('data.execution_steps.0.key', 'arrived')->assertJsonPath('data.execution_steps.0.occurred_at', $order->arrived_at->toIso8601String())
            ->assertJsonPath('data.execution_steps.1.key', 'repairing')->assertJsonPath('data.execution_steps.1.state', 'active')->assertJsonCount(3, 'data.execution_steps');
    }

    #[Test]
    public function inspection_and_approval_times_come_from_the_actual_quote_events_only(): void
    {
        [$order] = $this->work();
        $order->update(['pricing_mode' => PricingMode::Inspection, 'confirmed_at' => now()->subHour()]);
        foreach ([['EVT-040', 7, now()->subMinutes(9), ['type' => 'MATERIALS']], ['EVT-040', 8, now()->subMinutes(7), ['type' => 'EXECUTION_QUOTE']], ['EVT-041', 8, now()->subMinutes(5), []]] as [$code, $reference, $at, $meta]) {
            $order->events()->create(['event_code' => $code, 'actor_type' => 'PROVIDER', 'actor_id' => $order->provider_profile_id, 'ref_type' => 'proposal', 'ref_id' => $reference, 'meta' => $meta, 'created_at' => $at]);
        }
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()
            ->assertJsonPath('data.execution_steps.1.key', 'inspected')->assertJsonPath('data.execution_steps.1.occurred_at', now()->subMinutes(7)->toIso8601String())
            ->assertJsonPath('data.execution_steps.2.key', 'approved')->assertJsonPath('data.execution_steps.2.occurred_at', now()->subMinutes(5)->toIso8601String())
            ->assertJsonCount(5, 'data.execution_steps');
    }

    #[Test]
    public function old_assignment_quotes_do_not_invent_inspection_in_a_new_visit(): void
    {
        [$order] = $this->work();
        $order->update(['confirmed_at' => now()->subMinutes(15)]);
        $order->events()->create(['event_code' => 'EVT-040', 'actor_type' => 'PROVIDER', 'actor_id' => $order->provider_profile_id, 'meta' => ['type' => 'EXECUTION_QUOTE'], 'created_at' => now()->subDay()]);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonCount(3, 'data.execution_steps')->assertJsonPath('data.execution_steps.1.key', 'repairing');
    }

    #[Test]
    public function warranty_visit_does_not_claim_inspection_or_execution_quote_approval(): void
    {
        [$order] = $this->work();
        $claim = WarrantyClaim::factory()->create();
        $order->update(['warranty_claim_id' => $claim->id]);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()
            ->assertJsonCount(3, 'data.execution_steps')->assertJsonPath('data.execution_steps.1.key', 'repairing');
    }

    private function work(): array
    {
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->create(['provider_profile_id' => $provider->id, 'status' => OrderStatus::InProgress, 'pricing_mode' => PricingMode::Execution, 'arrived_at' => now()->subMinutes(10), 'work_started_at' => now()->subMinutes(5)]);

        return [$order->refresh(), $provider];
    }
}
