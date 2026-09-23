<?php

declare(strict_types=1);

namespace App\Modules\Orders\StateMachine;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode as E;
use App\Modules\Orders\Enums\OrderStatus as S;

/**
 * جدول الانتقالات T-01..T-29 — 10-ORDER-LIFECYCLE هو المرجع.
 * أي انتقال غير مذكور هنا **مرفوض** (`INVALID_TRANSITION`).
 *
 * تعديل هذا الجدول = تعديل الوثيقة. لا يُضاف انتقال بلا صف في 10.
 */
final class TransitionTable
{
    /** @var array<string, Transition>|null */
    private static ?array $byAction = null;

    /** @return list<Transition> */
    public static function all(): array
    {
        $C = ActorType::Customer;
        $P = ActorType::Provider;
        $A = ActorType::Admin;
        $Y = ActorType::System;

        return [
            new Transition('T-02', 'acceptOffer', [S::Open], S::Confirmed, [$C], E::OfferAccepted, requiresOffers: true, note: 'BR-034'),
            new Transition('T-03', 'expireOrder', [S::Open], S::Expired, [$Y], E::ExpiredWithoutSelection, note: 'selection_deadline_at — CFG-013 أو CFG-093'),
            new Transition('T-04', 'cancelOrder', [S::Open], S::Cancelled, [$C, $A], E::Cancelled, note: 'BR-070 — سبب إلزامي'),
            new Transition('T-05', 'startTrip', [S::Confirmed], S::OnTheWay, [$P], E::TripStarted, note: 'صلاحية الموقع؛ المجدول: now ≥ slot_start − 90د'),
            new Transition('T-06', 'backOut', [S::Confirmed, S::OnTheWay], S::Open, [$P, $A], E::ProviderBackedOut, note: 'BR-036 — يشمل T-07'),
            new Transition('T-08', 'cancelConfirmedOrder', [S::Confirmed, S::OnTheWay], S::Cancelled, [$C, $A], E::Cancelled, note: 'BR-070 — يشمل T-09'),
            new Transition('T-10', 'markArrived', [S::OnTheWay], S::Arrived, [$P], E::Arrived, note: 'BR-111 — يُرسل الموقع'),
            new Transition('T-11', 'startWork', [S::Arrived], S::InProgress, [$P], E::WorkStarted, note: 'pricing_mode = EXECUTION'),
            new Transition('T-12', 'submitExecutionQuote', [S::Arrived], S::AwaitingQuoteApproval, [$P], E::ProposalSubmitted, note: 'INSPECTION، BR-041..043'),
            new Transition('T-13', 'completeInspectionOnly', [S::Arrived], S::AwaitingPayment, [$P], E::InspectionOnlyCompleted, note: 'رسوم المعاينة > 0'),
            new Transition('T-14', 'approveExecutionQuote', [S::AwaitingQuoteApproval], S::InProgress, [$C], E::ProposalApproved),
            new Transition('T-15', 'rejectExecutionQuote', [S::AwaitingQuoteApproval], S::AwaitingPayment, [$C, $Y], E::ProposalRejected, note: 'رسوم المعاينة > 0'),
            new Transition('T-16', 'reportUnableAtArrival', [S::Arrived], S::Cancelled, [$P], E::UnableToPerform, note: 'تعذّر / عميل غير موجود بعد CFG-033'),
            new Transition('T-17', 'completeWork', [S::InProgress], S::AwaitingPayment, [$P], E::WorkCompleted, note: 'BR-045'),
            new Transition('T-18', 'reportUnableInProgress', [S::InProgress], S::Cancelled, [$P], E::UnableToPerform),
            new Transition('T-19', 'confirmCashReceived', [S::AwaitingPayment], S::AwaitingConfirmation, [$P, $A], E::CashReceived, note: 'المبلغ = final_amount'),
            new Transition('T-20', 'settleElectronicPayment', [S::AwaitingPayment], S::Closed, [$Y], E::ElectronicPaymentSucceeded, note: 'توقيع صحيح ومبلغ مطابق'),
            new Transition('T-21', 'confirmCompletion', [S::AwaitingConfirmation], S::Closed, [$C, $Y], E::CompletionConfirmed, note: 'أو مهلة CFG-051'),
            new Transition('T-22', 'openDispute', [S::Arrived, S::AwaitingQuoteApproval, S::InProgress, S::AwaitingPayment, S::AwaitingConfirmation], S::Disputed, [$C, $P], E::DisputeOpened, note: 'BR-120'),
            new Transition('T-23', 'resolveDisputeClose', [S::Disputed], S::Closed, [$A], E::DisputeResolved),
            new Transition('T-24', 'resolveDisputeCancel', [S::Disputed], S::Cancelled, [$A], E::DisputeResolved, note: 'استرداد كامل لأي دفع إلكتروني'),
            new Transition('T-25', 'adminCloseWithoutPayment', [S::AwaitingPayment], S::Closed, [$A], E::ClosedWithoutPayment, note: 'BR-055 — العمولة = 0'),
            new Transition('T-26', 'adminCancel', [S::Open, S::Confirmed, S::OnTheWay, S::Arrived, S::AwaitingQuoteApproval, S::InProgress], S::Cancelled, [$A], E::Cancelled),

            // ── وضع الموظفين — 39-OPERATING-MODES ──────────────────────────
            new Transition('T-27', 'assignProvider', [S::Open], S::Confirmed, [$A], E::ProviderAssigned, requiresOffers: false, note: 'BR-007'),
            new Transition('T-28', 'closeFreeInspection', [S::Arrived, S::AwaitingQuoteApproval], S::Closed, [$P, $C, $Y], E::ClosedFreeInspection, note: 'BR-056 — final_amount = 0'),
            new Transition('T-29', 'reassignProvider', [S::Confirmed, S::OnTheWay], S::Confirmed, [$A], E::ProviderAssigned, requiresOffers: false, note: 'T-06 + T-27 في معاملة واحدة'),
        ];
    }

    public static function find(string $action): ?Transition
    {
        self::$byAction ??= collect(self::all())->keyBy('action')->all();

        return self::$byAction[$action] ?? null;
    }

    /** @return list<Transition> */
    public static function fromStatus(S $status): array
    {
        return array_values(array_filter(self::all(), fn (Transition $t) => $t->allowsFrom($status)));
    }
}
