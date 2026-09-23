<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Services;

use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Models\Order;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;

/**
 * حساب المبالغ — BR-044. المصدر الوحيد لأي مبلغ على الطلب.
 * السعر المقبول لا يُعدَّل أبدًا (BR-040)؛ الإجمالي يُعاد حسابه من المقترحات الموافق عليها فقط.
 */
final readonly class OrderAmounts
{
    private function __construct(
        public string $laborBase,
        public string $laborTotal,
        public string $materialsTotal,
        public string $finalAmount,
    ) {}

    public static function for(Order $order): self
    {
        $approved = $order->proposals()
            ->where('status', ProposalStatus::Approved->value)
            ->get();

        $extraWork = self::sum($approved->where('type', ProposalType::ExtraWork)->pluck('amount')->all());
        $materials = self::sum($approved->where('type', ProposalType::Materials)->pluck('amount')->all());

        $laborBase = self::laborBase($order, $approved->firstWhere('type', ProposalType::ExecutionQuote)?->amount);
        $laborTotal = bcadd($laborBase, $extraWork, 2);

        return new self(
            laborBase: $laborBase,
            laborTotal: $laborTotal,
            materialsTotal: $materials,
            finalAmount: bcadd($laborTotal, $materials, 2),
        );
    }

    /**
     * - طلب تنفيذ: سعر العرض المقبول.
     * - طلب معاينة بعرض تنفيذ موافق عليه: العرض إن كانت الرسوم تُخصم، وإلا العرض + الرسوم.
     * - طلب معاينة بلا تنفيذ: الرسوم وحدها — وتساوي صفرًا عندما تكون المعاينة مجانية (BR-008).
     */
    private static function laborBase(Order $order, mixed $quote): string
    {
        $offer = $order->acceptedOffer;

        if ($order->pricing_mode === PricingMode::Execution) {
            return self::money($offer?->price);
        }

        $fee = self::money($offer?->price);

        if ($quote === null) {
            return $fee;
        }

        // في وضع الموظفين الرسوم صفر، فالنتيجة هي عرض التنفيذ وحده مهما كانت قيمة الخصم.
        return $offer?->inspection_fee_deductible
            ? self::money($quote)
            : bcadd(self::money($quote), $fee, 2);
    }

    /** BR-056 — طلب بمبلغ صفر يُغلق مباشرة بلا مرور بـ AWAITING_PAYMENT. */
    public function isZero(): bool
    {
        return bccomp($this->finalAmount, '0.00', 2) === 0;
    }

    /** BR-060 — العمولة على المصنعية فقط، بالنسبة المثبتة لحظة التأكيد. */
    public function commission(Order $order): string
    {
        $rate = (string) ($order->commission_rate ?? '0');
        $base = bcsub($this->laborTotal, self::money($order->labor_refunded), 2);

        if (bccomp($base, '0.00', 2) < 0) {
            $base = '0.00';
        }

        return bcmul($rate, $base, 2);
    }

    /** @return array<string, string> صالح للكتابة المباشرة على الطلب. */
    public function toColumns(): array
    {
        return [
            'labor_total' => $this->laborTotal,
            'materials_total' => $this->materialsTotal,
            'final_amount' => $this->finalAmount,
        ];
    }

    /** @param list<mixed> $amounts */
    private static function sum(array $amounts): string
    {
        return array_reduce($amounts, fn (string $c, $a) => bcadd($c, self::money($a), 2), '0.00');
    }

    private static function money(mixed $value): string
    {
        return number_format((float) ($value ?? 0), 2, '.', '');
    }
}
