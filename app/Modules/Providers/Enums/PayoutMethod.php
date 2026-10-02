<?php

declare(strict_types=1);

namespace App\Modules\Providers\Enums;

use Filament\Support\Contracts\HasLabel;

enum PayoutMethod: string implements HasLabel
{
    case Instapay = 'INSTAPAY';
    case Wallet = 'WALLET';
    case Bank = 'BANK';

    public function getLabel(): string
    {
        return match ($this) {
            self::Instapay => 'إنستاباي',
            self::Wallet => 'محفظة إلكترونية',
            self::Bank => 'حساب بنكي',
        };
    }

    public function fieldLabel(): string
    {
        return match ($this) {
            self::Instapay => 'رقم أو عنوان إنستاباي',
            self::Wallet => 'رقم المحفظة',
            self::Bank => 'رقم IBAN',
        };
    }
}
