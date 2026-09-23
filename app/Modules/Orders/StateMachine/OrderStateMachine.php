<?php

declare(strict_types=1);

namespace App\Modules\Orders\StateMachine;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\FeatureDisabledException;
use App\Support\Exceptions\InvalidTransitionException;
use Illuminate\Support\Facades\DB;

/**
 * المنفذ الوحيد لتغيير `orders.status` (30 §آلة حالات الطلب).
 *
 * كل انتقال: معاملة ← قفل الصف ← فحص الوضع ← فحص الانتقال ← تحديث ← حدث ← آثار بعد الالتزام.
 * لا يوجد في أي مكان آخر `$order->status = ...`؛ اختبار يحرس هذه القاعدة.
 */
final class OrderStateMachine
{
    public function __construct(private readonly FeatureGate $features) {}

    /**
     * @param array<string, mixed> $meta بيانات الحدث (24)
     * @param (callable(Order): void)|null $mutate تعديلات إضافية على الطلب داخل نفس المعاملة
     * @param (callable(Order, OrderEvent): void)|null $after آثار جانبية بعد الالتزام
     */
    public function apply(
        Order $order,
        string $action,
        ActorType $actorType,
        ?int $actorId = null,
        array $meta = [],
        ?callable $mutate = null,
        ?callable $after = null,
        ?string $refType = null,
        ?int $refId = null,
    ): Order {
        $transition = TransitionTable::find($action)
            ?? throw InvalidTransitionException::for($order->status, $action);

        // فحص الوضع أولًا: الميزة المعطّلة تُرفض قبل أي قفل أو كتابة (30، 39).
        $this->guardOperatingMode($transition, $action);

        [$order, $event] = DB::transaction(function () use ($order, $transition, $actorType, $actorId, $meta, $mutate, $refType, $refId, $action) {
            /** @var Order $fresh */
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());

            if (! $transition->allowsFrom($fresh->status)) {
                throw InvalidTransitionException::for($fresh->status, $action);
            }

            if (! $transition->allowsActor($actorType)) {
                throw InvalidTransitionException::for($fresh->status, $action);
            }

            $from = $fresh->status;

            if ($mutate !== null) {
                $mutate($fresh);
            }

            $fresh->status = $transition->to;
            $fresh->version = $fresh->version + 1;
            $fresh->save();

            $event = OrderEvent::query()->create([
                'order_id' => $fresh->getKey(),
                'event_code' => $transition->event,
                'actor_type' => $actorType,
                'actor_id' => $actorId,
                'from_status' => $from,
                'to_status' => $transition->to,
                'ref_type' => $refType,
                'ref_id' => $refId,
                'meta' => $meta + ['transition' => $transition->code],
                'created_at' => now(),
            ]);

            return [$fresh, $event];
        });

        if ($after !== null) {
            $after($order, $event);
        }

        return $order;
    }

    /** يخبر الواجهات بما هو متاح الآن بلا محاولة تنفيذ. */
    public function can(Order $order, string $action, ActorType $actorType): bool
    {
        $transition = TransitionTable::find($action);

        if ($transition === null) {
            return false;
        }

        if ($transition->requiresOffers !== null && $transition->requiresOffers !== $this->features->offersEnabled()) {
            return false;
        }

        return $transition->allowsFrom($order->status) && $transition->allowsActor($actorType);
    }

    /** @return list<string> أسماء الإجراءات المتاحة من الحالة الحالية لهذا المنفّذ. */
    public function availableActions(Order $order, ActorType $actorType): array
    {
        return array_values(array_map(
            fn (Transition $t) => $t->action,
            array_filter(
                TransitionTable::fromStatus($order->status),
                fn (Transition $t) => $this->can($order, $t->action, $actorType),
            ),
        ));
    }

    private function guardOperatingMode(Transition $transition, string $action): void
    {
        if ($transition->requiresOffers === null) {
            return;
        }

        if ($transition->requiresOffers !== $this->features->offersEnabled()) {
            throw FeatureDisabledException::for(Cfg::OffersEnabled, $action);
        }
    }

    /** الحالة التالية لو نُفّذ هذا الإجراء — للعرض في الواجهة. */
    public function targetOf(string $action): ?OrderStatus
    {
        return TransitionTable::find($action)?->to;
    }
}
