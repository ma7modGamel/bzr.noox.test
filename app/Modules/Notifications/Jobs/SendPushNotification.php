<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Jobs;

use App\Modules\Notifications\Contracts\PushSender;
use App\Modules\Notifications\Enums\NotificationCode;
use App\Modules\Notifications\Models\DeviceToken;
use App\Modules\Notifications\Push\PushMessage;
use App\Modules\Notifications\Push\PushResult;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Notifications\DatabaseNotification;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;
use RuntimeException;

/**
 * Push لإشعار داخلي محفوظ إلى جهاز واحد. EC-22: المحاولة الأولى ثم 3 إعادات بفواصل 10 و60 و300 ثانية.
 * الإشعار الداخلي لا يتأثر بأي نتيجة هنا.
 */
final class SendPushNotification implements ShouldQueue
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;
    use SerializesModels;

    /** @var list<int> */
    public const RETRY_DELAYS = [10, 60, 300];

    public int $tries = 4;

    public function __construct(
        public readonly string $notificationId,
        public readonly int $deviceTokenId,
    ) {}

    public function handle(PushSender $sender): void
    {
        $notification = DatabaseNotification::query()->find($this->notificationId);
        $device = DeviceToken::query()->find($this->deviceTokenId);

        // الرمز حُذف (خروج أو رمز غير صالح) أو الإشعار حُذف مع الحساب: لا شيء يُرسل.
        if ($notification === null || $device === null || $device->user_id !== (int) $notification->notifiable_id) {
            return;
        }

        $result = $sender->send($device->token, $this->message($notification));

        match ($result) {
            PushResult::Sent => null,
            PushResult::InvalidToken => $this->forget($device),
            PushResult::Retry => $this->retryOrFail(),
            PushResult::Fatal => $this->fail(new RuntimeException('FCM rejected the push permanently.')),
        };
    }

    private function message(DatabaseNotification $notification): PushMessage
    {
        /** @var array<string, mixed> $data */
        $data = $notification->data;
        $code = NotificationCode::tryFrom((string) ($data['code'] ?? ''));
        $orderId = $data['order_id'] ?? null;

        return new PushMessage(
            notificationId: (string) $notification->getKey(),
            code: (string) ($data['code'] ?? ''),
            title: (string) ($data['title'] ?? ''),
            body: (string) ($data['body'] ?? ''),
            deepLink: isset($data['deep_link']) ? (string) $data['deep_link'] : null,
            appMode: (string) ($data['app_mode'] ?? 'CUSTOMER'),
            channel: $code?->androidChannel() ?? 'orders',
            group: $orderId === null ? null : 'order-'.$orderId,
        );
    }

    private function forget(DeviceToken $device): void
    {
        Log::info('push.token_removed', ['device_token_id' => $device->getKey(), 'token' => $device->redacted()]);
        $device->delete();
    }

    private function retryOrFail(): void
    {
        $attempt = $this->attempts();

        if ($attempt >= $this->tries) {
            $this->fail(new RuntimeException('FCM push failed after '.$attempt.' attempts (EC-22).'));

            return;
        }

        $this->release(self::RETRY_DELAYS[$attempt - 1] ?? self::RETRY_DELAYS[2]);
    }
}
