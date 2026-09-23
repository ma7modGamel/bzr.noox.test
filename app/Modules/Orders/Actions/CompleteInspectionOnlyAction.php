<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * إنهاء الزيارة بلا تنفيذ.
 *
 * - رسوم معاينة > 0 ← T-13: AWAITING_PAYMENT بالرسوم.
 * - رسوم = 0 (المرحلة الأولى) ← T-28: CLOSED مباشرة بمبالغ صفرية وحالة دفع WAIVED،
 *   بلا محاولة دفع وبلا عمولة، والتقييم متاح كأي طلب مغلق (BR-056، DEC-040).
 */
final readonly class CompleteInspectionOnlyAction
{
    public function __construct(private OrderStateMachine $stateMachine) {}

    public function execute(Order $order, ActorType $actorType, ?int $actorId = null): Order
    {
        if ($order->pricing_mode !== PricingMode::Inspection) {
            throw BusinessRuleViolationException::rule(
                'BR-042',
                'الإنهاء بالمعاينة فقط متاح لطلبات المعاينة.',
            );
        }

        $amounts = OrderAmounts::for($order);

        return $amounts->isZero()
            ? $this->closeFree($order, $actorType, $actorId)
            : $this->awaitPayment($order, $amounts, $actorType, $actorId);
    }

    private function closeFree(Order $order, ActorType $actorType, ?int $actorId): Order
    {
        return $this->stateMachine->apply(
            order: $order,
            action: 'closeFreeInspection',
            actorType: $actorType,
            actorId: $actorId,
            meta: ['final_amount' => '0.00', 'rule' => 'BR-056'],
            mutate: function (Order $fresh): void {
                $fresh->labor_total = '0.00';
                $fresh->materials_total = '0.00';
                $fresh->final_amount = '0.00';
                $fresh->commission_amount = '0.00';
                $fresh->payment_status = OrderPaymentStatus::Waived;
                $fresh->completed_at = now();
                $fresh->closed_at = now();
            },
        );
    }

    private function awaitPayment(Order $order, OrderAmounts $amounts, ActorType $actorType, ?int $actorId): Order
    {
        return $this->stateMachine->apply(
            order: $order,
            action: 'completeInspectionOnly',
            actorType: $actorType,
            actorId: $actorId,
            meta: $amounts->toColumns(),
            mutate: function (Order $fresh) use ($amounts): void {
                $fresh->fill($amounts->toColumns());
                $fresh->completed_at = now();
            },
        );
    }
}
