<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Services;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\OperatingMode;
use Carbon\CarbonInterface;

/** ملخص العمولات لفترة — 16 §المال، DEC-066: الطلبات المغلقة حسب `closed_at`، مقسومة حسب الوضع. */
final class CommissionSummary
{
    /** @return array<string, array{orders: int, labor: string, materials: string, refunded: string, commission: string}> */
    public function between(CarbonInterface $from, CarbonInterface $to): array
    {
        $rows = Order::query()
            ->where('status', OrderStatus::Closed->value)
            ->whereBetween('closed_at', [$from, $to])
            ->selectRaw('operating_mode, count(*) as orders, coalesce(sum(labor_total),0) as labor, coalesce(sum(materials_total),0) as materials, coalesce(sum(refunded_total),0) as refunded, coalesce(sum(commission_amount),0) as commission')
            ->groupBy('operating_mode')
            ->get()
            ->toBase() // only() على مجموعة Eloquent يرشّح بالمفتاح الأساسي لا بمفتاح المصفوفة
            ->keyBy(fn ($row): string => $row->operating_mode instanceof OperatingMode ? $row->operating_mode->value : (string) $row->operating_mode);

        $result = [];
        foreach ([...OperatingMode::cases(), null] as $mode) {
            $key = $mode?->value ?? 'TOTAL';
            $source = $mode === null ? $rows : $rows->only([$mode->value]);
            $result[$key] = [
                'orders' => (int) $source->sum('orders'),
                'labor' => $this->money($source->sum('labor')),
                'materials' => $this->money($source->sum('materials')),
                'refunded' => $this->money($source->sum('refunded')),
                'commission' => $this->money($source->sum('commission')),
            ];
        }

        return $result;
    }

    private function money(mixed $value): string
    {
        return number_format((float) $value, 2, '.', '');
    }
}
