<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Contracts\PaymentGateway;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use Illuminate\Support\Facades\DB;

/** BR-052 / T-20 — تحقق، تطابق مبلغ، وتطبيق idempotent لإشعار فوري. */
final readonly class ProcessPaymentWebhookAction
{
    public function __construct(
        private PaymentGateway $gateway,
        private OrderStateMachine $stateMachine,
        private OrderClosure $closure,
    ) {}

    /** @param array<string, mixed> $payload */
    public function execute(array $payload): bool
    {
        if (! $this->gateway->hasValidWebhookSignature($payload)) {
            return false;
        }

        $merchantRef = $payload['merchantRefNumber'] ?? $payload['merchantRefNum'] ?? null;

        if (! is_string($merchantRef) || $merchantRef === '') {
            return false;
        }

        $payment = Payment::query()->where('merchant_ref', $merchantRef)->first();

        if ($payment === null) {
            return false;
        }

        return DB::transaction(function () use ($payment, $payload): bool {
            $order = Order::query()->lockForUpdate()->findOrFail($payment->order_id);
            $fresh = Payment::query()->lockForUpdate()->findOrFail($payment->getKey());

            if ($fresh->status !== PaymentStatus::Pending) {
                return true;
            }

            $status = (string) ($payload['orderStatus'] ?? '');
            $amount = number_format((float) ($payload['paymentAmount'] ?? 0), 2, '.', '');

            if ($status === 'PAID' && bccomp($amount, (string) $fresh->amount, 2) !== 0) {
                $this->fail($fresh, $order, 'AMOUNT_MISMATCH');

                return true;
            }

            if ($status === 'FAILED' || $status === 'EXPIRED') {
                $fresh->forceFill([
                    'status' => $status === 'FAILED' ? PaymentStatus::Failed : PaymentStatus::Expired,
                    'failure_reason' => $status,
                ])->save();
                $this->recordFailureEvent($fresh, $order, $status);

                return true;
            }

            if ($status !== 'PAID') {
                return true;
            }

            $fresh->forceFill([
                'status' => PaymentStatus::Succeeded,
                'gateway_reference' => (string) ($payload['referenceNumber'] ?? $fresh->gateway_reference),
                'fawry_reference_number' => (string) ($payload['referenceNumber'] ?? $fresh->fawry_reference_number),
                'paid_at' => now(),
            ])->save();

            if ($order->status !== OrderStatus::AwaitingPayment) {
                $fresh->forceFill(['failure_reason' => 'REQUIRES_REFUND_EC_15'])->save();
                $this->recordFailureEvent($fresh, $order, 'REQUIRES_REFUND_EC_15');

                return true;
            }

            $this->stateMachine->apply(
                order: $order,
                action: 'settleElectronicPayment',
                actorType: ActorType::System,
                meta: ['amount' => $fresh->amount, 'merchant_ref' => $fresh->merchant_ref],
                mutate: function (Order $locked): void {
                    $locked->payment_method = PaymentMethod::Electronic;
                    $locked->payment_status = OrderPaymentStatus::Paid;
                    $this->closure->apply($locked);
                },
                refType: 'payment',
                refId: $fresh->getKey(),
            );

            return true;
        });
    }

    private function fail(Payment $payment, Order $order, string $reason): void
    {
        $payment->forceFill([
            'status' => PaymentStatus::Failed,
            'failure_reason' => $reason,
        ])->save();
        $this->recordFailureEvent($payment, $order, $reason);
    }

    private function recordFailureEvent(Payment $payment, Order $order, string $reason): void
    {
        OrderEvent::query()->create([
            'order_id' => $order->getKey(),
            'event_code' => OrderEventCode::PaymentAttemptFailed,
            'actor_type' => ActorType::System,
            'from_status' => $order->status,
            'to_status' => $order->status,
            'ref_type' => 'payment',
            'ref_id' => $payment->getKey(),
            'meta' => ['reason' => $reason],
            'created_at' => now(),
        ]);
    }
}
