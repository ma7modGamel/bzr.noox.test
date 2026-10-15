<?php

declare(strict_types=1);

namespace App\Modules\Offers\Services;

use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Database\Eloquent\Collection;

final readonly class OfferRanking
{
    public function __construct(private SettingsRepository $settings) {}

    /** @param Collection<int, Offer> $offers
     * @return Collection<int, Offer>
     */
    public function best(Order $order, Collection $offers): Collection
    {
        $ratingWeight = max(0, $this->settings->int(Cfg::BestOfferRatingWeight));
        $priceWeight = max(0, $this->settings->int(Cfg::BestOfferPriceWeight));
        $etaWeight = $order->timing_type === TimingType::Now ? max(0, $this->settings->int(Cfg::BestOfferEtaWeight)) : 0;
        $minimumPrice = (float) ($offers->min('price') ?? 0);
        $minimumEta = $offers->where('eta_minutes', '>', 0)->min('eta_minutes');
        $score = static fn (Offer $offer): float => min(5, max(0, (float) $offer->providerProfile?->rating_avg)) / 5 * $ratingWeight
            + ($minimumPrice === 0.0 ? ((float) $offer->price === 0.0 ? 1 : 0) : ((float) $offer->price > 0 ? $minimumPrice / (float) $offer->price : 0)) * $priceWeight
            + ($offer->eta_minutes > 0 && $minimumEta !== null ? $minimumEta / $offer->eta_minutes : 0) * $etaWeight;

        return $offers->sort(fn (Offer $first, Offer $second): int => ($score($second) <=> $score($first)) ?: ((float) $first->price <=> (float) $second->price) ?: ($first->id <=> $second->id))->values();
    }
}
