<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use App\Support\Localization;
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
            self::Unpaid => Localization::text('copy.unpaid'),
            self::Paid => Localization::text('copy.paid'),
            self::PartiallyRefunded => Localization::text('copy.partially_refunded'),
            self::Refunded => Localization::text('copy.fully_refunded'),
            self::Waived => Localization::text('copy.no_amount_due'),
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
