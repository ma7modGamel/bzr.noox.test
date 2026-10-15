<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;

/**
 * T-21 — تأكيد العميل للإنهاء، أو الإغلاق التلقائي بعد CFG-051 (BR-080).
 * هنا تُحسب العمولة ويُحدَّد موعد قابلية التسوية (BR-060، BR-061).
 */
final readonly class ConfirmCompletionAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderClosure $closure,
        private SettingsRepository $settings,
    ) {}

    public function execute(Order $order, ActorType $actor, ?int $actorId = null): Order
    {
        if ($actor === ActorType::Customer && $order->customer_id !== $actorId) {
            throw BusinessRuleViolationException::rule('BR-080', Localization::text('copy.this_request_does_not_belong_to_you'));
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'confirmCompletion',
            actorType: $actor,
            actorId: $actorId,
            meta: ['auto' => $actor === ActorType::System],
            mutate: function (Order $fresh) use ($actor): void {
                $paidAt = $fresh->latestSuccessfulPayment?->paid_at;
                if ($actor === ActorType::System && ($paidAt === null || now()->isBefore($paidAt->addHours($this->settings->int(Cfg::AutoCloseHours))))) {
                    throw BusinessRuleViolationException::rule('BR-080', Localization::text('copy.cash_confirmation_window_is_still_open'));
                }
                $this->closure->apply($fresh);
            },
        );
    }
}
