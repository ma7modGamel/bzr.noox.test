<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Enums\TransferRejectionReason;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Notifications\InstapayTransferRejected;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Support\Facades\DB;

/**
 * BR-057 — لم يصل التحويل أو وصل بمبلغ مختلف: الدفعة FAILED، والطلب يبقى AWAITING_PAYMENT،
 * والعميل يصله NTF-30 ليعيد المحاولة أو يحوّل لنقدي.
 */
final readonly class RejectInstapayTransferAction
{
    public function execute(Payment $payment, Admin $admin, TransferRejectionReason $reason, ?string $note = null): Payment
    {
        if (! $admin->isSuper()) {
            throw new AuthorizationException('رفض التحويلات للمدير العام فقط (23).');
        }

        $rejected = DB::transaction(function () use ($payment, $admin, $reason, $note): Payment {
            $order = Order::query()->lockForUpdate()->findOrFail($payment->order_id);
            $fresh = Payment::query()->lockForUpdate()->findOrFail($payment->getKey());

            if ($fresh->status !== PaymentStatus::PendingVerification) {
                throw BusinessRuleViolationException::rule('BR-057', 'هذا التحويل لم يعد بانتظار التأكيد.');
            }

            $fresh->forceFill([
                'status' => PaymentStatus::Failed,
                'failure_reason' => $reason->value,
                'rejection_note' => $note,
                'verified_at' => now(),
                'verified_by_admin_id' => $admin->getKey(),
            ])->save();

            $order->version++;
            $order->save();

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::PaymentAttemptFailed,
                'actor_type' => ActorType::Admin,
                'actor_id' => $admin->getKey(),
                'from_status' => $order->status,
                'to_status' => $order->status,
                'ref_type' => 'payment',
                'ref_id' => $fresh->getKey(),
                'meta' => ['reason' => $reason->value, 'gateway' => $fresh->gateway, 'note' => $note],
                'created_at' => now(),
            ]);

            return $fresh;
        });

        $rejected->order->customer->notify(new InstapayTransferRejected($rejected->order, $reason));

        return $rejected;
    }
}
