<?php

declare(strict_types=1);

namespace App\Modules\Settings\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/**
 * وضع التشغيل — 39-OPERATING-MODES، BR-006.
 * يُثبَّت على كل طلب عند النشر (`orders.operating_mode`, BR-009) فلا يتأثر بتبديل المفتاح لاحقًا.
 */
enum OperatingMode: string implements HasColor, HasLabel
{
    /** CFG-090 معطّل: موظفون + تعيين يدوي + بلا عروض. */
    case Employee = 'EMPLOYEE';

    /** CFG-090 مفعّل: النموذج الكامل بعروض ومقارنة واختيار. */
    case Marketplace = 'MARKETPLACE';

    public function getLabel(): string
    {
        return match ($this) {
            self::Employee => 'وضع الموظفين',
            self::Marketplace => 'وضع السوق',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Employee => 'info',
            self::Marketplace => 'primary',
        };
    }

    public function hasOffers(): bool
    {
        return $this === self::Marketplace;
    }
}
