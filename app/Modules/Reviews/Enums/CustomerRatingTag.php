<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

enum CustomerRatingTag: string implements HasLabel
{
    case Cooperative = 'COOPERATIVE';
    case ClearDescription = 'CLEAR_DESCRIPTION';
    case Punctual = 'PUNCTUAL';
    case Respectful = 'RESPECTFUL';
    case SlowResponse = 'SLOW_RESPONSE';
    case UnpreparedSite = 'UNPREPARED_SITE';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
