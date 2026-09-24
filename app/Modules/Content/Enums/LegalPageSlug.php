<?php

declare(strict_types=1);

namespace App\Modules\Content\Enums;

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
            self::Terms => 'الشروط والأحكام',
            self::Privacy => 'سياسة الخصوصية',
            self::Cancellation => 'سياسة الإلغاء',
            self::Faq => 'الأسئلة الشائعة',
        };
    }
}
