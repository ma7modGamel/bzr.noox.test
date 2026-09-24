<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;

/**
 * T-16 / T-18 — "تعذّر التنفيذ" أو "العميل غير موجود" (BR-071).
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

        if (mb_strlen(trim($note)) < 5) {
            throw BusinessRuleViolationException::rule('BR-071', 'سبب تعذّر التنفيذ إلزامي.');
        }

        return $this->cancel($order, $provider, $reason, $note, $this->actionFor($order));
    }

    /** BR-071 — لا يُسجَّل غياب العميل إلا بعد CFG-033 من الوصول (AC-EXE-05). */
    public function customerNoShow(Order $order, ProviderProfile $provider): Order
    {
        $this->assertOwnership($order, $provider);

        if ($order->status !== OrderStatus::Arrived || $order->arrived_at === null) {
            throw BusinessRuleViolationException::rule('BR-071', 'يُسجَّل غياب العميل بعد تسجيل الوصول.');
        }

        $wait = $this->settings->int(Cfg::CustomerNoShowWaitMinutes);

        if ($order->arrived_at->addMinutes($wait)->gt(CarbonImmutable::now())) {
            throw BusinessRuleViolationException::rule(
                'BR-071',
                "يمكن تسجيل غياب العميل بعد {$wait} دقيقة من الوصول.",
            );
        }

        return $this->cancel($order, $provider, CancelReason::CustomerNoShow, null, 'reportUnableAtArrival');
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
            throw BusinessRuleViolationException::rule('BR-071', 'هذا الطلب غير مسنَد إليك.');
        }
    }
}
