<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Support\Facades\DB;

/**
 * BR-057 / T-20 — المدير العام رأى المبلغ في حساب المنصة فيؤكد الاستلام.
 * نفس أثر نجاح فوري: الدفعة SUCCEEDED والطلب CLOSED (BR-052).
 */
final readonly class ConfirmInstapayTransferAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderClosure $closure,
    ) {}

    public function execute(Payment $payment, Admin $admin): Order
    {
        if (! $admin->isSuper()) {
            throw new AuthorizationException('تأكيد التحويلات للمدير العام فقط (23).');
        }

        return DB::transaction(function () use ($payment, $admin): Order {
            $order = Order::query()->lockForUpdate()->findOrFail($payment->order_id);
            $fresh = Payment::query()->lockForUpdate()->findOrFail($payment->getKey());

            if ($fresh->status !== PaymentStatus::PendingVerification || $order->status !== OrderStatus::AwaitingPayment) {
                throw BusinessRuleViolationException::rule('BR-057', 'هذا التحويل لم يعد بانتظار التأكيد.');
            }

            $fresh->forceFill([
                'status' => PaymentStatus::Succeeded,
                'paid_at' => now(),
                'verified_at' => now(),
                'verified_by_admin_id' => $admin->getKey(),
                'recorded_by_type' => ActorType::Admin,
                'recorded_by_id' => $admin->getKey(),
            ])->save();

            return $this->stateMachine->apply(
                order: $order,
                action: 'settleElectronicPayment',
                actorType: ActorType::Admin,
                actorId: $admin->getKey(),
                meta: [
                    'amount' => $fresh->amount,
                    'gateway' => $fresh->gateway,
                    'transfer_reference' => $fresh->transfer_reference,
                ],
                mutate: function (Order $locked): void {
                    $locked->payment_method = PaymentMethod::Electronic;
                    $locked->payment_status = OrderPaymentStatus::Paid;
                    $this->closure->apply($locked);
                },
                refType: 'payment',
                refId: $fresh->getKey(),
            );
        });
    }
}
