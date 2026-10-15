<?php

declare(strict_types=1);

namespace App\Modules\Offers\Actions;

use App\Modules\Communication\Services\ContactMasker;
use App\Modules\Offers\Enums\OfferSource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\DomainException;
use App\Support\Localization;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;

/**
 * O-01 — تقديم عرض (09، BR-030..BR-032).
 * وضع السوق فقط: مبني بالكامل ومعطّل بـ CFG-090 في المرحلة الأولى (39).
 */
final readonly class SubmitOfferAction
{
    public function __construct(
        private FeatureGate $features,
        private SettingsRepository $settings,
        private ProviderEligibility $eligibility,
        private ContactMasker $masker,
    ) {}

    public function execute(
        Order $order,
        ProviderProfile $provider,
        string $price,
        ?int $etaMinutes = null,
        ?bool $inspectionFeeDeductible = null,
        ?string $includesText = null,
        ?string $note = null,
    ): Offer {
        $this->features->requireOffers('submitOffer');

        $this->assertWindowIsOpen($order);
        $this->assertProviderMayOffer($order, $provider);
        $previous = $this->assertNoActiveOffer($order, $provider);

        $this->assertPrice($price);
        $etaMinutes = $this->normalizeEta($order, $etaMinutes);
        $inspectionFeeDeductible = $this->normalizeDeductible($order, $inspectionFeeDeductible);

        return DB::transaction(function () use ($order, $provider, $price, $etaMinutes, $inspectionFeeDeductible, $includesText, $note, $previous): Offer {
            $offer = Offer::query()->create([
                'order_id' => $order->getKey(),
                'provider_profile_id' => $provider->getKey(),
                'source' => OfferSource::Provider,
                'price' => $price,
                'eta_minutes' => $etaMinutes,
                'inspection_fee_deductible' => $inspectionFeeDeductible,
                'includes_text' => $includesText ?? Localization::text('copy.labor_only'),
                // BR-032 — الأرقام والروابط تُحجب من ملاحظة العرض
                'note' => $note === null ? null : $this->masker->mask($note)['body'],
                'status' => OfferStatus::Submitted,
                'previous_offer_id' => $previous?->getKey(),
                'submitted_at' => now(),
            ]);

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::OfferSubmitted,
                'actor_type' => ActorType::Provider,
                'actor_id' => $provider->getKey(),
                'from_status' => $order->status,
                'to_status' => $order->status,
                'ref_type' => 'offer',
                'ref_id' => $offer->getKey(),
                'meta' => ['price' => $price, 'eta_minutes' => $etaMinutes],
                'created_at' => now(),
            ]);

            return $offer;
        });
    }

    /** "صافي لك" المعروض للفني عند كتابة العرض (09، DEC-009). */
    public function netAmount(string $price): string
    {
        $rate = $this->settings->decimal(Cfg::DefaultCommissionRate) ?? '0';

        return bcsub($price, bcmul($price, $rate, 2), 2);
    }

    /** نفس حراس التنفيذ التي تغذي available_actions. */
    public function canSubmit(Order $order, ProviderProfile $provider): bool
    {
        try {
            $this->features->requireOffers('submitOffer');
            $this->assertWindowIsOpen($order);
            $this->assertProviderMayOffer($order, $provider);
            $this->assertNoActiveOffer($order, $provider);

            return true;
        } catch (DomainException) {
            return false;
        }
    }

    /** BR-031 — الطلب مفتوح، قبل إغلاق النافذة، وعدد العروض أقل من CFG-011. */
    private function assertWindowIsOpen(Order $order): void
    {
        if ($order->isWarrantyVisit()) {
            throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_assignment_invalid'));
        }
        if ($order->status !== OrderStatus::Open) {
            throw BusinessRuleViolationException::rule('BR-031', Localization::text('copy.this_request_is_no_longer_receiving_offers'));
        }

        if ($order->offers_close_at !== null && CarbonImmutable::now()->gt($order->offers_close_at)) {
            throw BusinessRuleViolationException::rule('BR-031', Localization::text('copy.the_offer_submission_window_has_closed'));
        }

        $max = $this->settings->int(Cfg::MaxOffersPerOrder);

        if ($order->offers()->where('status', OfferStatus::Submitted->value)->count() >= $max) {
            throw BusinessRuleViolationException::rule('BR-031', Localization::text('dynamic.offer_limit', ['count' => $max]));
        }
    }

    private function assertProviderMayOffer(Order $order, ProviderProfile $provider): void
    {
        // BR-036 — الفني المعتذر ممنوع من التقديم على نفس الطلب
        $backedOut = Offer::query()
            ->where('order_id', $order->getKey())
            ->where('provider_profile_id', $provider->getKey())
            ->where('status', OfferStatus::BackedOut->value)
            ->exists();

        if ($backedOut) {
            throw BusinessRuleViolationException::rule('BR-036', Localization::text('copy.you_cannot_submit_an_offer_for_a_request_you'));
        }

        // BR-022 — الأهلية كاملة، ومنها المنع بسبب المستحقات (BR-063) في وضع السوق
        if (! $this->eligibility->isEligible($order, $provider)) {
            throw BusinessRuleViolationException::rule('BR-022', Localization::text('copy.you_are_not_eligible_for_this_request'));
        }
    }

    /** BR-030 — عرض نشط واحد، وإعادة التقديم مرة واحدة فقط (DEC-014). */
    private function assertNoActiveOffer(Order $order, ProviderProfile $provider): ?Offer
    {
        $offers = Offer::query()
            ->where('order_id', $order->getKey())
            ->where('provider_profile_id', $provider->getKey())
            ->orderBy('id')
            ->get();

        $active = $offers->firstWhere('status', OfferStatus::Submitted);

        if ($active !== null) {
            throw BusinessRuleViolationException::rule('BR-030', Localization::text('copy.you_already_have_an_active_offer_for_this_request'));
        }

        $withdrawn = $offers->where('status', OfferStatus::Withdrawn);

        if ($withdrawn->count() >= 2) {
            throw BusinessRuleViolationException::rule('BR-030', Localization::text('copy.an_offer_can_only_be_resubmitted_once'));
        }

        return $withdrawn->last();
    }

    /** BR-032 — السعر لا يقل عن CFG-012. */
    private function assertPrice(string $price): void
    {
        $min = (string) $this->settings->decimal(Cfg::MinOfferAmount);

        if (bccomp($price, $min, 2) < 0) {
            throw BusinessRuleViolationException::rule('BR-032', Localization::text('dynamic.offer_min', ['amount' => $min]));
        }
    }

    /** BR-032 — وقت الوصول لطلبات `NOW` فقط، بين 5 و180 دقيقة. */
    private function normalizeEta(Order $order, ?int $etaMinutes): ?int
    {
        if ($order->timing_type !== TimingType::Now) {
            return null;
        }

        if ($etaMinutes === null || $etaMinutes < 5 || $etaMinutes > 180) {
            throw BusinessRuleViolationException::rule('BR-032', Localization::text('copy.arrival_time_must_be_between_5_and_180_minutes'));
        }

        return $etaMinutes;
    }

    /** BR-032 / DEC-005 — خصم رسوم المعاينة إلزامي في طلب المعاينة فقط. */
    private function normalizeDeductible(Order $order, ?bool $deductible): ?bool
    {
        if ($order->pricing_mode !== PricingMode::Inspection) {
            return null;
        }

        if ($deductible === null) {
            throw BusinessRuleViolationException::rule('BR-032', Localization::text('copy.specify_whether_the_inspection_fee_is_deductible'));
        }

        return $deductible;
    }
}
