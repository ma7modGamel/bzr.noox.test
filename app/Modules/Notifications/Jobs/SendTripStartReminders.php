<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Jobs;

use App\Modules\Notifications\Services\OrderNotifications;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Foundation\Queue\Queueable;

/**
 * NTF-07 — تذكير الفني ببدء التحرك (EC-07): NOW بعد CFG-031 من التأكيد، والمجدول عند بداية الفترة.
 * مرة واحدة لكل تعيين عبر `notification_dispatches`.
 */
final class SendTripStartReminders implements ShouldQueue
{
    use Queueable;

    public function handle(OrderNotifications $notifications, SettingsRepository $settings): void
    {
        $now = now();
        $nowCutoff = $now->copy()->subMinutes($settings->int(Cfg::TripStartReminderMinutes));

        Order::query()
            ->with('providerProfile.user')
            ->where('status', OrderStatus::Confirmed->value)
            ->whereNotNull('provider_profile_id')
            // نافذة يوم واحد: لا تذكيرات قديمة متراكمة بعد نشر أو توقف المجدول.
            ->where('confirmed_at', '>=', $now->copy()->subDay())
            ->where(fn (Builder $query) => $query
                ->where(fn (Builder $q) => $q->where('timing_type', TimingType::Now->value)->where('confirmed_at', '<=', $nowCutoff))
                ->orWhere(fn (Builder $q) => $q->where('timing_type', TimingType::Scheduled->value)->where('slot_start', '<=', $now)))
            ->orderBy('id')
            ->chunkById(100, fn ($orders) => $orders->each(fn (Order $order) => $notifications->tripStartReminder($order)));
    }
}
