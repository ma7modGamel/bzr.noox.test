<?php

declare(strict_types=1);

namespace App\Modules\Payments\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Payments\Contracts\PaymentGateway;
use App\Modules\Payments\Data\GatewayPayment;
use App\Modules\Payments\Enums\PaymentChannel;
use App\Modules\Payments\Enums\PaymentSimulation;
use App\Modules\Payments\Models\Payment;
use Carbon\CarbonImmutable;
use Illuminate\Support\Str;

/** محاكي staging يحاكي عقد فوري، بما فيه webhook موقّع، بلا اتصال خارجي. */
final readonly class StagingPaymentGateway implements PaymentGateway
{
    public function __construct(private FawryWebhookSignature $signature) {}

    public function create(
        Payment $payment,
        User $customer,
        PaymentChannel $channel,
        PaymentSimulation $simulation = PaymentSimulation::Pending,
    ): GatewayPayment {
        $gatewayReference = 'STG-'.Str::upper(Str::random(18));
        $referenceNumber = $channel === PaymentChannel::Kiosk
            ? (string) random_int(1000000000, 9999999999)
            : null;
        $expiresAt = CarbonImmutable::now()->addDay();
        $checkoutUrl = $channel === PaymentChannel::Kiosk
            ? null
            : rtrim((string) config('services.fawry.checkout_url'), '/').'/'.$gatewayReference;

        $webhook = $simulation === PaymentSimulation::Pending
            ? null
            : $this->webhook($payment, $customer, $channel, $gatewayReference, $referenceNumber, $simulation);

        return new GatewayPayment(
            gatewayReference: $gatewayReference,
            referenceNumber: $referenceNumber,
            checkoutUrl: $checkoutUrl,
            expiresAt: $expiresAt,
            simulatedWebhook: $webhook,
        );
    }

    public function hasValidWebhookSignature(array $payload): bool
    {
        return $this->signature->verify($payload);
    }

    /** @return array<string, mixed> */
    private function webhook(
        Payment $payment,
        User $customer,
        PaymentChannel $channel,
        string $gatewayReference,
        ?string $referenceNumber,
        PaymentSimulation $simulation,
    ): array {
        $payload = [
            'referenceNumber' => $referenceNumber ?? $gatewayReference,
            'merchantRefNumber' => $payment->merchant_ref,
            'paymentAmount' => $payment->amount,
            'orderAmount' => $payment->amount,
            'fawryFees' => '0.00',
            'paymentMethod' => $channel->value,
            'orderStatus' => match ($simulation) {
                PaymentSimulation::Success => 'PAID',
                PaymentSimulation::Failure => 'FAILED',
                PaymentSimulation::Expiry => 'EXPIRED',
                PaymentSimulation::Pending => 'UNPAID',
            },
            'paymentTime' => now()->getTimestampMs(),
            'customerMail' => $customer->email,
            'customerMobile' => $customer->phone,
        ];
        $payload['signature'] = $this->signature->make($payload);

        return $payload;
    }
}
