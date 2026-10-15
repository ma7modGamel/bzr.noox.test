<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Actions\AssignWarrantyVisitAction;
use App\Modules\Support\Models\WarrantyClaim;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Carbon\CarbonInterface;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class WarrantyClaimsTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function warranty_can_be_requested_after_the_dispute_window_and_preserves_the_original_order(): void
    {
        $order = $this->repairedOrder(now()->subDays(10));
        $response = $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/warranty-claims", ['description' => 'عاد التسريب في نفس مكان الإصلاح'])->assertCreated();
        $response->assertJsonPath('data.status', 'REQUESTED');
        $this->assertSame(OrderStatus::Closed, $order->refresh()->status);
        $this->assertSame('500.00', $order->final_amount);
        $this->assertDatabaseCount('disputes', 0);
        $this->getJson("/api/v1/orders/{$order->id}/warranty-claims")->assertOk()->assertJsonCount(1, 'data');
        $this->postJson("/api/v1/orders/{$order->id}/warranty-claims", ['description' => 'بلاغ آخر لنفس الإصلاح'])->assertUnprocessable();
        $this->assertDatabaseCount('warranty_claims', 1);
    }

    #[Test]
    public function warranty_deadline_uses_repair_completion_and_excludes_unperformed_inspections(): void
    {
        $order = $this->repairedOrder(now()->subDays(15));
        $order->update(['closed_at' => now()]);
        $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/warranty-claims", ['description' => 'عاد التسريب بعد الإصلاح'])->assertUnprocessable();
        $order->update(['completed_at' => null]);
        $this->postJson("/api/v1/orders/{$order->id}/warranty-claims", ['description' => 'أريد زيارة أخرى بعد المعاينة'])->assertUnprocessable();
        $order->update(['completed_at' => now(), 'work_started_at' => null]);
        $this->postJson("/api/v1/orders/{$order->id}/warranty-claims", ['description' => 'المعاينة لم تشمل تنفيذ إصلاح'])->assertUnprocessable();
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.warranty_until', null);
        $this->assertDatabaseCount('warranty_claims', 0);
    }

    #[Test]
    public function customers_cannot_access_other_customers_warranty_claims(): void
    {
        $order = $this->repairedOrder(now());
        $this->actingAs(User::factory()->create(), 'sanctum')->postJson("/api/v1/orders/{$order->id}/warranty-claims", ['description' => 'عاد التسريب بعد الإصلاح'])->assertNotFound();
        $this->getJson("/api/v1/orders/{$order->id}/warranty-claims")->assertNotFound();
    }

    #[Test]
    public function administration_assigns_an_eligible_different_technician_with_a_recorded_appointment(): void
    {
        $order = $this->repairedOrder(now());
        $claim = WarrantyClaim::factory()->create(['order_id' => $order->id]);
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $admin = Admin::factory()->create();
        $time = CarbonImmutable::now()->addDays(2);
        $assigned = app(AssignWarrantyVisitAction::class)->execute($claim, $provider, $admin, $time, 'تم الاتفاق مع العميل على الموعد');
        $this->assertSame('ASSIGNED', $assigned->status);
        $this->assertSame($provider->id, $assigned->provider_profile_id);
        $this->assertSame($admin->id, $assigned->assigned_by_admin_id);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}/warranty-claims")->assertOk()->assertJsonPath('data.0.status', 'ASSIGNED');
        $this->assertSame($order->provider_profile_id, $order->refresh()->provider_profile_id);
    }

    #[Test]
    public function original_technician_cannot_be_assigned_for_the_warranty_visit(): void
    {
        $order = $this->repairedOrder(now());
        $claim = WarrantyClaim::factory()->create(['order_id' => $order->id]);
        $provider = $order->providerProfile;
        $provider->categories()->sync([$order->category_id]);
        $provider->areas()->sync([$order->area_id]);
        $this->expectException(BusinessRuleViolationException::class);
        app(AssignWarrantyVisitAction::class)->execute($claim, $provider, Admin::factory()->create(), CarbonImmutable::now()->addDay(), 'تم التواصل مع العميل');
    }

    private function repairedOrder(CarbonInterface $completedAt): Order
    {
        return Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => ProviderProfile::factory(), 'completed_at' => $completedAt,
            'work_started_at' => $completedAt->copy()->subHour(), 'closed_at' => $completedAt, 'final_amount' => '500.00', 'labor_total' => '500.00',
        ]);
    }
}
