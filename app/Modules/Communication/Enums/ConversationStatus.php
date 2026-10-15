<?php

declare(strict_types=1);

namespace App\Modules\Communication\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** 21 §8 — BR-100..BR-102. */
enum ConversationStatus: string implements HasLabel
{
    case Open = 'OPEN';
    case ReadOnly = 'READ_ONLY';

    public function getLabel(): string
    {
        return match ($this) {
            self::Open => Localization::text('copy.open'),
            self::ReadOnly => Localization::text('copy.read_only'),
        };
    }
}
