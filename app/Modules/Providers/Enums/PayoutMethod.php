<?php

declare(strict_types=1);

namespace App\Modules\Providers\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

enum PayoutMethod: string implements HasLabel
{
    case Instapay = 'INSTAPAY';
    case Wallet = 'WALLET';
    case Bank = 'BANK';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }

    public function fieldLabel(): string
    {
        return Localization::enum($this, 'field');
    }
}
