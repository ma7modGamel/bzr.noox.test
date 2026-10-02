<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Pricing\Enums\PriceReviewReason;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonInterface;
use Illuminate\Support\Facades\DB;

/**
 * تقديم مقترح سعر — 12، BR-040..BR-043.
 *
 * `EXECUTION_QUOTE` ينقل الطلب إلى `AWAITING_QUOTE_APPROVAL` (T-12).
 * `MATERIALS` و`EXTRA_WORK` لا يغيّران حالة الطلب؛ البند الإضافي يتوقف حتى القرار (DEC-007).
 */
final readonly class SubmitProposalAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private SettingsRepository $settings,
    ) {}

    public function execute(
        Order $order,
        ProviderProfile $provider,
        ProposalType $type,
        string $amount,
        string $reason,
        ?string $photoPath = null,
        ?string $outsidePriceGuideReason = null,
    ): PriceProposal {
        $this->assertOwnership($order, $provider);
        $this->assertAmountAndReason($amount, $reason);
        $this->assertNoPendingProposal($order);
        $this->assertTypeIsAllowed($order, $type, $amount);
        $priceGuide = $this->priceGuideDecision($order, $type, $amount, $outsidePriceGuideReason);

        $expiresAt = now()->addMinutes($this->settings->int(Cfg::ProposalTimeoutMinutes));

        if ($type === ProposalType::ExecutionQuote) {
            $proposal = null;

            $this->stateMachine->apply(
                order: $order,
                action: 'submitExecutionQuote',
                actorType: ActorType::Provider,
                actorId: $provider->getKey(),
                meta: ['type' => $type->value, 'amount' => $amount],
                mutate: function (Order $fresh) use (&$proposal, $provider, $type, $amount, $reason, $photoPath, $expiresAt, $priceGuide): void {
                    $proposal = $this->createProposal($fresh, $provider, $type, $amount, $reason, $photoPath, $expiresAt, $priceGuide);
                },
                refType: 'proposal',
            );

            return $proposal->refresh();
        }

        // خامات / أعمال إضافية: سجل وحدث بلا انتقال حالة
        return DB::transaction(function () use ($order, $provider, $type, $amount, $reason, $photoPath, $expiresAt, $priceGuide): PriceProposal {
            $proposal = $this->createProposal($order, $provider, $type, $amount, $reason, $photoPath, $expiresAt, $priceGuide);

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::ProposalSubmitted,
                'actor_type' => ActorType::Provider,
                'actor_id' => $provider->getKey(),
                'from_status' => $order->status,
                'to_status' => $order->status,
                'ref_type' => 'proposal',
                'ref_id' => $proposal->getKey(),
                'meta' => ['type' => $type->value, 'amount' => $amount],
                'created_at' => now(),
            ]);

            return $proposal;
        });
    }

    private function createProposal(
        Order $order,
        ProviderProfile $provider,
        ProposalType $type,
        string $amount,
        string $reason,
        ?string $photoPath,
        CarbonInterface $expiresAt,
        array $priceGuide,
    ): PriceProposal {
        return PriceProposal::query()->create([
            'order_id' => $order->getKey(),
            'provider_profile_id' => $provider->getKey(),
            'type' => $type,
            'amount' => $amount,
            'reason' => $reason,
            'photo_path' => $photoPath,
            'price_guide_min' => $priceGuide['minimum'],
            'price_guide_max' => $priceGuide['maximum'],
            'outside_price_guide' => $priceGuide['requires_review'],
            'price_review_reason' => $priceGuide['review_reason'],
            'outside_price_guide_reason' => $priceGuide['outside_reason'],
            'status' => ProposalStatus::Pending,
            'expires_at' => $expiresAt,
        ]);
    }

    /**
     * BR-046 — نطاق الموظفين يُنسخ مع العرض. «مشكلة أخرى» معفاة من النطاق، لكن كل أسعارها تُراجع.
     *
     * @return array{minimum: ?string, maximum: ?string, requires_review: bool, review_reason: ?PriceReviewReason, outside_reason: ?string}
     */
    private function priceGuideDecision(
        Order $order,
        ProposalType $type,
        string $amount,
        ?string $outsidePriceGuideReason,
    ): array {
        $empty = [
            'minimum' => null,
            'maximum' => null,
            'requires_review' => false,
            'review_reason' => null,
            'outside_reason' => null,
        ];

        if ($type !== ProposalType::ExecutionQuote || $order->operating_mode !== OperatingMode::Employee) {
            return $empty;
        }

        $problemType = $order->problemType()->firstOrFail();
        $minimum = $problemType->employee_price_min;
        $maximum = $problemType->employee_price_max;

        if ($problemType->is_other) {
            return [
                'minimum' => $minimum,
                'maximum' => $maximum,
                'requires_review' => true,
                'review_reason' => PriceReviewReason::OtherProblem,
                'outside_reason' => null,
            ];
        }

        if ($minimum === null || $maximum === null) {
            throw BusinessRuleViolationException::rule(
                'BR-046',
                'لا يوجد دليل سعر فعّال لنوع المشكلة.',
                ['reason' => 'PRICE_GUIDE_MISSING'],
            );
        }

        $isOutside = bccomp($amount, $minimum, 2) < 0 || bccomp($amount, $maximum, 2) > 0;

        if (! $isOutside) {
            return [
                'minimum' => $minimum,
                'maximum' => $maximum,
                'requires_review' => false,
                'review_reason' => null,
                'outside_reason' => null,
            ];
        }

        $outsideReason = trim((string) $outsidePriceGuideReason);

        if (mb_strlen($outsideReason) < 10 || mb_strlen($outsideReason) > 300) {
            throw BusinessRuleViolationException::rule(
                'BR-046',
                'سبب السعر خارج دليل الأسعار يجب أن يكون بين 10 و300 حرف.',
                ['reason' => 'OUTSIDE_PRICE_GUIDE_REASON_REQUIRED'],
            );
        }

        return [
            'minimum' => $minimum,
            'maximum' => $maximum,
            'requires_review' => true,
            'review_reason' => PriceReviewReason::OutsideRange,
            'outside_reason' => $outsideReason,
        ];
    }

    private function assertOwnership(Order $order, ProviderProfile $provider): void
    {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-041', 'هذا الطلب غير مسنَد إليك.');
        }
    }

    /** BR-041 — المبلغ > 0، والسبب 5 أحرف على الأقل و300 على الأكثر. */
    private function assertAmountAndReason(string $amount, string $reason): void
    {
        if (bccomp($amount, '0.00', 2) <= 0) {
            throw BusinessRuleViolationException::rule('BR-041', 'مبلغ المقترح يجب أن يكون أكبر من صفر.');
        }

        $length = mb_strlen(trim($reason));

        if ($length < 5 || $length > 300) {
            throw BusinessRuleViolationException::rule('BR-041', 'سبب المقترح بين 5 و300 حرف.');
        }
    }

    /** BR-041 — مقترح معلّق واحد فقط على الطلب (يحرسه قيد فريد في 29 أيضًا). */
    private function assertNoPendingProposal(Order $order): void
    {
        if ($order->hasPendingProposal()) {
            throw BusinessRuleViolationException::rule('BR-041', 'يوجد مقترح معلّق على هذا الطلب.');
        }
    }

    /** BR-042 و BR-043. */
    private function assertTypeIsAllowed(Order $order, ProposalType $type, string $amount): void
    {
        if ($type === ProposalType::ExecutionQuote) {
            if ($order->pricing_mode !== PricingMode::Inspection) {
                throw BusinessRuleViolationException::rule('BR-042', 'عرض التنفيذ يخص طلبات المعاينة فقط.');
            }

            if ($order->status !== OrderStatus::Arrived) {
                throw BusinessRuleViolationException::rule('BR-042', 'عرض التنفيذ يُقدَّم بعد الوصول.');
            }

            // مرة واحدة مهما كانت نتيجته (BR-042)
            if ($order->hasExecutionQuote()) {
                throw BusinessRuleViolationException::rule('BR-042', 'عرض التنفيذ يُقدَّم مرة واحدة فقط.');
            }

            $this->assertQuoteCoversInspectionFee($order, $amount);

            return;
        }

        if ($order->status !== OrderStatus::InProgress) {
            throw BusinessRuleViolationException::rule(
                'BR-042',
                'مقترحات الخامات والأعمال الإضافية تُقدَّم أثناء التنفيذ فقط.',
            );
        }
    }

    /** BR-043 — إن كانت رسوم المعاينة تُخصم فعرض التنفيذ ≥ الرسوم. لا أثر لها حين تكون المعاينة مجانية. */
    private function assertQuoteCoversInspectionFee(Order $order, string $amount): void
    {
        $offer = $order->acceptedOffer;

        if ($offer === null || ! $offer->inspection_fee_deductible) {
            return;
        }

        if (bccomp($amount, (string) $offer->price, 2) < 0) {
            throw BusinessRuleViolationException::rule(
                'BR-043',
                'عرض التنفيذ لا يقل عن رسوم المعاينة المخصومة ('.$offer->price.' جنيه).',
            );
        }
    }
}
