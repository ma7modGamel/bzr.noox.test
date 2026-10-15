<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Offers\Actions\SubmitOfferAction;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\PaymentGatewayChannel;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Services\PaymentChannels;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Modules\Support\Actions\RequestWarrantyAction;
use App\Modules\Support\Enums\DisputeStatus;
use App\Support\Localization;

/** حقول العرض التي يعتمد عليها التطبيقان بدل إعادة منطق الأعمال محليًا. */
final readonly class OrderPresentation
{
    public const CUSTOMER_ACTIONS = [
        'accept_offer', 'edit_request', 'republish', 'cancel', 'change_payment_method',
        'request_warranty',
        'pay_electronic', 'submit_instapay_transfer', 'approve_proposal', 'reject_proposal', 'confirm_completion',
        'open_dispute', 'rate', 'share_visit', 'call', 'chat', 'report_provider',
    ];

    public const PROVIDER_ACTIONS = [
        'upload_repair_before', 'upload_repair_after', 'update_completion_estimate', 'report_arrival_exception',
        'submit_offer', 'withdraw_offer', 'start_trip', 'back_out', 'mark_arrived',
        'start_work', 'submit_execution_quote', 'complete_inspection_only', 'submit_proposal',
        'withdraw_proposal', 'complete_work', 'report_unable', 'report_no_show',
        'confirm_cash', 'open_dispute', 'rate_customer', 'navigate', 'call', 'chat',
    ];

    public function __construct(
        private OrderStateMachine $stateMachine,
        private SettingsRepository $settings,
        private FeatureGate $features,
        private SubmitOfferAction $submitOffer,
        private PaymentChannels $channels,
    ) {}

    /** @return list<string> */
    public static function actionNames(): array
    {
        return array_values(array_unique([...self::CUSTOMER_ACTIONS, ...self::PROVIDER_ACTIONS]));
    }

    /** @return array{message: string, deadline_label: ?string, deadline_at: ?string}|null */
    public function waiting(Order $order): ?array
    {
        if ($order->status !== OrderStatus::Open) {
            return null;
        }

        if ($order->isWarrantyVisit() || $order->operating_mode === OperatingMode::Employee) {
            $message = $order->isWarrantyVisit() ? 'copy.waiting_warranty_assignment' : 'copy.waiting_assignment';
            $deadline = $order->selection_deadline_at;
            $label = 'copy.assignment_deadline';
        } elseif ($order->offers_close_at?->isFuture()) {
            $message = 'copy.waiting_offers';
            $deadline = $order->offers_close_at;
            $label = 'copy.offers_deadline';
        } else {
            $hasOffers = $order->offers->contains(fn ($offer): bool => $offer->status === OfferStatus::Submitted);
            $message = $hasOffers ? 'copy.waiting_selection' : 'copy.waiting_no_offers';
            $deadline = $hasOffers ? $order->selection_deadline_at : null;
            $label = 'copy.selection_deadline';
        }

        if ($deadline?->isPast()) {
            $message = 'copy.waiting_deadline_elapsed';
        }

        return [
            'message' => Localization::text($message),
            'deadline_label' => $deadline === null ? null : Localization::text($label),
            'deadline_at' => $deadline?->toIso8601String(),
        ];
    }

    /** @return list<string> */
    public function availableActions(Order $order, User $user, ActorType $actor): array
    {
        $order->loadMissing(['offers', 'pendingProposalRecord', 'review', 'customerRating', 'conversations', 'disputes']);

        return $actor === ActorType::Provider
            ? $this->providerActions($order, $user)
            : $this->customerActions($order, $user);
    }

    /** @return array<string, ?string> */
    public function deadlines(Order $order): array
    {
        $order->loadMissing(['pendingProposalRecord', 'latestSuccessfulPayment']);

        return [
            'offers_close_at' => $order->status === OrderStatus::Open
                ? $order->offers_close_at?->toIso8601String()
                : null,
            'selection_deadline_at' => $order->status === OrderStatus::Open
                ? $order->selection_deadline_at?->toIso8601String()
                : null,
            'proposal_expires_at' => $order->pendingProposalRecord?->expires_at?->toIso8601String(),
            'no_show_allowed_at' => $order->status === OrderStatus::Arrived
                ? $order->arrived_at?->addMinutes($this->settings->int(Cfg::CustomerNoShowWaitMinutes))->toIso8601String()
                : null,
            'auto_close_at' => $order->status === OrderStatus::AwaitingConfirmation
                ? $order->latestSuccessfulPayment?->paid_at?->addHours($this->settings->int(Cfg::AutoCloseHours))->toIso8601String()
                : null,
        ];
    }

    public function displayStatus(Order $order, ActorType $actor): string
    {
        if ($order->status === OrderStatus::AwaitingPayment && $this->channels->pendingTransfer($order) !== null) {
            return 'order.status.'.strtolower($actor->value).'.AWAITING_TRANSFER_VERIFICATION'; // BR-057
        }

        return 'order.status.'.strtolower($actor->value).'.'.$order->status->value;
    }

    public function phoneVisible(Order $order): bool
    {
        return false;
    }

    /** @return list<array{key: string, state: string}>|null */
    public function stepper(Order $order): ?array
    {
        if (in_array($order->status, [
            OrderStatus::Open,
            OrderStatus::Cancelled,
            OrderStatus::Expired,
        ], true)) {
            return null;
        }

        $status = $order->status === OrderStatus::Disputed ? $order->disputed_from_status : $order->status;

        if ($status === null) {
            return null;
        }

        $keys = ['confirmed', 'on_the_way', 'arrived', 'in_progress', 'payment', 'closed'];
        $active = match ($status) {
            OrderStatus::Confirmed, OrderStatus::OnTheWay => 1,
            OrderStatus::Arrived, OrderStatus::AwaitingQuoteApproval => 2,
            OrderStatus::InProgress => 3,
            OrderStatus::AwaitingPayment => 4,
            OrderStatus::AwaitingConfirmation => 5,
            OrderStatus::Closed => 6,
            default => 0,
        };

        $onHold = $order->status === OrderStatus::Disputed;

        return array_map(static fn (string $key, int $index): array => [
            'key' => $key,
            'state' => $index < $active
                ? 'done'
                : ($index === $active ? ($onHold ? 'on_hold' : 'active') : 'pending'),
        ], $keys, array_keys($keys));
    }

    /** @return list<string> */
    private function customerActions(Order $order, User $user): array
    {
        if ($order->customer_id !== $user->getKey()) {
            return [];
        }

        $actions = [];

        $hasSubmittedOffers = $order->offers->contains(
            fn ($offer): bool => $offer->status === OfferStatus::Submitted,
        );
        $hasAnyOffers = $order->offers->isNotEmpty();

        if (! $order->isWarrantyVisit() && $order->operating_mode === OperatingMode::Marketplace
            && $order->status === OrderStatus::Open && ! $hasAnyOffers) {
            $actions[] = 'edit_request';

            if ($order->offers_close_at?->isPast()) {
                $actions[] = 'republish';
            }
        }

        if (! $order->isWarrantyVisit() && $order->operating_mode === OperatingMode::Marketplace
            && $order->status === OrderStatus::Expired && ! $hasSubmittedOffers) {
            $actions[] = 'republish';
        }

        if ($this->stateMachine->can($order, 'acceptOffer', ActorType::Customer)
            && $hasSubmittedOffers
            && ($order->selection_deadline_at === null || now()->isBefore($order->selection_deadline_at))) {
            $actions[] = 'accept_offer';
        }

        if ($this->stateMachine->can($order, 'cancelOrder', ActorType::Customer)
            || $this->stateMachine->can($order, 'cancelConfirmedOrder', ActorType::Customer)) {
            $actions[] = 'cancel';
        }

        if ($order->pendingProposalRecord !== null) {
            if ($order->status === OrderStatus::InProgress
                && in_array($order->pendingProposalRecord->type, [ProposalType::Materials, ProposalType::ExtraWork], true)
                && $order->pendingProposalRecord->expires_at->isFuture()) {
                $actions[] = 'approve_proposal';
                $actions[] = 'reject_proposal';
            }
            if ($this->stateMachine->can($order, 'approveExecutionQuote', ActorType::Customer)) {
                $actions[] = 'approve_proposal';
            }
            if ($this->stateMachine->can($order, 'rejectExecutionQuote', ActorType::Customer)) {
                $actions[] = 'reject_proposal';
            }
        }

        if ($this->stateMachine->can($order, 'confirmCompletion', ActorType::Customer)) {
            $actions[] = 'confirm_completion';
        }

        if ($this->stateMachine->can($order, 'openDispute', ActorType::Customer)
            || $this->postCloseDisputeIsOpen($order)) {
            $actions[] = 'open_dispute';
        }

        if ($order->status === OrderStatus::AwaitingPayment && $this->channels->pendingTransfer($order) === null) {
            // BR-050 / BR-051 / BR-057 — القنوات المفعّلة فقط، ولا تغيير أثناء انتظار تأكيد تحويل.
            $actions[] = 'change_payment_method';

            if ($order->payment_method === PaymentMethod::Electronic) {
                if ($this->channels->isEnabled(PaymentGatewayChannel::Fawry)) {
                    $actions[] = 'pay_electronic';
                }

                if ($this->channels->isEnabled(PaymentGatewayChannel::InstapayManual)) {
                    $actions[] = 'submit_instapay_transfer';
                }
            }
        }

        if ($this->ratingIsOpen($order) && $order->review === null) {
            $actions[] = 'rate';
        }

        if (RequestWarrantyAction::eligible($order)) {
            $actions[] = 'request_warranty';
        }

        if ($order->provider_profile_id !== null && ! $order->status->isFinal()) {
            $actions[] = 'share_visit';

            if ($this->phoneVisible($order)) {
                $actions[] = 'call';
            }

            $actions[] = 'chat';
        } elseif ($order->provider_profile_id !== null) {
            if ($this->phoneVisible($order)) {
                $actions[] = 'call';
            }

            if ($order->conversations->contains('provider_profile_id', $order->provider_profile_id)) {
                $actions[] = 'chat';
            }
        } elseif ($order->status === OrderStatus::Open
            && $order->offers->contains(fn ($offer): bool => $offer->status === OfferStatus::Submitted)) {
            $actions[] = 'chat';
        }

        return array_values(array_unique($actions));
    }

    /** @return list<string> */
    private function providerActions(Order $order, User $user): array
    {
        $profile = $user->providerProfile;

        if ($profile === null) {
            return [];
        }

        if ($order->status === OrderStatus::Open && $order->provider_profile_id === null) {
            $actions = [];

            $ownOffer = $order->offers->first(
                fn ($offer): bool => $offer->provider_profile_id === $profile->getKey()
                    && $offer->status === OfferStatus::Submitted,
            );

            if ($this->features->offersEnabled() && $this->submitOffer->canSubmit($order, $profile)) {
                $actions[] = 'submit_offer';
            }

            if ($this->features->offersEnabled() && $ownOffer !== null) {
                $actions[] = 'withdraw_offer';
            }

            if ($order->conversations->contains('provider_profile_id', $profile->getKey())) {
                $actions[] = 'chat';
            }

            return $actions;
        }

        if ($order->provider_profile_id !== $profile->getKey()) {
            return [];
        }

        $actions = match ($order->status) {
            OrderStatus::Confirmed => $this->tripMayStart($order)
                ? ['start_trip', 'back_out']
                : ['back_out'],
            OrderStatus::OnTheWay => ['mark_arrived', 'back_out', 'navigate'],
            OrderStatus::Arrived => $order->pricing_mode === PricingMode::Inspection
                ? ['submit_execution_quote', 'complete_inspection_only', 'report_unable']
                : ['start_work', 'report_unable'],
            OrderStatus::InProgress => $order->pendingProposalRecord === null
                ? ['submit_proposal', 'complete_work', 'report_unable']
                : ['report_unable'],
            OrderStatus::AwaitingPayment => $this->channels->pendingTransfer($order) === null ? ['confirm_cash'] : [],
            default => [],
        };

        if ($order->hasPendingNoShowReport()) {
            $actions = [];
        }

        if ($order->status === OrderStatus::OnTheWay && $order->operating_mode === OperatingMode::Employee && $order->arrival_exception_report === null) {
            $actions[] = 'report_arrival_exception';
        }

        if ($order->status === OrderStatus::InProgress && ! $order->hasPendingNoShowReport()) {
            $actions[] = 'update_completion_estimate';
        }

        if (! $order->hasPendingNoShowReport()) {
            if (in_array($order->status, [OrderStatus::Arrived, OrderStatus::AwaitingQuoteApproval, OrderStatus::InProgress], true)
                && $order->media->where('purpose', 'REPAIR_BEFORE')->count() < 5) {
                $actions[] = 'upload_repair_before';
            }
            if ($order->status === OrderStatus::InProgress && $order->media->where('purpose', 'REPAIR_AFTER')->count() < 5) {
                $actions[] = 'upload_repair_after';
            }
        }

        if ($order->status === OrderStatus::Arrived
            && ! $order->hasPendingNoShowReport()
            && $order->arrived_at?->addMinutes($this->settings->int(Cfg::CustomerNoShowWaitMinutes))->lte(now())) {
            $actions[] = 'report_no_show';
        }

        if ($this->ratingIsOpen($order) && $order->customerRating === null) {
            $actions[] = 'rate_customer';
        }

        if ($this->phoneVisible($order)) {
            $actions[] = 'call';
        }

        if (! $order->status->isFinal()
            || $order->conversations->contains('provider_profile_id', $profile->getKey())) {
            $actions[] = 'chat';
        }

        return array_values(array_unique($actions));
    }

    private function ratingIsOpen(Order $order): bool
    {
        return $order->status === OrderStatus::Closed
            && $order->closed_at !== null
            && now()->lte($order->closed_at->addDays($this->settings->int(Cfg::RatingWindowDays)));
    }

    private function postCloseDisputeIsOpen(Order $order): bool
    {
        return $order->status === OrderStatus::Closed
            && $order->closed_at !== null
            && ! $order->disputes->contains(
                fn ($dispute): bool => $dispute->status === DisputeStatus::Open,
            )
            && now()->lte($order->closed_at->addHours($this->settings->int(Cfg::DisputeWindowHours)));
    }

    private function tripMayStart(Order $order): bool
    {
        return $order->timing_type !== TimingType::Scheduled
            || $order->slot_start === null
            || now()->gte($order->slot_start->subMinutes($this->settings->int(Cfg::MinLeadMinutesBeforeSlot)));
    }
}
