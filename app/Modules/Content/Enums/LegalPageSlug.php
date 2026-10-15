<?php

declare(strict_types=1);

namespace App\Modules\Content\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** الصفحات الأربع في DEC-051؛ المسار العام نفسه هو الـ slug. */
enum LegalPageSlug: string implements HasLabel
{
    case Terms = 'terms';
    case Privacy = 'privacy';
    case Cancellation = 'cancellation-policy';
    case Faq = 'faq';

    public function getLabel(): string
    {
        return match ($this) {
            self::Terms => Localization::text('copy.terms_and_conditions'),
            self::Privacy => Localization::text('copy.privacy_policy'),
            self::Cancellation => Localization::text('copy.cancellation_policy'),
            self::Faq => Localization::text('copy.frequently_asked_questions'),
        };
    }
}
