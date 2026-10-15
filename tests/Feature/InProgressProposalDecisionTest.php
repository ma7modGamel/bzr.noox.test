<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Jobs\ExpirePendingProposals;
use App\Modules\Orders\Models\Order;
use App\Modules\Pricing\Actions\DecideProposalAction;
use App\Modules\Pricing\Actions\SubmitProposalAction;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Localization;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Tests\Support\AttachesRepairPhotos;
use Tests\TestCase;

final class InProgressProposalDecisionTest extends TestCase
{
    use AttachesRepairPhotos;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
    }

    #[Test]
    #[TestWith(['MATERIALS', true, '500.00', '120.00', '620.00'])]
    #[TestWith(['EXTRA_WORK', true, '620.00', '0.00', '620.00'])]
    #[TestWith(['MATERIALS', false, '500.00', '0.00', '500.00'])]
    #[TestWith(['EXTRA_WORK', false, '500.00', '0.00', '500.00'])]
    public function customer_decides_in_progress_costs_before_completion(string $type, bool $approve, string $labor, string $materials, string $total): void
    {
        [$order, $provider] = $this->repair();
        $proposal = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::from($type), '120.00', 'خامات أو أعمال إضافية مطلوبة للإصلاح');
        $this->attachRepairPhotos($order, $provider);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/complete", ['expected_version' => $order->version])->assertUnprocessable();
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER');
        $actions = $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->json('data.available_actions');
        $this->getJson("/api/v1/orders/{$order->id}/proposals")->assertOk()->assertJsonPath('data.1.current_total', '500.00')
            ->assertJsonPath('data.1.projected_total', '620.00')
            ->assertJsonPath('data.1.server_time', now()->toIso8601String())
            ->assertJsonPath('data.1.expiry_message', Localization::text('copy.proposal_extra_expiry'));
        $this->assertContains('approve_proposal', $actions);
        $this->assertContains('reject_proposal', $actions);
        $version = $order->refresh()->version;
        $this->postJson("/api/v1/orders/{$order->id}/proposals/{$proposal->id}/decide", ['approve' => $approve, 'expected_version' => $version])
            ->assertOk()->assertJsonPath('data.status', 'IN_PROGRESS')->assertJsonPath('data.version', $version + 1);
        $this->assertDatabaseHas('price_proposals', ['id' => $proposal->id, 'status' => $approve ? 'APPROVED' : 'REJECTED', 'decided_by_type' => 'CUSTOMER']);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/complete", ['expected_version' => $order->refresh()->version])->assertOk()
            ->assertJsonPath('data.amounts.labor_total', $labor)->assertJsonPath('data.amounts.materials_total', $materials)->assertJsonPath('data.amounts.final_amount', $total);
    }

    #[Test]
    public function expired_proposal_cannot_be_approved_and_the_scheduler_resolves_it_without_charge(): void
    {
        [$order, $provider] = $this->repair();
        $proposal = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::Materials, '120.00', 'خامات مطلوبة لإصلاح التسريب');
        $proposal->update(['expires_at' => now()]);
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER');
        $actions = $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->json('data.available_actions');
        $this->assertNotContains('approve_proposal', $actions);
        $this->postJson("/api/v1/orders/{$order->id}/proposals/{$proposal->id}/decide", ['approve' => true, 'expected_version' => $order->version])->assertUnprocessable();
        app(ExpirePendingProposals::class)->handle(app(DecideProposalAction::class));
        $this->assertDatabaseHas('price_proposals', ['id' => $proposal->id, 'status' => 'EXPIRED']);
        $this->assertSame(OrderStatus::InProgress, $order->refresh()->status);
        $this->assertSame('500.00', OrderAmounts::for($order)->finalAmount);
    }

    #[Test]
    public function another_customer_cannot_decide_costs(): void
    {
        [$order, $provider] = $this->repair();
        $proposal = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::ExtraWork, '120.00', 'عمل إضافي مطلوب لإصلاح التسريب');
        $this->actingAs(User::factory()->create(), 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson("/api/v1/orders/{$order->id}/proposals/{$proposal->id}/decide", ['approve' => true, 'expected_version' => $order->version])->assertNotFound();
        $this->assertDatabaseHas('price_proposals', ['id' => $proposal->id, 'status' => 'PENDING']);
    }

    /** @return array{Order, ProviderProfile} */
    private function repair(): array
    {
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Arrived)->create(['provider_profile_id' => $provider->id]);
        $order->problemType->update(['employee_price_min' => 300, 'employee_price_max' => 700]);
        $quote = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::ExecutionQuote, '500.00', 'إصلاح تسريب أسفل الحوض');
        $order = app(DecideProposalAction::class)->approve($order->refresh(), $quote, ActorType::Customer, $order->customer_id);

        return [$order, $provider];
    }
}
