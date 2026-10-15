<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Actions\BackOutAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Pricing\Actions\DecideProposalAction;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Actions\AssignWarrantyVisitAction;
use App\Modules\Support\Actions\RequestWarrantyAction;
use App\Modules\Support\Models\WarrantyClaim;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\Support\AttachesRepairPhotos;
use Tests\TestCase;

final class WarrantyVisitWorkflowTest extends TestCase
{
    use AttachesRepairPhotos;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        CarbonImmutable::setTestNow('2026-10-13 09:00:00');
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function assigned_visit_is_visible_to_both_parties_and_free_repair_closes_without_payment(): void
    {
        [$original, $visit, $provider] = $this->visit();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/orders?scope=current')->assertOk()->assertJsonPath('data.0.id', $visit->id)
            ->assertJsonPath('data.0.is_warranty_visit', true)->assertJsonCount(1, 'data.0.proposal_type_options');
        $this->actingAs($original->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->getJson("/api/v1/orders/{$visit->id}")->assertOk()->assertJsonPath('data.warranty_original_order_number', $original->number);
        $this->startRepair($visit, $provider);
        $this->providerPost($visit, $provider, 'complete')->assertUnprocessable();
        $this->attachRepairPhotos($visit, $provider);
        $this->providerPost($visit, $provider, 'complete')->assertOk()->assertJsonPath('data.status', 'CLOSED')
            ->assertJsonPath('data.amounts.final_amount', '0.00')->assertJsonPath('data.amounts.payment_status', 'WAIVED')
            ->assertJsonPath('data.warranty_until', $original->completed_at->addDays(14)->toIso8601String());
        $this->assertDatabaseCount('payments', 0);
        $this->assertSame('COMPLETED', $visit->warrantyClaim->refresh()->status);
        $this->assertSame('500.00', $original->refresh()->final_amount);
        $this->assertFalse(RequestWarrantyAction::eligible($visit->refresh()));
    }

    #[Test]
    public function only_customer_approved_materials_are_payable_and_labor_is_always_zero(): void
    {
        [$original, $visit, $provider] = $this->visit();
        $this->startRepair($visit, $provider);
        $proposal = $this->providerPost($visit, $provider, 'proposals', ['type' => 'MATERIALS', 'amount' => 120, 'reason' => 'خرطوم جديد لإصلاح العطل'])->assertCreated();
        $id = $proposal->json('data.id');
        $this->attachRepairPhotos($visit, $provider);
        $this->providerPost($visit, $provider, 'complete')->assertUnprocessable();
        $actions = $this->actingAs($original->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->getJson("/api/v1/orders/{$visit->id}")->assertOk()->json('data.available_actions');
        $this->assertContains('approve_proposal', $actions);
        $this->assertContains('reject_proposal', $actions);
        $this->actingAs($original->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson("/api/v1/orders/{$visit->id}/proposals/{$id}/decide", ['approve' => true, 'expected_version' => $visit->refresh()->version])->assertOk();
        $this->providerPost($visit, $provider, 'complete')->assertOk()->assertJsonPath('data.status', 'AWAITING_PAYMENT')
            ->assertJsonPath('data.amounts.labor_total', '0.00')->assertJsonPath('data.amounts.materials_total', '120.00')->assertJsonPath('data.amounts.final_amount', '120.00');
        $this->providerPost($visit, $provider, 'cash-received', ['amount' => 120])->assertOk();
        $this->actingAs($original->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson("/api/v1/orders/{$visit->id}/confirm-completion", ['expected_version' => $visit->refresh()->version])->assertOk();
        $this->assertSame('0.00', $visit->refresh()->commission_amount);
        $this->assertSame('COMPLETED', $visit->warrantyClaim->refresh()->status);
    }

    #[Test]
    public function technician_cannot_charge_labor_or_extra_work_on_warranty(): void
    {
        [, $visit, $provider] = $this->visit();
        $this->startRepair($visit, $provider);
        foreach (['EXECUTION_QUOTE', 'EXTRA_WORK'] as $type) {
            $this->providerPost($visit, $provider, 'proposals', ['type' => $type, 'amount' => 200, 'reason' => 'محاولة إضافة مصنعية للضمان'])->assertUnprocessable();
        }
        $this->assertDatabaseCount('price_proposals', 0);
    }

    #[Test]
    public function refusing_materials_does_not_charge_customer_and_completion_remains_free(): void
    {
        [$original, $visit, $provider] = $this->visit();
        $this->startRepair($visit, $provider);
        $id = $this->providerPost($visit, $provider, 'proposals', ['type' => 'MATERIALS', 'amount' => 80, 'reason' => 'قطعة غيار تحتاج موافقة العميل'])->assertCreated()->json('data.id');
        $this->actingAs($original->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson("/api/v1/orders/{$visit->id}/proposals/{$id}/decide", ['approve' => false, 'expected_version' => $visit->refresh()->version])->assertOk();
        $this->attachRepairPhotos($visit, $provider);
        $this->providerPost($visit, $provider, 'complete')->assertOk()->assertJsonPath('data.amounts.final_amount', '0.00');
    }

    #[Test]
    public function administration_cannot_approve_materials_on_behalf_of_customer(): void
    {
        [, $visit, $provider] = $this->visit();
        $this->startRepair($visit, $provider);
        $this->providerPost($visit, $provider, 'proposals', ['type' => 'MATERIALS', 'amount' => 80, 'reason' => 'خامات مطلوبة تحتاج موافقة'])->assertCreated();
        $this->expectException(BusinessRuleViolationException::class);
        app(DecideProposalAction::class)->approve($visit->refresh(), $visit->proposals()->firstOrFail(), ActorType::Admin, Admin::factory()->create()->id);
    }

    #[Test]
    public function technician_backout_keeps_the_visit_for_administrative_reassignment(): void
    {
        [$original, $visit, $provider] = $this->visit();
        app(BackOutAction::class)->execute($visit, ActorType::Provider, $provider->id, CancelReason::Other, $provider);
        $claim = $visit->warrantyClaim->refresh();
        $this->assertSame('REQUESTED', $claim->status);
        $replacement = ProviderProfile::factory()->servingFor($visit->category_id, $visit->area_id)->create();
        $assigned = app(AssignWarrantyVisitAction::class)->execute($claim, $replacement, Admin::factory()->create(), CarbonImmutable::now()->addHours(2), 'تم الاتفاق على الفني البديل والموعد');
        $this->assertSame($visit->id, $assigned->visit->id);
        $this->assertSame($replacement->id, $assigned->visit->provider_profile_id);
        $this->assertSame($original->provider_profile_id, $original->refresh()->provider_profile_id);
    }

    private function startRepair(Order $visit, ProviderProfile $provider): void
    {
        $this->providerPost($visit, $provider, 'start-trip', ['lat' => 30.06, 'lng' => 31.34])->assertOk();
        $this->providerPost($visit, $provider, 'arrived', ['lat' => 30.06, 'lng' => 31.34, 'confirm_far_arrival' => true, 'arrival_code' => $this->arrivalCode($visit->refresh())])->assertOk();
        $this->providerPost($visit, $provider, 'start-work')->assertOk();
    }

    private function providerPost(Order $visit, ProviderProfile $provider, string $path, array $body = []): TestResponse
    {
        return $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$visit->id}/{$path}", $body + ['expected_version' => $visit->refresh()->version]);
    }

    /** @return array{Order, Order, ProviderProfile} */
    private function visit(): array
    {
        $original = Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => ProviderProfile::factory(), 'work_started_at' => now()->subDays(5)->subHour(),
            'completed_at' => now()->subDays(5), 'closed_at' => now()->subDays(5), 'final_amount' => 500,
        ]);
        $claim = WarrantyClaim::factory()->create(['order_id' => $original->id]);
        $provider = ProviderProfile::factory()->servingFor($original->category_id, $original->area_id)->create();
        $assigned = app(AssignWarrantyVisitAction::class)->execute($claim, $provider, Admin::factory()->create(), CarbonImmutable::now()->addMinutes(30), 'تم تنسيق زيارة ضمان مع العميل');

        return [$original, $assigned->visit, $provider];
    }
}
