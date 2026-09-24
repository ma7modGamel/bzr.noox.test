<?php

declare(strict_types=1);

namespace App\Modules\Payments\Services;

use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentGatewayChannel;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;

/** القنوات المفعّلة من اللوحة (CFG-053..058) — مصدر واحد للـ API والإجراءات واللوحة. */
final readonly class PaymentChannels
{
    public function __construct(private SettingsRepository $settings) {}

    public function isEnabled(PaymentGatewayChannel $channel): bool
    {
        if ($channel === PaymentGatewayChannel::InstapayManual && $this->instapayAddress() === null) {
            return false; // لا تُعرض القناة قبل أن يُدخل المدير العام عنوان المنصة
        }

        return $this->settings->bool($channel->setting());
    }

    /** @return list<PaymentGatewayChannel> */
    public function enabled(): array
    {
        return array_values(array_filter(
            PaymentGatewayChannel::cases(),
            fn (PaymentGatewayChannel $channel): bool => $this->isEnabled($channel),
        ));
    }

    /** طريقة BR-050 متاحة إذا كانت إحدى قنواتها مفعّلة. */
    public function methodEnabled(PaymentMethod $method): bool
    {
        foreach ($this->enabled() as $channel) {
            if ($channel->method() === $method) {
                return true;
            }
        }

        return false;
    }

    /** @return array{address: string, display_name: ?string, link: ?string}|null */
    public function instapay(): ?array
    {
        if (! $this->isEnabled(PaymentGatewayChannel::InstapayManual)) {
            return null;
        }

        return [
            'address' => (string) $this->instapayAddress(),
            'display_name' => $this->blankToNull($this->settings->string(Cfg::InstapayDisplayName)),
            'link' => $this->blankToNull($this->settings->string(Cfg::InstapayLink)),
        ];
    }

    /** تحويل إنستاباي بانتظار تأكيد الإدارة لهذا الطلب، إن وُجد (BR-057). */
    public function pendingTransfer(Order $order): ?Payment
    {
        return Payment::query()
            ->where('order_id', $order->getKey())
            ->where('status', PaymentStatus::PendingVerification->value)
            ->first();
    }

    private function instapayAddress(): ?string
    {
        return $this->blankToNull($this->settings->string(Cfg::InstapayAddress));
    }

    private function blankToNull(?string $value): ?string
    {
        return $value === null || trim($value) === '' ? null : trim($value);
    }
}
