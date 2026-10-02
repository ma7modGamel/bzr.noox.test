<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Enums\NotificationCode;

/** إشعارات الحساب من قرارات الإدارة — NTF-22 وNTF-26 (17، DEC-060). */
final class AccountNotifications
{
    public function __construct(private readonly Notifier $notifier) {}

    /** NTF-22 — نتيجة مراجعة طلب الانضمام؛ Push وداخلي وبريد. */
    public function applicationReviewed(User $user, bool $approved, ?string $reason = null): void
    {
        $this->notifier->send($user, new NotificationContent(
            NotificationCode::ApplicationReviewed,
            $approved ? 'تم قبول طلب انضمامك' : 'تعذّر قبول طلب انضمامك',
            $approved
                ? 'يمكنك الآن تفعيل «متاح الآن» واستقبال الطلبات.'
                : 'السبب: '.$reason.' يمكنك تعديل بياناتك وإعادة التقديم من التطبيق.',
            'provider/application',
            NotificationContent::PROVIDER,
        ));
    }

    /** NTF-26 — إيقاف/حظر/إعادة تفعيل؛ بريد فقط (17). */
    public function accountStatusChanged(User $user, string $title, string $body): void
    {
        $this->notifier->send($user, new NotificationContent(
            NotificationCode::AccountStatusChanged, $title, $body, null, NotificationContent::CUSTOMER,
        ));
    }
}
