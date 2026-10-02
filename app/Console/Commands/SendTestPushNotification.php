<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Enums\NotificationCode;
use App\Modules\Notifications\Models\DeviceToken;
use App\Modules\Notifications\Services\Notifier;
use Illuminate\Console\Command;

/** DEP-PUSH-04 — إشعار اختبار حقيقي لحساب على staging، يفتح C32. */
final class SendTestPushNotification extends Command
{
    protected $signature = 'notifications:push-test {user : رقم المستخدم أو بريده} {--mode=CUSTOMER : CUSTOMER أو PROVIDER}';

    protected $description = 'إرسال إشعار اختبار داخلي + Push لكل أجهزة مستخدم (DEP-PUSH-04)';

    public function handle(Notifier $notifier): int
    {
        $key = (string) $this->argument('user');
        $user = User::query()->where(is_numeric($key) ? 'id' : 'email', $key)->first();

        if ($user === null) {
            $this->error('المستخدم غير موجود.');

            return self::FAILURE;
        }

        $mode = strtoupper((string) $this->option('mode')) === 'PROVIDER' ? NotificationContent::PROVIDER : NotificationContent::CUSTOMER;
        $id = $notifier->send($user, new NotificationContent(
            NotificationCode::OrderClosed,
            'إشعار اختبار',
            'هذا إشعار اختبار من '.config('app.name').'.',
            $mode === NotificationContent::PROVIDER ? 'provider/notifications' : 'notifications',
            $mode,
        ));

        $devices = DeviceToken::query()->where('user_id', $user->getKey())->count();
        $this->info("الإشعار {$id} محفوظ، وأُرسل للطابور لـ{$devices} جهاز عبر ".config('services.fcm.driver').'.');

        return self::SUCCESS;
    }
}
