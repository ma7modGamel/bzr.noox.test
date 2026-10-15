<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** BR-050، DEC-011، DEC-038. */
enum PaymentMethod: string implements HasLabel
{
    case Cash = 'CASH';
    case Electronic = 'ELECTRONIC';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
