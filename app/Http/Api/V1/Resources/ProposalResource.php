<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Pricing\Services\OrderAmounts;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** @mixin PriceProposal */
final class ProposalResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'type' => $this->type->value,
            'type_label' => $this->type->getLabel(),
            'amount' => $this->amount,
            'projected_total' => OrderAmounts::projected($this->order, $this->resource)->finalAmount,
            'reason' => $this->reason,
            'photo_path' => $this->photo_path,
            'status' => $this->status->value,
            'status_label' => $this->status->getLabel(),
            // العدّاد يُحسب من هنا لا من ساعة الجهاز (42 §التحديث اللحظي)
            'expires_at' => $this->expires_at?->toIso8601String(),
            'decided_at' => $this->decided_at?->toIso8601String(),
        ];
    }
}
