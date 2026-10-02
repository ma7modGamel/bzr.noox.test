<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Modules\Orders\Models\OrderEvent;
use Illuminate\Contracts\Events\ShouldHandleEventsAfterCommit;
use Throwable;

/**
 * كل حدث في 24 يمر هنا بعد حفظ معاملته فقط، فلا إشعار لحدث تراجع (17 §التوقيت).
 * فشل الإشعار لا يُفشل الإجراء الذي أنشأ الحدث؛ يُسجَّل ويستمر.
 */
final class OrderEventObserver implements ShouldHandleEventsAfterCommit
{
    public function __construct(private readonly OrderNotifications $notifications) {}

    public function created(OrderEvent $event): void
    {
        try {
            $this->notifications->handle($event);
        } catch (Throwable $exception) {
            report($exception);
        }
    }
}
