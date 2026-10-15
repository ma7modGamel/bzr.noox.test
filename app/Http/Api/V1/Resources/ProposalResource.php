<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Support\Localization;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** @mixin PriceProposal */
final class ProposalResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $preview = OrderAmounts::preview($this->order, $this->resource);

        return [
            'id' => $this->id,
            'type' => $this->type->value,
            'type_label' => $this->type->getLabel(),
            'amount' => $this->amount,
            'current_total' => $preview['current']->finalAmount,
            'projected_total' => $preview['projected']->finalAmount,
            'reason' => $this->reason,
            'photo_path' => $this->photo_path,
            'photo_url' => $this->photo_path === null ? null : route('api.orders.proposal-image', ['order' => $this->order_id, 'proposal' => $this->id]),
            'price_guide' => $this->price_guide_min === null && $this->price_guide_max === null ? null : [
                'minimum' => $this->price_guide_min,
                'maximum' => $this->price_guide_max,
            ],
            'outside_price_guide' => $this->outside_price_guide,
            'price_review_reason' => $this->price_review_reason?->value,
            'outside_price_guide_reason' => $this->outside_price_guide_reason,
            'status' => $this->status->value,
            'status_label' => $this->status->getLabel(),
            // العدّاد يُحسب من هنا لا من ساعة الجهاز (42 §التحديث اللحظي)
            'expires_at' => $this->expires_at?->toIso8601String(),
            'server_time' => now()->toIso8601String(),
            'expiry_message' => Localization::text($this->type !== ProposalType::ExecutionQuote ? 'copy.proposal_extra_expiry'
                : (bccomp($preview['current']->finalAmount, '0.00', 2) === 0 ? 'copy.proposal_free_inspection_expiry' : 'copy.proposal_paid_inspection_expiry')),
            'decided_at' => $this->decided_at?->toIso8601String(),
        ];
    }
}
