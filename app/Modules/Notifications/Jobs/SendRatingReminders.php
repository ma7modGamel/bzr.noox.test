<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Jobs;

use App\Modules\Notifications\Services\OrderNotifications;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;

/**
 * NTF-18 — تذكير التقييم مرة واحدة بعد CFG-072 من الإغلاق، ما دامت نافذة CFG-070 مفتوحة
 * والطرف لم يقيّم ولم يوقف التذكير من C33 (DEC-058).
 */
final class SendRatingReminders implements ShouldQueue
{
    use Queueable;

    public function handle(OrderNotifications $notifications, SettingsRepository $settings): void
    {
        $now = now();

        Order::query()
            ->with(['customer', 'providerProfile.user', 'review', 'customerRating'])
            ->where('status', OrderStatus::Closed->value)
            ->whereNotNull('provider_profile_id')
            ->where('closed_at', '<=', $now->copy()->subHours($settings->int(Cfg::RatingReminderHours)))
            ->where('closed_at', '>', $now->copy()->subDays($settings->int(Cfg::RatingWindowDays)))
            ->where(fn ($query) => $query->whereDoesntHave('review')->orWhereDoesntHave('customerRating'))
            ->orderBy('id')
            ->chunkById(100, function ($orders) use ($notifications): void {
                foreach ($orders as $order) {
                    if ($order->review === null && $order->customer !== null) {
                        $notifications->ratingReminder($order, $order->customer, asProvider: false);
                    }

                    if ($order->customerRating === null && $order->providerProfile?->user !== null) {
                        $notifications->ratingReminder($order, $order->providerProfile->user, asProvider: true);
                    }
                }
            });
    }
}
