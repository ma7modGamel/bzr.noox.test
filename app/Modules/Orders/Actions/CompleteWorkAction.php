<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * T-17 — تم الإنهاء. يحسب المبالغ ويثبّتها (BR-044) ثم ينتظر الدفع.
 * BR-045 — لا إنهاء مع وجود مقترح معلّق.
 */
final readonly class CompleteWorkAction
{
    public function __construct(private OrderStateMachine $stateMachine) {}

    public function execute(Order $order, ProviderProfile $provider): Order
    {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-022', 'هذا الطلب غير مسنَد إليك.');
        }

        if ($order->hasPendingProposal()) {
            throw BusinessRuleViolationException::rule(
                'BR-045',
                'لا يمكن إنهاء العمل مع وجود مقترح سعر معلّق.',
            );
        }

        $amounts = OrderAmounts::for($order);

        // طلب في التنفيذ لا يصل صفرًا: السعر إما عرض مقبول أو عرض تنفيذ موافق عليه
        if ($amounts->isZero()) {
            throw BusinessRuleViolationException::rule(
                'BR-044',
                'لا يمكن إنهاء طلب بلا مبلغ مستحق.',
            );
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'completeWork',
            actorType: ActorType::Provider,
            actorId: $provider->getKey(),
            meta: $amounts->toColumns(),
            mutate: function (Order $fresh) use ($amounts): void {
                $fresh->fill($amounts->toColumns());
                $fresh->completed_at = now();
            },
        );
    }
}
