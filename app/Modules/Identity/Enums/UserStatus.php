<?php

declare(strict_types=1);

namespace App\Modules\Identity\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** 21 §5 — BR-003. */
enum UserStatus: string implements HasColor, HasLabel
{
    case Active = 'ACTIVE';
    case Blocked = 'BLOCKED';

    public function getLabel(): string
    {
        return match ($this) {
            self::Active => Localization::text('copy.active'),
            self::Blocked => Localization::text('copy.blocked'),
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Active => 'success',
            self::Blocked => 'danger',
        };
    }
}
