<?php

declare(strict_types=1);

namespace App\Modules\Identity\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** DEC-021 — 23-PERMISSIONS-MATRIX. المدير العام وحده يملك المال والإعدادات. */
enum AdminRole: string implements HasColor, HasLabel
{
    case Operations = 'OPERATIONS';
    case Super = 'SUPER';

    public function getLabel(): string
    {
        return match ($this) {
            self::Operations => 'مدير تشغيل',
            self::Super => 'مدير عام',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Operations => 'info',
            self::Super => 'primary',
        };
    }

    public function isSuper(): bool
    {
        return $this === self::Super;
    }
}
