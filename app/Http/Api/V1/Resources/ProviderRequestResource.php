<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Offers\Actions\SubmitOfferAction;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** @mixin Order */
final class ProviderRequestResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $profileId = $request->user()?->providerProfile?->getKey();
        $ownOffer = $this->offers
            ->where('provider_profile_id', $profileId)
            ->sortByDesc('id')
            ->first();
        $settings = app(SettingsRepository::class);

        return [
            'order' => (new OrderResource($this->resource))->resolve($request),
            'media' => $this->media->map(static fn ($media): array => [
                'id' => $media->getKey(),
                'type' => $media->type,
                'size_bytes' => $media->size_bytes,
                'duration_sec' => $media->duration_sec,
                'url' => "/api/v1/media/{$media->getKey()}",
            ])->values(),
            'own_offer' => $ownOffer === null ? null : [
                'id' => $ownOffer->getKey(),
                'status' => $ownOffer->status->value,
                'display_status' => $ownOffer->status->getLabel(),
                'price' => $ownOffer->price,
                'net_amount' => app(SubmitOfferAction::class)->netAmount($ownOffer->price),
            ],
            'offer_context' => [
                'min_amount' => $settings->decimal(Cfg::MinOfferAmount),
                'commission_rate' => $settings->decimal(Cfg::DefaultCommissionRate),
                'eta_min_minutes' => 5,
                'eta_max_minutes' => 180,
                'eta_options' => [15, 30, 45, 60, 90, 120, 180],
                'deductible_options' => [
                    ['code' => 'YES', 'label' => 'تُخصم من المصنعية', 'value' => true],
                    ['code' => 'NO', 'label' => 'لا تُخصم من المصنعية', 'value' => false],
                ],
                'default_includes_text' => 'المصنعية فقط',
                'can_reapply' => $ownOffer?->status === OfferStatus::Withdrawn
                    && $this->offers->where('provider_profile_id', $profileId)
                        ->where('status', OfferStatus::Withdrawn)->count() < 2,
            ],
        ];
    }
}
