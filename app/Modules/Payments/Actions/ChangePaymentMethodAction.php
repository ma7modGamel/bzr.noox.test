<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Services\PaymentChannels;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Facades\DB;

/** BR-050 — تغيير الطريقة متاح حتى تسجيل الدفع فقط. */
final readonly class ChangePaymentMethodAction
{
    public function execute(Order $order, User $customer, PaymentMethod $method): Order
    {
        return DB::transaction(function () use ($order, $customer, $method): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());

            if ($fresh->customer_id !== $customer->getKey()
                || $fresh->status !== OrderStatus::AwaitingPayment) {
                throw BusinessRuleViolationException::rule('BR-050', 'لا يمكن تغيير طريقة الدفع الآن.');
            }

            if ($fresh->payment_method === $method) {
                return $fresh;
            }

            if (app(PaymentChannels::class)->pendingTransfer($fresh) !== null) {
                throw BusinessRuleViolationException::rule('BR-057', 'التحويل المرسل بانتظار تأكيد الإدارة؛ لا يمكن تغيير الطريقة الآن.');
            }

            if (! app(PaymentChannels::class)->methodEnabled($method)) {
                throw BusinessRuleViolationException::rule('BR-051', 'طريقة الدفع هذه غير متاحة حاليًا.');
            }

            $from = $fresh->payment_method;

            Payment::query()
                ->where('order_id', $fresh->getKey())
                ->where('status', PaymentStatus::Pending->value)
                ->update(['status' => PaymentStatus::Cancelled->value]);

            $fresh->payment_method = $method;
            $fresh->version++;
            $fresh->save();

            OrderEvent::query()->create([
                'order_id' => $fresh->getKey(),
                'event_code' => OrderEventCode::PaymentMethodChanged,
                'actor_type' => ActorType::Customer,
                'actor_id' => $customer->getKey(),
                'from_status' => $fresh->status,
                'to_status' => $fresh->status,
                'meta' => ['from' => $from?->value, 'to' => $method->value],
                'created_at' => now(),
            ]);

            return $fresh;
        });
    }
}
