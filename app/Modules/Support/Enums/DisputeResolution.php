<?php

declare(strict_types=1);

namespace App\Modules\Support\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** 16 §قرار النزاع. */
enum DisputeResolution: string implements HasLabel
{
    case CloseAsIs = 'CLOSE_AS_IS';
    case CloseAdjusted = 'CLOSE_ADJUSTED';
    case CloseWithRefund = 'CLOSE_WITH_REFUND';
    case CancelFullRefund = 'CANCEL_FULL_REFUND';
    case CloseWithoutPayment = 'CLOSE_WITHOUT_PAYMENT';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
