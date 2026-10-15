<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Services;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\OperatingMode;
use App\Support\Localization;
use Carbon\CarbonImmutable;
use Illuminate\Support\Collection;

final class ProviderEarningsService
{
    /** @return array{balance: float, available: float, pending: float, transactions: Collection<int, array<string, mixed>>} */
    public function summary(ProviderProfile $profile): array
    {
        $orders = Order::query()
            ->with('disputes')
            ->whereBelongsTo($profile, 'providerProfile')
            ->where('status', OrderStatus::Closed->value)
            ->orderByDesc('closed_at')
            ->orderByDesc('id')
            ->get();

        $orderRows = $orders->map(function (Order $order): array {
            $amount = $this->orderContribution($order);
            $available = $order->settlement_eligible_at?->isPast() === true && ! $order->hasOpenDispute();

            return [
                'id' => 'order-'.$order->getKey(),
                'type' => 'ORDER',
                'title' => Localization::text('earnings.order', ['number' => $order->number]),
                'detail' => $this->orderDetail($order, $amount),
                'amount' => $this->money($amount),
                'occurred_at' => $order->closed_at?->toIso8601String(),
                'available' => $available,
            ];
        });

        $payoutRows = $profile->payouts()->latest('paid_at')->get()->map(fn ($payout): array => [
            'id' => 'payout-'.$payout->getKey(),
            'type' => 'PAYOUT',
            'title' => Localization::text('copy.transfer_from_the_platform'),
            'detail' => Localization::text('earnings.payout', ['amount' => $this->money((float) $payout->amount)]),
            'amount' => $this->money(-((float) $payout->amount)),
            'occurred_at' => $payout->paid_at?->toIso8601String(),
            'available' => true,
        ]);
        $remittanceRows = $profile->remittances()->latest('received_at')->get()->map(fn ($remittance): array => [
            'id' => 'remittance-'.$remittance->getKey(),
            'type' => 'REMITTANCE',
            'title' => Localization::text('copy.cash_remittance'),
            'detail' => Localization::text('earnings.remittance', ['amount' => $this->money((float) $remittance->amount)]),
            'amount' => $this->money((float) $remittance->amount),
            'occurred_at' => $remittance->received_at?->toIso8601String(),
            'available' => true,
        ]);

        $transactions = $orderRows->concat($payoutRows)->concat($remittanceRows)
            ->sortByDesc('occurred_at')
            ->values();
        $balance = (float) $transactions->sum(fn (array $row): float => (float) $row['amount']);
        $available = (float) $transactions
            ->filter(fn (array $row): bool => $row['available'])
            ->sum(fn (array $row): float => (float) $row['amount']);

        return [
            'balance' => $balance,
            'available' => $available,
            'pending' => $balance - $available,
            'transactions' => $transactions,
        ];
    }

    /**
     * @param  Collection<int, array<string, mixed>>  $transactions
     * @param  array{type?: string, date_from?: string|null, date_to?: string|null, page?: int}  $filters
     * @return Collection<int, array<string, mixed>>
     */
    public function filter(Collection $transactions, array $filters): Collection
    {
        $from = empty($filters['date_from']) ? null : CarbonImmutable::parse($filters['date_from'], config('app.timezone'))->startOfDay();
        $to = empty($filters['date_to']) ? null : CarbonImmutable::parse($filters['date_to'], config('app.timezone'))->endOfDay();

        return $transactions->filter(function (array $row) use ($filters, $from, $to): bool {
            if (($filters['type'] ?? 'ALL') !== 'ALL' && $row['type'] !== $filters['type']) {
                return false;
            }
            $date = isset($row['occurred_at']) ? CarbonImmutable::parse($row['occurred_at']) : null;

            return ($from === null || ($date !== null && $date->greaterThanOrEqualTo($from)))
                && ($to === null || ($date !== null && $date->lessThanOrEqualTo($to)));
        })->values();
    }

    private function orderContribution(Order $order): float
    {
        $cash = $order->payment_method === PaymentMethod::Cash;
        if ($order->operating_mode === OperatingMode::Employee) {
            // DEC-066 — ما استُرد من الخامات للعميل لا يُرد للموظف.
            $materialsRefunded = (float) $order->refunded_total - (float) $order->labor_refunded;

            return $cash ? -((float) $order->labor_total) : (float) $order->materials_total - $materialsRefunded;
        }

        if ($cash) {
            return -((float) $order->commission_amount);
        }

        return (float) $order->final_amount - (float) $order->refunded_total - (float) $order->commission_amount;
    }

    private function orderDetail(Order $order, float $amount): string
    {
        if ($order->operating_mode === OperatingMode::Employee) {
            return $amount < 0 ? Localization::text('earnings.cash_due', ['amount' => $this->money(abs($amount))]) : Localization::text('earnings.materials', ['amount' => $this->money($amount)]);
        }

        // DEC-068 — النقدي في وضع السوق دين عمولة على الفني، لا «صافي» له.
        if ($order->payment_method === PaymentMethod::Cash) {
            return Localization::text('earnings.commission', ['amount' => $this->money(abs($amount))]);
        }

        return Localization::text('earnings.net', ['amount' => $this->money($amount)]);
    }

    private function money(float $amount): string
    {
        return number_format($amount, 2, '.', '');
    }
}
