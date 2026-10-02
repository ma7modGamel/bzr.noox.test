<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Channels;

use App\Modules\Notifications\Notifications\AppNotification;
use Illuminate\Notifications\Channels\DatabaseChannel;
use Illuminate\Notifications\Notification;

/**
 * القناة الداخلية مع عمود `app_mode` لتصفية C32 حسب الوضع (DEC-058).
 */
final class AppDatabaseChannel extends DatabaseChannel
{
    /** @return array<string, mixed> */
    protected function buildPayload($notifiable, Notification $notification): array
    {
        $payload = parent::buildPayload($notifiable, $notification);

        if ($notification instanceof AppNotification) {
            $payload['app_mode'] = $notification->content->appMode;
        }

        return $payload;
    }
}
