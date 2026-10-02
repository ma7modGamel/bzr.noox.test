<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Customers\Services\CustomerInspectionMetrics;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class CustomerInspectionMetricsTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function يحسب_عدادات_المعاينة_المعلوماتية_من_السجل_فقط(): void
    {
        $customer = User::factory()->create();
        $otherCustomer = User::factory()->create();
        $provider = ProviderProfile::factory()->create();

        $rejectedOrder = Order::factory()->for($customer, 'customer')->create();
        $expiredOrder = Order::factory()->for($customer, 'customer')->create();
        $inspectionOnlyOrder = Order::factory()->for($customer, 'customer')->create();

        $this->proposal($rejectedOrder, $provider, ProposalType::ExecutionQuote, ProposalStatus::Rejected);
        $this->proposal($expiredOrder, $provider, ProposalType::ExecutionQuote, ProposalStatus::Expired);
        $this->proposal($inspectionOnlyOrder, $provider, ProposalType::Materials, ProposalStatus::Rejected);

        foreach ([$rejectedOrder, $expiredOrder, $inspectionOnlyOrder] as $order) {
            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::ClosedFreeInspection,
                'actor_type' => ActorType::System,
                'from_status' => $order->status,
                'to_status' => $order->status,
                'created_at' => now(),
            ]);
        }

        $unrelatedOrder = Order::factory()->for($otherCustomer, 'customer')->create();
        $this->proposal($unrelatedOrder, $provider, ProposalType::ExecutionQuote, ProposalStatus::Rejected);

        $metrics = app(CustomerInspectionMetrics::class)->for($customer);

        $this->assertSame(1, $metrics->explicitRejections);
        $this->assertSame(1, $metrics->expiredQuotes);
        $this->assertSame(3, $metrics->freeInspectionsClosedWithoutExecution);
    }

    private function proposal(
        Order $order,
        ProviderProfile $provider,
        ProposalType $type,
        ProposalStatus $status,
    ): void {
        PriceProposal::query()->create([
            'order_id' => $order->getKey(),
            'provider_profile_id' => $provider->getKey(),
            'type' => $type,
            'amount' => '100.00',
            'reason' => 'سبب للاختبار',
            'status' => $status,
            'expires_at' => now()->addHour(),
        ]);
    }
}
