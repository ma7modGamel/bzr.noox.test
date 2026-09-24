<?php

declare(strict_types=1);

namespace App\Modules\Payments\Contracts;

use App\Modules\Identity\Models\User;
use App\Modules\Payments\Data\GatewayPayment;
use App\Modules\Payments\Enums\PaymentChannel;
use App\Modules\Payments\Enums\PaymentSimulation;
use App\Modules\Payments\Models\Payment;

/** حد التكامل الوحيد الذي يمكن استبدال محاكيه بتنفيذ فوري الحقيقي دون تغيير الشاشات. */
interface PaymentGateway
{
    public function create(
        Payment $payment,
        User $customer,
        PaymentChannel $channel,
        PaymentSimulation $simulation = PaymentSimulation::Pending,
    ): GatewayPayment;

    /** @param array<string, mixed> $payload */
    public function hasValidWebhookSignature(array $payload): bool;
}
