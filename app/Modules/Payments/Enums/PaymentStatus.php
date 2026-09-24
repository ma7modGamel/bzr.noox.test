<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** حالة محاولة الدفع — 19 §حالات محاولة الدفع. */
enum PaymentStatus: string implements HasColor, HasLabel
{
    case Pending = 'PENDING';
    case PendingVerification = 'PENDING_VERIFICATION'; // تحويل إنستاباي بانتظار تأكيد المدير العام — BR-057
    case Succeeded = 'SUCCEEDED';
    case Failed = 'FAILED';
    case Expired = 'EXPIRED';
    case Cancelled = 'CANCELLED';

    public function getLabel(): string
    {
        return match ($this) {
            self::Pending => 'قيد التنفيذ',
            self::PendingVerification => 'بانتظار تأكيد التحويل',
            self::Succeeded => 'ناجحة',
            self::Failed => 'فاشلة',
            self::Expired => 'منتهية',
            self::Cancelled => 'ملغاة',
        };
    }

    /** محاولة معلّقة تمنع إنشاء غيرها (قيد `pending_key`). */
    public function isOpen(): bool
    {
        return $this === self::Pending || $this === self::PendingVerification;
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Pending, self::PendingVerification => 'warning',
            self::Succeeded => 'success',
            self::Failed => 'danger',
            default => 'gray',
        };
    }
}
