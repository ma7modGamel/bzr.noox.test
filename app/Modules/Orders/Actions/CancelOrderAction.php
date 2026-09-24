<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * الإلغاء — 13، BR-070..BR-072. لا رسوم ولا غرامات (DEC-016).
 *
 * العميل: مجانًا حتى `ON_THE_WAY` (T-04 / T-08). بعد الوصول: فتح مشكلة فقط.
 * الإدارة: حتى `IN_PROGRESS` (T-26).
 */
final readonly class CancelOrderAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderTermination $termination,
    ) {}

    public function execute(
        Order $order,
        ActorType $actor,
        ?int $actorId,
        CancelReason $reason,
        ?string $note = null,
    ): Order {
        $this->assertActorMayCancel($order, $actor, $actorId);

        return $this->stateMachine->apply(
            order: $order,
            action: $this->actionFor($order, $actor),
            actorType: $actor,
            actorId: $actorId,
            meta: ['reason_code' => $reason->value, 'note' => $note],
            mutate: function (Order $fresh) use ($actor, $actorId, $reason, $note): void {
                $fresh->cancelled_at = now();
                $fresh->cancelled_by_type = $actor->value;
                $fresh->cancelled_by_id = $actorId;
                $fresh->cancel_reason_code = $reason;
                $fresh->cancel_note = $note;

                $this->termination->apply($fresh);
            },
        );
    }

    /** BR-070 — العميل لا يلغي بعد الوصول؛ "عندي مشكلة" هو المسار (DEC-016). */
    private function assertActorMayCancel(Order $order, ActorType $actor, ?int $actorId): void
    {
        if ($actor !== ActorType::Customer) {
            return;
        }

        if ($order->customer_id !== $actorId) {
            throw BusinessRuleViolationException::rule('BR-070', 'هذا الطلب ليس طلبك.');
        }

        $allowed = [OrderStatus::Open, OrderStatus::Confirmed, OrderStatus::OnTheWay];

        if (! in_array($order->status, $allowed, true)) {
            throw BusinessRuleViolationException::rule(
                'BR-070',
                'الإلغاء غير متاح بعد وصول الفني. استخدم "عندي مشكلة".',
            );
        }
    }

    private function actionFor(Order $order, ActorType $actor): string
    {
        if ($actor === ActorType::Admin) {
            return 'adminCancel';
        }

        return $order->status === OrderStatus::Open ? 'cancelOrder' : 'cancelConfirmedOrder';
    }
}
