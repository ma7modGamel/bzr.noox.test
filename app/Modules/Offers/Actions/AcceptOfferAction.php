<?php

declare(strict_types=1);

namespace App\Modules\Offers\Actions;

use App\Modules\Communication\Enums\ConversationStatus;
use App\Modules\Communication\Models\Conversation;
use App\Modules\Communication\Services\ConversationService;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;

/**
 * O-03 / T-02 — قبول العرض (BR-034، BR-035).
 *
 * الاختيار يؤكد فورًا (DEC-006): العروض الأخرى ← `NOT_SELECTED`، محادثاتها ← للقراءة فقط،
 * وتُثبَّت نسبة العمولة على الطلب لحظة التأكيد (DEC-009).
 */
final readonly class AcceptOfferAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private FeatureGate $features,
        private SettingsRepository $settings,
        private ProviderEligibility $eligibility,
        private ConversationService $conversations,
    ) {}

    public function execute(Order $order, Offer $offer, int $customerId, PaymentMethod $paymentMethod): Order
    {
        $this->features->requireOffers('acceptOffer');

        if ($order->customer_id !== $customerId) {
            throw BusinessRuleViolationException::rule('BR-034', 'هذا الطلب ليس طلبك.');
        }

        $this->assertOfferIsSelectable($order, $offer);
        $this->assertProviderIsFree($order, $offer);

        return $this->stateMachine->apply(
            order: $order,
            action: 'acceptOffer',
            actorType: ActorType::Customer,
            actorId: $customerId,
            meta: [
                'offer_id' => $offer->getKey(),
                'payment_method' => $paymentMethod->value,
                'commission_rate' => $this->commissionRate(),
            ],
            mutate: function (Order $fresh) use ($offer, $paymentMethod): void {
                $offer->forceFill(['status' => OfferStatus::Accepted, 'decided_at' => now()])->save();

                // BR-035 — باقي العروض ومحادثاتها
                Offer::query()
                    ->where('order_id', $fresh->getKey())
                    ->whereKeyNot($offer->getKey())
                    ->where('status', OfferStatus::Submitted->value)
                    ->update(['status' => OfferStatus::NotSelected->value, 'decided_at' => now()]);

                Conversation::query()
                    ->where('order_id', $fresh->getKey())
                    ->where('provider_profile_id', '!=', $offer->provider_profile_id)
                    ->update(['status' => ConversationStatus::ReadOnly->value]);

                $fresh->provider_profile_id = $offer->provider_profile_id;
                $fresh->accepted_offer_id = $offer->getKey();
                $fresh->payment_method = $paymentMethod;
                $fresh->confirmed_at = now();
                $fresh->commission_rate = $this->commissionRate();

                $this->conversations->ensureAssignedConversation($fresh, $offer->provider_profile_id);
            },
            refType: 'offer',
            refId: $offer->getKey(),
        );
    }

    /** DEC-009 — النسبة تُثبَّت لحظة التأكيد فلا يؤثر تغييرها لاحقًا (EC-30). */
    private function commissionRate(): string
    {
        $rate = $this->settings->decimal(Cfg::DefaultCommissionRate);

        if ($rate === null) {
            // OD-01 — لا يُفعَّل وضع السوق قبل حسم النسبة (35)
            throw BusinessRuleViolationException::rule(
                'BR-060',
                'نسبة العمولة (CFG-062) غير محددة؛ يلزم حسم OD-01 قبل تفعيل وضع السوق.',
            );
        }

        return $rate;
    }

    private function assertOfferIsSelectable(Order $order, Offer $offer): void
    {
        if ($offer->order_id !== $order->getKey() || $offer->status !== OfferStatus::Submitted) {
            throw BusinessRuleViolationException::rule('BR-034', 'العرض لم يعد متاحًا للاختيار.');
        }

        if ($order->status !== OrderStatus::Open) {
            throw BusinessRuleViolationException::rule('BR-034', 'الطلب لم يعد مفتوحًا.');
        }

        if ($order->selection_deadline_at !== null && CarbonImmutable::now()->gt($order->selection_deadline_at)) {
            throw BusinessRuleViolationException::rule('BR-034', 'انتهت مهلة اختيار العرض.');
        }
    }

    /** BR-034 — الفني نشط، وبلا طلب NOW نشط أو فترة متداخلة. */
    private function assertProviderIsFree(Order $order, Offer $offer): void
    {
        $provider = $offer->providerProfile;

        if ($provider->status !== ProviderStatus::Active) {
            throw BusinessRuleViolationException::rule('BR-034', 'مقدم الخدمة لم يعد نشطًا.');
        }

        if (! $this->eligibility->isEligible($order, $provider)) {
            throw BusinessRuleViolationException::rule(
                'BR-034',
                'مقدم الخدمة مشغول بطلب آخر في نفس التوقيت. اختر عرضًا آخر.',
            );
        }
    }
}
