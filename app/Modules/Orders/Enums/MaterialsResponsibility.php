<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use Filament\Support\Contracts\HasLabel;

/** BR-016 — معلوماتية للفني؛ التكلفة لا تدخل إلا بمقترح MATERIALS (DEC-007). */
enum MaterialsResponsibility: string implements HasLabel
{
    case CustomerHas = 'CUSTOMER_HAS';
    case ProviderSupplies = 'PROVIDER_SUPPLIES';
    case Unsure = 'UNSURE';

    public function getLabel(): string
    {
        return match ($this) {
            self::CustomerHas => 'عندي الخامات',
            self::ProviderSupplies => 'الفني يوفر الخامات',
            self::Unsure => 'غير محدد',
        };
    }
}
