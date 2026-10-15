<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** BR-017، DEC-004. */
enum TimingType: string implements HasLabel
{
    case Now = 'NOW';
    case Scheduled = 'SCHEDULED';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }
}
