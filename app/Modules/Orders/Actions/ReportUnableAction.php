<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;

/**
 * تعذّر التنفيذ يلغي؛ غياب العميل بلاغ للإدارة وحدها دون تغيير الحالة (BR-071).
 * الإلغاء هنا بلا أي مبلغ مهما كانت المرحلة (13).
 */
final readonly class ReportUnableAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderTermination $termination,
        private SettingsRepository $settings,
    ) {}

    public function unableToPerform(Order $order, ProviderProfile $provider, CancelReason $reason, string $note): Order
    {
        $this->assertOwnership($order, $provider);

        if ($reason === CancelReason::CustomerNoShow) {
            return $this->customerNoShow($order, $provider);
        }

        if (mb_strlen(trim($note)) < 5) {
            throw BusinessRuleViolationException::rule('BR-071', Localization::text('copy.a_reason_for_being_unable_to_perform_the_work'));
        }

        return $this->cancel($order, $provider, $reason, $note, $this->actionFor($order));
    }

    /** BR-071 — لا يُسجَّل غياب العميل إلا بعد CFG-033 من الوصول (AC-EXE-05). */
    public function customerNoShow(Order $order, ProviderProfile $provider): Order
    {
        return DB::transaction(function () use ($order, $provider): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            $this->assertOwnership($fresh, $provider);

            if ($fresh->status !== OrderStatus::Arrived || $fresh->arrived_at === null) {
                throw BusinessRuleViolationException::rule('BR-071', Localization::text('copy.customer_absence_can_only_be_reported_after_arrival_is'));
            }

            $wait = $this->settings->int(Cfg::CustomerNoShowWaitMinutes);

            if ($fresh->arrived_at->addMinutes($wait)->gt(CarbonImmutable::now())) {
                throw BusinessRuleViolationException::rule('BR-071', Localization::text('dynamic.absence_wait', ['minutes' => $wait]));
            }

            if (! $fresh->hasPendingNoShowReport()) {
                OrderEvent::query()->create([
                    'order_id' => $fresh->getKey(),
                    'event_code' => OrderEventCode::CustomerNoShowReported,
                    'actor_type' => ActorType::Provider,
                    'actor_id' => $provider->getKey(),
                    'from_status' => $fresh->status,
                    'to_status' => $fresh->status,
                    'created_at' => now(),
                ]);
            }

            $fresh->unsetRelation('latestNoShowEvent');

            return $fresh;
        });
    }

    private function cancel(Order $order, ProviderProfile $provider, CancelReason $reason, ?string $note, string $action): Order
    {
        return $this->stateMachine->apply(
            order: $order,
            action: $action,
            actorType: ActorType::Provider,
            actorId: $provider->getKey(),
            meta: ['reason_code' => $reason->value, 'note' => $note],
            mutate: function (Order $fresh) use ($provider, $reason, $note): void {
                $fresh->cancelled_at = now();
                $fresh->cancelled_by_type = ActorType::Provider->value;
                $fresh->cancelled_by_id = $provider->getKey();
                $fresh->cancel_reason_code = $reason;
                $fresh->cancel_note = $note;

                $this->termination->apply($fresh);
            },
        );
    }

    private function actionFor(Order $order): string
    {
        return $order->status === OrderStatus::InProgress
            ? 'reportUnableInProgress'
            : 'reportUnableAtArrival';
    }

    private function assertOwnership(Order $order, ProviderProfile $provider): void
    {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-071', Localization::text('copy.this_request_is_not_assigned_to_you'));
        }
    }
}
