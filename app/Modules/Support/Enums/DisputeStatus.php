<?php

declare(strict_types=1);

namespace App\Modules\Support\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** 21 §7. */
enum DisputeStatus: string implements HasColor, HasLabel
{
    case Open = 'OPEN';
    case Resolved = 'RESOLVED';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Open => 'danger',
            self::Resolved => 'success',
        };
    }
}
