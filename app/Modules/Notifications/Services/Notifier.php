<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Channels\AppDatabaseChannel;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Jobs\SendPushNotification;
use App\Modules\Notifications\Models\DeviceToken;
use App\Modules\Notifications\Models\NotificationDispatch;
use App\Modules\Notifications\Notifications\AppNotification;
use Illuminate\Database\UniqueConstraintViolationException;
use Illuminate\Support\Str;

/**
 * نقطة الإرسال الوحيدة — 17 §الإرسال وإعادة المحاولة:
 * الداخلي يُحفظ أولًا، ثم Push لكل جهاز في الطابور، ثم البريد في الطابور؛ كل قناة مستقلة عن الأخرى.
 */
final class Notifier
{
    /** @return string|null رقم الإشعار الداخلي، أو null لإشعار بريد فقط أو لحساب محذوف. */
    public function send(User $user, NotificationContent $content): ?string
    {
        if ($user->trashed()) {
            return null;
        }

        $id = null;

        if ($content->code->sendsPush()) {
            $notification = new AppNotification($content);
            $notification->id = $id = (string) Str::uuid();
            $user->notifyNow($notification, [AppDatabaseChannel::class]);

            DeviceToken::query()
                ->where('user_id', $user->getKey())
                ->pluck('id')
                ->each(fn (int $deviceId) => SendPushNotification::dispatch($id, $deviceId)->afterCommit());
        }

        if ($content->code->sendsMail() && filled($user->email)) {
            $user->notify(new AppNotification($content, ['mail']));
        }

        return $id;
    }

    /** إرسال مرة واحدة لكل مفتاح (NTF-07، NTF-18، نوافذ NTF-04). يعيد null إن سبق الإرسال. */
    public function sendOnce(User $user, NotificationContent $content, string $key): ?string
    {
        try {
            $dispatch = NotificationDispatch::query()->create([
                'user_id' => $user->getKey(),
                'code' => $content->code->value,
                'order_id' => $content->orderId,
                'key' => $key,
            ]);
        } catch (UniqueConstraintViolationException) {
            return null;
        }

        $id = $this->send($user, $content);
        $dispatch->update(['notification_id' => $id]);

        return $id;
    }
}
