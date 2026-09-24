<?php

declare(strict_types=1);

namespace App\Modules\Payments\Notifications;

use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\TransferRejectionReason;
use Illuminate\Bus\Queueable;
use Illuminate\Notifications\Notification;

/**
 * NTF-30 — تعذّر تأكيد تحويل إنستاباي (17). القائمة الداخلية الآن؛ Push والبريد في دفعة الإشعارات (41 §6).
 */
final class InstapayTransferRejected extends Notification
{
    use Queueable;

    public function __construct(
        private readonly Order $order,
        private readonly TransferRejectionReason $reason,
    ) {}

    /** @return list<string> */
    public function via(object $notifiable): array
    {
        return ['database'];
    }

    /** @return array<string, mixed> */
    public function toArray(object $notifiable): array
    {
        return [
            'code' => 'NTF-30',
            'title' => 'تعذّر تأكيد تحويل إنستاباي',
            'body' => $this->reason->getLabel().' — يمكنك إعادة المحاولة أو الدفع نقدًا للفني. طلب #'.$this->order->number,
            'deep_link' => 'bzr://orders/'.$this->order->getKey(),
            'order_id' => $this->order->getKey(),
            'reason' => $this->reason->value,
        ];
    }
}
