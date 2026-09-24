<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use Filament\Support\Contracts\HasLabel;

/** قنوات فوري التي تعرضها C22. */
enum PaymentChannel: string implements HasLabel
{
    case Card = 'CARD';
    case Wallet = 'WALLET';
    case Kiosk = 'KIOSK';

    public function getLabel(): string
    {
        return match ($this) {
            self::Card => 'بطاقة بنكية',
            self::Wallet => 'محفظة إلكترونية',
            self::Kiosk => 'كود دفع من منفذ فوري',
        };
    }
}
