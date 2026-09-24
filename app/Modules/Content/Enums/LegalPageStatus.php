<?php

declare(strict_types=1);

namespace App\Modules\Content\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

enum LegalPageStatus: string implements HasColor, HasLabel
{
    case Draft = 'DRAFT';
    case Published = 'PUBLISHED';

    public function getLabel(): string
    {
        return match ($this) {
            self::Draft => 'مسودة',
            self::Published => 'منشورة',
        };
    }

    public function getColor(): string
    {
        return $this === self::Published ? 'success' : 'warning';
    }
}
