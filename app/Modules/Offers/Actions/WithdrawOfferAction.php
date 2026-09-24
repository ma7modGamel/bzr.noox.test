<?php

declare(strict_types=1);

namespace App\Modules\Offers\Actions;

use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Facades\DB;

/** O-02 — سحب العرض قبل الاختيار (BR-030، DEC-014). */
final readonly class WithdrawOfferAction
{
    public function __construct(private FeatureGate $features) {}

    public function execute(Offer $offer, ProviderProfile $provider): Offer
    {
        $this->features->requireOffers('withdrawOffer');

        if ($offer->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-030', 'هذا العرض ليس عرضك.');
        }

        if ($offer->status !== OfferStatus::Submitted) {
            throw BusinessRuleViolationException::rule('BR-030', 'العرض لم يعد قابلًا للسحب.');
        }

        if ($offer->order->status !== OrderStatus::Open) {
            throw BusinessRuleViolationException::rule('BR-030', 'الطلب لم يعد مفتوحًا.');
        }

        return DB::transaction(function () use ($offer, $provider): Offer {
            $offer->forceFill([
                'status' => OfferStatus::Withdrawn,
                'withdrawn_at' => now(),
            ])->save();

            OrderEvent::query()->create([
                'order_id' => $offer->order_id,
                'event_code' => OrderEventCode::OfferWithdrawn,
                'actor_type' => ActorType::Provider,
                'actor_id' => $provider->getKey(),
                'from_status' => $offer->order->status,
                'to_status' => $offer->order->status,
                'ref_type' => 'offer',
                'ref_id' => $offer->getKey(),
                'created_at' => now(),
            ]);

            return $offer;
        });
    }
}
