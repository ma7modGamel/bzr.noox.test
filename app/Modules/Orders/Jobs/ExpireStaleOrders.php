<?php

declare(strict_types=1);

namespace App\Modules\Orders\Jobs;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Support\Exceptions\DomainException;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;

/**
 * T-03 — انتهاء الطلب عند `selection_deadline_at`.
 * في وضع السوق: مهلة اختيار عرض. في وضع الموظفين: مهلة التعيين (CFG-093).
 * المهمة idempotent: تعيد التحقق من الحالة داخل المعاملة قبل التنفيذ (30).
 */
final class ExpireStaleOrders implements ShouldQueue
{
    use Queueable;

    public function handle(OrderStateMachine $stateMachine, OrderTermination $termination): int
    {
        $expired = 0;

        Order::query()
            ->where('status', OrderStatus::Open->value)
            ->whereNotNull('selection_deadline_at')
            ->where('selection_deadline_at', '<=', now())
            ->orderBy('id')
            ->chunkById(100, function ($orders) use ($stateMachine, $termination, &$expired): void {
                foreach ($orders as $order) {
                    try {
                        $stateMachine->apply(
                            order: $order,
                            action: 'expireOrder',
                            actorType: ActorType::System,
                            meta: ['deadline' => $order->selection_deadline_at?->toIso8601String()],
                            mutate: function (Order $fresh) use ($termination): void {
                                $fresh->expired_at = now();
                                $termination->apply($fresh);
                            },
                        );

                        $expired++;
                    } catch (DomainException) {
                        // سبقها إجراء آخر (تعيين أو قبول عرض أو إلغاء) — تُترك كما هي
                    }
                }
            });

        return $expired;
    }
}
