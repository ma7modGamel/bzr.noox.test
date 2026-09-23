<?php

declare(strict_types=1);

namespace App\Modules\Communication\Enums;

use Filament\Support\Contracts\HasLabel;

/** 21 §8 — BR-100..BR-102. */
enum ConversationStatus: string implements HasLabel
{
    case Open = 'OPEN';
    case ReadOnly = 'READ_ONLY';

    public function getLabel(): string
    {
        return match ($this) {
            self::Open => 'مفتوحة',
            self::ReadOnly => 'للقراءة فقط',
        };
    }
}
