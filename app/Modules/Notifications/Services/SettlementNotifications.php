<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Data\LocalizedMessage;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Enums\NotificationCode;

/** إشعارات المستحقات — NTF-23 وNTF-24 (17، DEC-066)، وتفتح `provider/earnings`. */
final class SettlementNotifications
{
    public function __construct(private readonly Notifier $notifier) {}

    /** NTF-23 — سجّل المدير العام تحويلًا للفني. */
    public function payoutSent(User $user, string $amount, string $reference): void
    {
        $this->notifier->send($user, new NotificationContent(
            NotificationCode::PayoutSent,
            new LocalizedMessage('notifications.payout_title'),
            new LocalizedMessage('notifications.payout_body', ['amount' => $amount, 'reference' => $reference]),
            'provider/earnings',
            NotificationContent::PROVIDER,
        ));
    }

    /** NTF-24 — تجاوز الدين حد CFG-061؛ وضع السوق فقط (BR-063، BR-065). */
    public function duesLimitExceeded(User $user, string $debt, string $limit): void
    {
        $this->notifier->send($user, new NotificationContent(
            NotificationCode::DuesLimitExceeded,
            new LocalizedMessage('notifications.dues_title'),
            new LocalizedMessage('notifications.dues_body', ['debt' => $debt, 'limit' => $limit]),
            'provider/earnings',
            NotificationContent::PROVIDER,
        ));
    }
}
