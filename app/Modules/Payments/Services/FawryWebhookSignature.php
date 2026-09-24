<?php

declare(strict_types=1);

namespace App\Modules\Payments\Services;

/** يبني توقيع callback وفق ترتيب حقول FawryPay الموثق، ولا يسجل المفتاح أو الحمولة. */
final readonly class FawryWebhookSignature
{
    public function __construct(private string $securityKey) {}

    /** @param array<string, mixed> $payload */
    public function make(array $payload): string
    {
        $raw = implode('', [
            $payload['referenceNumber'] ?? '',
            $payload['merchantRefNumber'] ?? $payload['merchantRefNum'] ?? '',
            $this->amount($payload['paymentAmount'] ?? 0),
            $this->amount($payload['orderAmount'] ?? 0),
            $payload['orderStatus'] ?? '',
            $payload['paymentMethod'] ?? '',
            $this->optionalAmount($payload, 'fawryFees'),
            $this->optionalAmount($payload, 'shippingFees'),
            $payload['authNumber'] ?? '',
            $payload['customerMail'] ?? '',
            $payload['customerMobile'] ?? '',
            $this->securityKey,
        ]);

        return hash('sha256', $raw);
    }

    /** @param array<string, mixed> $payload */
    public function verify(array $payload): bool
    {
        $signature = $payload['signature'] ?? null;

        return is_string($signature) && hash_equals($this->make($payload), strtolower($signature));
    }

    private function amount(mixed $value): string
    {
        return number_format((float) $value, 2, '.', '');
    }

    /** @param array<string, mixed> $payload */
    private function optionalAmount(array $payload, string $key): string
    {
        return array_key_exists($key, $payload) && $payload[$key] !== null
            ? $this->amount($payload[$key])
            : '';
    }
}
