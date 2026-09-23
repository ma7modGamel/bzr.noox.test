<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** `orders.payment_status` — 19. WAIVED يشمل الإغلاق بمعاينة مجانية بمبلغ صفر (BR-056). */
enum OrderPaymentStatus: string implements HasColor, HasLabel
{
    case Unpaid = 'UNPAID';
    case Paid = 'PAID';
    case PartiallyRefunded = 'PARTIALLY_REFUNDED';
    case Refunded = 'REFUNDED';
    case Waived = 'WAIVED';

    public function getLabel(): string
    {
        return match ($this) {
            self::Unpaid => 'غير مدفوع',
            self::Paid => 'مدفوع',
            self::PartiallyRefunded => 'مسترد جزئيًا',
            self::Refunded => 'مسترد بالكامل',
            self::Waived => 'بلا مبلغ',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Unpaid => 'warning',
            self::Paid => 'success',
            self::PartiallyRefunded, self::Refunded => 'info',
            self::Waived => 'gray',
        };
    }
}
