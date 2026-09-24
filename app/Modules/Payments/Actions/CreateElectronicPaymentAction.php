<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Payments\Contracts\PaymentGateway;
use App\Modules\Payments\Enums\PaymentChannel;
use App\Modules\Payments\Enums\PaymentGatewayChannel;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentSimulation;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Services\PaymentChannels;
use App\Modules\Settings\Enums\Cfg;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\FeatureDisabledException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Str;
use Throwable;

/** BR-051 — ينشئ محاولة إلكترونية ثم يمرر نتيجة المحاكي عبر معالج webhook نفسه. */
final readonly class CreateElectronicPaymentAction
{
    public function __construct(
        private PaymentGateway $gateway,
        private ProcessPaymentWebhookAction $processWebhook,
    ) {}

    public function execute(
        Order $order,
        User $customer,
        PaymentChannel $channel,
        PaymentSimulation $simulation = PaymentSimulation::Pending,
    ): Payment {
        if (! app(PaymentChannels::class)->isEnabled(PaymentGatewayChannel::Fawry)) {
            throw FeatureDisabledException::for(Cfg::ChannelFawryEnabled, 'payElectronic');
        }

        $payment = DB::transaction(function () use ($order, $customer, $channel): Payment {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());

            if ($fresh->customer_id !== $customer->getKey()
                || $fresh->status !== OrderStatus::AwaitingPayment
                || $fresh->payment_method !== PaymentMethod::Electronic) {
                throw BusinessRuleViolationException::rule('BR-051', 'الدفع الإلكتروني غير متاح لهذا الطلب.');
            }

            if (app(PaymentChannels::class)->pendingTransfer($fresh) !== null) {
                throw BusinessRuleViolationException::rule('BR-057', 'يوجد تحويل إنستاباي بانتظار تأكيد الإدارة لهذا الطلب.');
            }

            Payment::query()
                ->where('order_id', $fresh->getKey())
                ->where('status', PaymentStatus::Pending->value)
                ->update(['status' => PaymentStatus::Cancelled->value]);

            $payment = Payment::query()->create([
                'order_id' => $fresh->getKey(),
                'method' => PaymentMethod::Electronic,
                'amount' => $fresh->final_amount,
                'status' => PaymentStatus::Pending,
                'gateway' => 'FAWRY',
                'merchant_ref' => 'BZR-'.$fresh->getKey().'-'.Str::upper((string) Str::ulid()),
                'channel' => $channel->value,
            ]);

            OrderEvent::query()->create([
                'order_id' => $fresh->getKey(),
                'event_code' => OrderEventCode::PaymentAttemptCreated,
                'actor_type' => ActorType::Customer,
                'actor_id' => $customer->getKey(),
                'from_status' => $fresh->status,
                'to_status' => $fresh->status,
                'ref_type' => 'payment',
                'ref_id' => $payment->getKey(),
                'meta' => ['channel' => $channel->value],
                'created_at' => now(),
            ]);

            return $payment;
        });

        try {
            $gatewayPayment = $this->gateway->create($payment, $customer, $channel, $simulation);
            $payment->forceFill([
                'gateway_reference' => $gatewayPayment->gatewayReference,
                'fawry_reference_number' => $gatewayPayment->referenceNumber,
                'expires_at' => $gatewayPayment->expiresAt,
                'checkout_url' => $gatewayPayment->checkoutUrl,
            ])->save();

            if ($gatewayPayment->simulatedWebhook !== null) {
                $this->processWebhook->execute($gatewayPayment->simulatedWebhook);
            }
        } catch (Throwable $exception) {
            $payment->forceFill([
                'status' => PaymentStatus::Failed,
                'failure_reason' => 'GATEWAY_CREATE_FAILED',
            ])->save();

            throw $exception;
        }

        return $payment->refresh();
    }
}
