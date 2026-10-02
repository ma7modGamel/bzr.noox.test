<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Services;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\OperatingMode;
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
                'title' => 'طلب #'.$order->number,
                'detail' => $this->orderDetail($order, $amount),
                'amount' => $this->money($amount),
                'occurred_at' => $order->closed_at?->toIso8601String(),
                'available' => $available,
            ];
        });

        $payoutRows = $profile->payouts()->latest('paid_at')->get()->map(fn ($payout): array => [
            'id' => 'payout-'.$payout->getKey(),
            'type' => 'PAYOUT',
            'title' => 'تحويل من المنصة',
            'detail' => 'تم التحويل · '.$this->money((float) $payout->amount).' جنيه',
            'amount' => $this->money(-((float) $payout->amount)),
            'occurred_at' => $payout->paid_at?->toIso8601String(),
            'available' => true,
        ]);
        $remittanceRows = $profile->remittances()->latest('received_at')->get()->map(fn ($remittance): array => [
            'id' => 'remittance-'.$remittance->getKey(),
            'type' => 'REMITTANCE',
            'title' => 'توريد نقدية',
            'detail' => 'تم التوريد · '.$this->money((float) $remittance->amount).' جنيه',
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

    private function orderContribution(Order $order): float
    {
        $cash = $order->payment_method === PaymentMethod::Cash;
        if ($order->operating_mode === OperatingMode::Employee) {
            return $cash ? -((float) $order->labor_total) : (float) $order->materials_total;
        }

        if ($cash) {
            return -((float) $order->commission_amount);
        }

        return (float) $order->final_amount - (float) $order->refunded_total - (float) $order->commission_amount;
    }

    private function orderDetail(Order $order, float $amount): string
    {
        if ($order->operating_mode === OperatingMode::Employee) {
            return $amount < 0 ? 'مطلوب توريده · '.$this->money(abs($amount)).' جنيه' : 'رد خامات · '.$this->money($amount).' جنيه';
        }

        return 'صافي الطلب · '.$this->money($amount).' جنيه';
    }

    private function money(float $amount): string
    {
        return number_format($amount, 2, '.', '');
    }
}
