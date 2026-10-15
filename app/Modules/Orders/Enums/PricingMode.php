<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** BR-017، BR-008 — في وضع الموظفين كل الطلبات INSPECTION والحقل مخفي. */
enum PricingMode: string implements HasLabel
{
    case Execution = 'EXECUTION';
    case Inspection = 'INSPECTION';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
