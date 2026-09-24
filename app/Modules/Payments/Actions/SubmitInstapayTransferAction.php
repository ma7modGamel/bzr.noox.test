<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Payments\Enums\PaymentGatewayChannel;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Services\PaymentChannels;
use App\Modules\Settings\Enums\Cfg;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\FeatureDisabledException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;

/**
 * BR-057 — العميل حوّل المبلغ من تطبيق البنك ويرسل الرقم المرجعي للتحويل (وإيصالًا اختياريًا).
 * الطلب يبقى AWAITING_PAYMENT؛ الدفعة PENDING_VERIFICATION حتى يقرر المدير العام.
 */
final readonly class SubmitInstapayTransferAction
{
    public function __construct(private PaymentChannels $channels) {}

    public function execute(Order $order, User $customer, string $transferReference, ?int $receiptMediaId = null): Payment
    {
        if (! $this->channels->isEnabled(PaymentGatewayChannel::InstapayManual)) {
            throw FeatureDisabledException::for(Cfg::ChannelInstapayEnabled, 'submitInstapayTransfer');
        }

        return DB::transaction(function () use ($order, $customer, $transferReference, $receiptMediaId): Payment {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());

            if ($fresh->customer_id !== $customer->getKey() || $fresh->status !== OrderStatus::AwaitingPayment) {
                throw BusinessRuleViolationException::rule('BR-057', 'تحويل إنستاباي غير متاح لهذا الطلب الآن.');
            }

            if ($this->channels->pendingTransfer($fresh) !== null) {
                throw BusinessRuleViolationException::rule('BR-057', 'تم إرسال تحويل لهذا الطلب وهو بانتظار تأكيد الإدارة.');
            }

            if ($receiptMediaId !== null) {
                $media = OrderMedia::query()->find($receiptMediaId);

                if ($media === null || $media->uploaded_by !== $customer->getKey()) {
                    throw BusinessRuleViolationException::rule('BR-057', 'صورة الإيصال غير صالحة.');
                }

                $media->forceFill(['order_id' => $fresh->getKey(), 'expires_at' => null])->save();
            }

            // تحويل إنستاباي دفع إلكتروني (BR-050)؛ أي محاولة فوري معلّقة تُلغى.
            Payment::query()
                ->where('order_id', $fresh->getKey())
                ->where('status', PaymentStatus::Pending->value)
                ->update(['status' => PaymentStatus::Cancelled->value]);

            if ($fresh->payment_method !== PaymentMethod::Electronic) {
                $fresh->payment_method = PaymentMethod::Electronic;
            }
            $fresh->version++;
            $fresh->save();

            $payment = Payment::query()->create([
                'order_id' => $fresh->getKey(),
                'method' => PaymentMethod::Electronic,
                'amount' => $fresh->final_amount,
                'status' => PaymentStatus::PendingVerification,
                'gateway' => PaymentGatewayChannel::InstapayManual->value,
                'merchant_ref' => 'BZR-'.$fresh->getKey().'-'.Str::upper((string) Str::ulid()),
                'channel' => 'INSTAPAY',
                'transfer_reference' => $transferReference,
                'receipt_media_id' => $receiptMediaId,
                'submitted_at' => now(),
            ]);

            OrderEvent::query()->create([
                'order_id' => $fresh->getKey(),
                'event_code' => OrderEventCode::InstapayTransferSubmitted,
                'actor_type' => ActorType::Customer,
                'actor_id' => $customer->getKey(),
                'from_status' => $fresh->status,
                'to_status' => $fresh->status,
                'ref_type' => 'payment',
                'ref_id' => $payment->getKey(),
                'meta' => ['transfer_reference' => $transferReference, 'has_receipt' => $receiptMediaId !== null],
                'created_at' => now(),
            ]);

            return $payment;
        });
    }
}
