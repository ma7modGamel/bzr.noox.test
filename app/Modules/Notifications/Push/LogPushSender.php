<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Push;

use App\Modules\Notifications\Contracts\PushSender;
use Illuminate\Support\Facades\Log;

/**
 * `PUSH_DRIVER=log` للتطوير والاختبار فقط (DEC-058): يسجّل الرمز ورقم الإشعار بلا رمز جهاز أو نص.
 */
final class LogPushSender implements PushSender
{
    public function send(string $token, PushMessage $message): PushResult
    {
        Log::info('push.log', ['code' => $message->code, 'notification_id' => $message->notificationId]);

        return PushResult::Sent;
    }
}
