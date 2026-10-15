<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Data\LocalizedMessage;
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
            $approved ? new LocalizedMessage('notifications.application_approved_title') : new LocalizedMessage('notifications.application_rejected_title'),
            $approved
                ? new LocalizedMessage('notifications.application_approved_body')
                : new LocalizedMessage('notifications.application_rejected_body', ['reason' => $reason ?? '']),
            'provider/application',
            NotificationContent::PROVIDER,
        ));
    }

    /** NTF-26 — إيقاف/حظر/إعادة تفعيل؛ بريد فقط (17). */
    public function accountStatusChanged(User $user, string|LocalizedMessage $title, string|LocalizedMessage $body): void
    {
        $this->notifier->send($user, new NotificationContent(
            NotificationCode::AccountStatusChanged, $title, $body, null, NotificationContent::CUSTOMER,
        ));
    }
}
