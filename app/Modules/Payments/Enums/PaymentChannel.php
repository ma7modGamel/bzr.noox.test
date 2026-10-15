<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** قنوات فوري التي تعرضها C22. */
enum PaymentChannel: string implements HasLabel
{
    case Card = 'CARD';
    case Wallet = 'WALLET';
    case Kiosk = 'KIOSK';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
