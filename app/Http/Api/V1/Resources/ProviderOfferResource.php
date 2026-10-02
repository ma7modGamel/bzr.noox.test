<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Offers\Actions\SubmitOfferAction;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Settings\Services\FeatureGate;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** @mixin Offer */
final class ProviderOfferResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $actions = [];
        $features = app(FeatureGate::class);

        if ($features->offersEnabled() && $this->order->status === OrderStatus::Open) {
            $actions[] = 'open_available_request';

            if ($this->status === OfferStatus::Submitted) {
                $actions[] = 'withdraw_offer';
            }
        } elseif ($this->status === OfferStatus::Accepted
            && $this->order->provider_profile_id === $request->user()?->providerProfile?->getKey()) {
            $actions[] = 'open_assigned_order';
        }

        return [
            'id' => $this->getKey(),
            'status' => $this->status->value,
            'display_status' => $this->status->getLabel(),
            'price' => $this->price,
            'net_amount' => app(SubmitOfferAction::class)->netAmount($this->price),
            'eta_minutes' => $this->eta_minutes,
            'inspection_fee_deductible' => $this->inspection_fee_deductible,
            'includes_text' => $this->includes_text,
            'note' => $this->note,
            'submitted_at' => $this->submitted_at?->toIso8601String(),
            'available_actions' => $actions,
            'order' => [
                'id' => $this->order->getKey(),
                'number' => $this->order->number,
                'version' => $this->order->version,
                'category' => $this->order->category?->name,
                'problem_type' => $this->order->problemType?->name,
                'area' => $this->order->area?->name,
                'timing_type' => $this->order->timing_type->value,
                'slot_start' => $this->order->slot_start?->toIso8601String(),
            ],
        ];
    }
}
