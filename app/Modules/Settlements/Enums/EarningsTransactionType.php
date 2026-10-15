<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

enum EarningsTransactionType: string implements HasLabel
{
    case All = 'ALL';
    case Order = 'ORDER';
    case Payout = 'PAYOUT';
    case Remittance = 'REMITTANCE';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
