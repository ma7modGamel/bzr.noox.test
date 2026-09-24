<?php

declare(strict_types=1);

namespace App\Modules\Payments\Data;

use Carbon\CarbonImmutable;

final readonly class GatewayPayment
{
    /** @param array<string, mixed>|null $simulatedWebhook */
    public function __construct(
        public string $gatewayReference,
        public ?string $referenceNumber,
        public ?string $checkoutUrl,
        public CarbonImmutable $expiresAt,
        public ?array $simulatedWebhook = null,
    ) {}
}
