<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

/**
 * قرار العميل على مقترح السعر — 12، T-14 / T-15 / T-28.
 *
 * - عرض تنفيذ موافق عليه ← `IN_PROGRESS` (T-14).
 * - عرض تنفيذ مرفوض أو منتهي المهلة ← الدفع برسوم المعاينة (T-15)، أو إغلاق بلا مبلغ إن كانت مجانية (T-28، BR-056).
 * - خامات/أعمال إضافية ← قرار بلا تغيير حالة؛ الرفض لا يلغي الطلب (12).
 */
final readonly class DecideProposalAction
{
    public function __construct(private OrderStateMachine $stateMachine) {}

    public function approve(Order $order, PriceProposal $proposal, ActorType $actor, ?int $actorId = null): Order
    {
        $this->assertDecidable($order, $proposal);
        $this->assertWarrantyConsent($order, $proposal, $actor, $actorId);
        if ($order->isWarrantyVisit() && $actor !== ActorType::Customer) {
            throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_materials_consent_required'));
        }

        if ($proposal->type === ProposalType::ExecutionQuote) {
            return $this->stateMachine->apply(
                order: $order,
                action: 'approveExecutionQuote',
                actorType: $actor,
                actorId: $actorId,
                meta: ['amount' => (string) $proposal->amount],
                mutate: function (Order $fresh) use ($proposal, $actor): void {
                    $this->settle($proposal, ProposalStatus::Approved, $actor);
                    $fresh->work_started_at = now();
                },
                refType: 'proposal',
                refId: $proposal->getKey(),
            );
        }

        return $this->decideInPlace($order, $proposal, ProposalStatus::Approved, $actor, $actorId, OrderEventCode::ProposalApproved);
    }

    public function reject(Order $order, PriceProposal $proposal, ActorType $actor, ?int $actorId = null): Order
    {
        return $this->close($order, $proposal, ProposalStatus::Rejected, $actor, $actorId, OrderEventCode::ProposalRejected);
    }

    /** انتهاء المهلة = رفض (BR-041، DEC-029) — ينفذه المجدول باسم النظام. */
    public function expire(Order $order, PriceProposal $proposal): Order
    {
        return $this->close($order, $proposal, ProposalStatus::Expired, ActorType::System, null, OrderEventCode::ProposalExpired);
    }

    private function close(
        Order $order,
        PriceProposal $proposal,
        ProposalStatus $status,
        ActorType $actor,
        ?int $actorId,
        OrderEventCode $event,
    ): Order {
        $this->assertDecidable($order, $proposal);
        $this->assertWarrantyConsent($order, $proposal, $actor, $actorId);

        if ($proposal->type !== ProposalType::ExecutionQuote) {
            return $this->decideInPlace($order, $proposal, $status, $actor, $actorId, $event);
        }

        // رفض عرض التنفيذ: المبلغ المستحق هو رسوم المعاينة وحدها (BR-044)
        $this->settle($proposal, $status, $actor);
        $amounts = OrderAmounts::for($order->refresh());

        if ($amounts->isZero()) {
            // BR-056 — معاينة مجانية: إغلاق مباشر بلا دفع
            return $this->stateMachine->apply(
                order: $order,
                action: 'closeFreeInspection',
                actorType: $actor,
                actorId: $actorId,
                meta: ['final_amount' => '0.00', 'rule' => 'BR-056', 'proposal_status' => $status->value],
                mutate: function (Order $fresh): void {
                    $fresh->labor_total = '0.00';
                    $fresh->materials_total = '0.00';
                    $fresh->final_amount = '0.00';
                    $fresh->commission_amount = '0.00';
                    $fresh->payment_status = OrderPaymentStatus::Waived;
                    $fresh->completed_at = now();
                    $fresh->closed_at = now();
                },
                refType: 'proposal',
                refId: $proposal->getKey(),
            );
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'rejectExecutionQuote',
            actorType: $actor,
            actorId: $actorId,
            meta: $amounts->toColumns() + ['proposal_status' => $status->value],
            mutate: function (Order $fresh) use ($amounts): void {
                $fresh->fill($amounts->toColumns());
                $fresh->completed_at = now();
            },
            refType: 'proposal',
            refId: $proposal->getKey(),
        );
    }

    /** خامات/أعمال إضافية: قرار يُسجَّل بلا انتقال (12). */
    private function decideInPlace(
        Order $order,
        PriceProposal $proposal,
        ProposalStatus $status,
        ActorType $actor,
        ?int $actorId,
        OrderEventCode $event,
    ): Order {
        DB::transaction(function () use ($order, $proposal, $status, $actor, $actorId, $event): void {
            $order = Order::query()->lockForUpdate()->findOrFail($order->id);
            $proposal = PriceProposal::query()->lockForUpdate()->findOrFail($proposal->id);
            $this->assertDecidable($order, $proposal);
            $this->assertWarrantyConsent($order, $proposal, $actor, $actorId);
            if ($order->status !== OrderStatus::InProgress
                || ($actor !== ActorType::System && ($actor !== ActorType::Customer || $actorId !== $order->customer_id))
                || ($status === ProposalStatus::Approved && $actor !== ActorType::Customer)
                || ($actor === ActorType::Customer && ! $proposal->expires_at->isFuture())) {
                throw BusinessRuleViolationException::rule('BR-041', Localization::text('copy.proposal_customer_decision_required'));
            }
            if ($order->isWarrantyVisit() && $status === ProposalStatus::Approved && $actor !== ActorType::Customer) {
                throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_materials_consent_required'));
            }
            $this->settle($proposal, $status, $actor);
            $order->increment('version');

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => $event,
                'actor_type' => $actor,
                'actor_id' => $actorId,
                'from_status' => $order->status,
                'to_status' => $order->status,
                'ref_type' => 'proposal',
                'ref_id' => $proposal->getKey(),
                'meta' => ['type' => $proposal->type->value, 'amount' => (string) $proposal->amount],
                'created_at' => now(),
            ]);
        });

        return $order->refresh();
    }

    private function settle(PriceProposal $proposal, ProposalStatus $status, ActorType $actor): void
    {
        $proposal->forceFill([
            'status' => $status,
            'decided_at' => now(),
            'decided_by_type' => $actor,
        ])->save();
    }

    private function assertWarrantyConsent(Order $order, PriceProposal $proposal, ActorType $actor, ?int $actorId): void
    {
        if ($order->isWarrantyVisit() && ($proposal->type !== ProposalType::Materials
            || $order->status !== OrderStatus::InProgress
            || ($actor !== ActorType::System && ($actor !== ActorType::Customer || $actorId !== $order->customer_id)))) {
            throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_materials_consent_required'));
        }
    }

    private function assertDecidable(Order $order, PriceProposal $proposal): void
    {
        if ($proposal->order_id !== $order->getKey()) {
            throw BusinessRuleViolationException::rule('BR-041', Localization::text('copy.the_proposal_does_not_belong_to_this_request'));
        }

        if (! $proposal->isPending()) {
            throw BusinessRuleViolationException::rule('BR-041', Localization::text('copy.the_proposal_is_no_longer_pending'));
        }
    }
}
