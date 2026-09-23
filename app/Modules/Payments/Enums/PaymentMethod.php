<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use Filament\Support\Contracts\HasLabel;

/** BR-050، DEC-011، DEC-038. */
enum PaymentMethod: string implements HasLabel
{
    case Cash = 'CASH';
    case Electronic = 'ELECTRONIC';

    public function getLabel(): string
    {
        return match ($this) {
            self::Cash => 'نقدي',
            self::Electronic => 'إلكتروني (فوري)',
        };
    }
}
