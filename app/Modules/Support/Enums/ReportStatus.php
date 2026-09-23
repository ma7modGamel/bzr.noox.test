<?php

declare(strict_types=1);

namespace App\Modules\Support\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** 21 §9 — ASM-11: البلاغ ملف مستقل عن النزاع ولا يغير أي حالة (BR-121). */
enum ReportStatus: string implements HasColor, HasLabel
{
    case New = 'NEW';
    case Reviewed = 'REVIEWED';

    public function getLabel(): string
    {
        return match ($this) {
            self::New => 'جديد',
            self::Reviewed => 'تمت مراجعته',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::New => 'warning',
            self::Reviewed => 'success',
        };
    }
}
