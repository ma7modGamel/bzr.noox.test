<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** أسباب رفض تحويل إنستاباي — BR-057. */
enum TransferRejectionReason: string implements HasLabel
{
    case NotReceived = 'NOT_RECEIVED';
    case AmountMismatch = 'AMOUNT_MISMATCH';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
