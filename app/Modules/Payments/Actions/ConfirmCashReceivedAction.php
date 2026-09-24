<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Services\PaymentChannels;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Str;

/**
 * T-19 — تأكيد استلام المبلغ نقدًا (BR-053).
 *
 * لا دفع جزئي: المبلغ يساوي `final_amount` كاملًا.
 * إن كانت الطريقة المختارة إلكترونية تتحول تلقائيًا إلى نقدي وتُلغى أي محاولة معلّقة (EVT-013).
 */
final readonly class ConfirmCashReceivedAction
{
    public function __construct(private OrderStateMachine $stateMachine) {}

    /** @param ActorType $actor الفني، أو الإدارة نيابة عنه (BR-055) */
    public function execute(Order $order, string $amount, ActorType $actor, ?int $actorId = null): Order
    {
        $expected = (string) ($order->final_amount ?? '0.00');

        if (app(PaymentChannels::class)->pendingTransfer($order) !== null) {
            throw BusinessRuleViolationException::rule('BR-057', 'يوجد تحويل إنستاباي بانتظار تأكيد الإدارة لهذا الطلب.');
        }

        if (bccomp($amount, $expected, 2) !== 0) {
            throw BusinessRuleViolationException::rule(
                'BR-053',
                "المبلغ المستلم يجب أن يساوي الإجمالي بالكامل ({$expected} جنيه). لا دفع جزئي.",
            );
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'confirmCashReceived',
            actorType: $actor,
            actorId: $actorId,
            meta: ['amount' => $amount],
            mutate: function (Order $fresh) use ($amount, $actor, $actorId): void {
                $this->switchToCashIfNeeded($fresh, $actor, $actorId);

                Payment::query()->create([
                    'order_id' => $fresh->getKey(),
                    'method' => PaymentMethod::Cash,
                    'amount' => $amount,
                    'status' => PaymentStatus::Succeeded,
                    'merchant_ref' => 'CASH-'.$fresh->getKey().'-'.Str::lower(Str::random(10)),
                    'paid_at' => now(),
                    'recorded_by_type' => $actor,
                    'recorded_by_id' => $actorId,
                ]);

                $fresh->payment_method = PaymentMethod::Cash;
                $fresh->payment_status = OrderPaymentStatus::Paid;
            },
        );
    }

    /** EVT-013 — تغيير طريقة الدفع يُسجَّل، وتُلغى محاولة فوري المعلّقة. */
    private function switchToCashIfNeeded(Order $order, ActorType $actor, ?int $actorId): void
    {
        if ($order->payment_method !== PaymentMethod::Electronic) {
            return;
        }

        Payment::query()
            ->where('order_id', $order->getKey())
            ->where('status', PaymentStatus::Pending->value)
            ->update(['status' => PaymentStatus::Cancelled->value]);

        OrderEvent::query()->create([
            'order_id' => $order->getKey(),
            'event_code' => OrderEventCode::PaymentMethodChanged,
            'actor_type' => $actor,
            'actor_id' => $actorId,
            'from_status' => $order->status,
            'to_status' => $order->status,
            'meta' => ['from' => PaymentMethod::Electronic->value, 'to' => PaymentMethod::Cash->value, 'rule' => 'BR-053'],
            'created_at' => now(),
        ]);
    }
}
