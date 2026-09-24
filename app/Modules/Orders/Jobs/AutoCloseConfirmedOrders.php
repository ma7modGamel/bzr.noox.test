<?php

declare(strict_types=1);

namespace App\Modules\Orders\Jobs;

use App\Modules\Orders\Actions\ConfirmCompletionAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\DomainException;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;

/** BR-080 — `AWAITING_CONFIRMATION` يُغلق تلقائيًا بعد CFG-051 (T-21، EVT-071). */
final class AutoCloseConfirmedOrders implements ShouldQueue
{
    use Queueable;

    public function handle(ConfirmCompletionAction $confirm, SettingsRepository $settings): int
    {
        $cutoff = now()->subHours($settings->int(Cfg::AutoCloseHours));
        $closed = 0;

        Order::query()
            ->where('status', OrderStatus::AwaitingConfirmation->value)
            ->where('completed_at', '<=', $cutoff)
            ->orderBy('id')
            ->chunkById(100, function ($orders) use ($confirm, &$closed): void {
                foreach ($orders as $order) {
                    try {
                        $confirm->execute($order, ActorType::System);
                        $closed++;
                    } catch (DomainException) {
                        // العميل أكّد أو فتح مشكلة قبل المهمة
                    }
                }
            });

        return $closed;
    }
}
