<?php

declare(strict_types=1);

namespace App\Modules\Customers\Services;

use App\Modules\Customers\Data\InspectionMetrics;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Models\Order;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;

/** عدادات BR-047 مشتقة فقط من السجل ولا تؤثر في أهلية العميل. */
final class CustomerInspectionMetrics
{
    public function for(User $customer): InspectionMetrics
    {
        $executionQuotes = PriceProposal::query()
            ->where('type', ProposalType::ExecutionQuote)
            ->whereHas('order', fn ($query) => $query->where('customer_id', $customer->getKey()));

        return new InspectionMetrics(
            explicitRejections: (clone $executionQuotes)->where('status', ProposalStatus::Rejected)->count(),
            expiredQuotes: (clone $executionQuotes)->where('status', ProposalStatus::Expired)->count(),
            freeInspectionsClosedWithoutExecution: Order::query()
                ->where('customer_id', $customer->getKey())
                ->whereHas('events', fn ($query) => $query->where('event_code', OrderEventCode::ClosedFreeInspection))
                ->count(),
        );
    }
}
