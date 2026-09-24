<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * T-21 — تأكيد العميل للإنهاء، أو الإغلاق التلقائي بعد CFG-051 (BR-080).
 * هنا تُحسب العمولة ويُحدَّد موعد قابلية التسوية (BR-060، BR-061).
 */
final readonly class ConfirmCompletionAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderClosure $closure,
    ) {}

    public function execute(Order $order, ActorType $actor, ?int $actorId = null): Order
    {
        if ($actor === ActorType::Customer && $order->customer_id !== $actorId) {
            throw BusinessRuleViolationException::rule('BR-080', 'هذا الطلب ليس طلبك.');
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'confirmCompletion',
            actorType: $actor,
            actorId: $actorId,
            meta: ['auto' => $actor === ActorType::System],
            mutate: fn (Order $fresh) => $this->closure->apply($fresh),
        );
    }
}
