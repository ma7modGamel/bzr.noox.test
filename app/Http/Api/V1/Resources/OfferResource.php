<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Offers\Models\Offer;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * كارت العرض كما يراه العميل (09 §ما يظهر للعميل).
 * صف التعيين الإداري لا يصل هنا أصلًا: `Order::offers()` يستبعده (BR-007).
 *
 * @mixin Offer
 */
final class OfferResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $provider = $this->providerProfile;

        return [
            'id' => $this->id,
            'price' => $this->price,
            'eta_minutes' => $this->eta_minutes,
            'inspection_fee_deductible' => $this->inspection_fee_deductible,
            'includes_text' => $this->includes_text,
            'note' => $this->note,
            'status' => $this->status->value,
            'submitted_at' => $this->submitted_at?->toIso8601String(),
            'provider' => [
                'id' => $provider?->id,
                'name' => $provider?->user->name,
                'avatar_path' => $provider?->user->avatar_path,
                'is_verified' => (bool) $provider?->isVerified(),
                'rating_avg' => $provider?->rating_avg,
                'ratings_count' => $provider?->ratings_count,
                'completed_orders' => $provider?->completed_orders_count,
            ],
        ];
    }
}
