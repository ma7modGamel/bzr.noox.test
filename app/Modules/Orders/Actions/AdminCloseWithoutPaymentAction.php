<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Support\Exceptions\BusinessRuleViolationException;

/** T-25 — إغلاق بدون دفع بقرار الإدارة مع سبب؛ العمولة = 0 (BR-055). */
final readonly class AdminCloseWithoutPaymentAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderClosure $closure,
    ) {}

    public function execute(Order $order, Admin $admin, string $reason): Order
    {
        if (mb_strlen(trim($reason)) < 5) {
            throw BusinessRuleViolationException::rule('BR-055', 'سبب الإغلاق بدون دفع إلزامي.');
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'adminCloseWithoutPayment',
            actorType: ActorType::Admin,
            actorId: $admin->getKey(),
            meta: ['reason' => $reason, 'rule' => 'BR-055'],
            mutate: function (Order $fresh) use ($reason): void {
                $fresh->payment_status = OrderPaymentStatus::Waived;
                $fresh->cancel_note = $reason;
                $this->closure->apply($fresh, withCommission: false);
            },
        );
    }
}
