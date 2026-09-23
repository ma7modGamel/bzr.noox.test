<?php

declare(strict_types=1);

namespace App\Filament\Support;

/**
 * مجموعات التنقل — ترتيبها يتبع وحدات 16-ADMIN-OPERATIONS:
 * التشغيل اليومي أولًا، ثم الناس، ثم المال، ثم الإعداد.
 */
final class NavigationGroups
{
    public const Operations = 'التشغيل';
    public const People = 'الناس';
    public const Care = 'الجودة والدعم';
    public const Money = 'المال';
    public const Configuration = 'الإعداد';

    /** @return list<string> */
    public static function all(): array
    {
        return [
            self::Operations,
            self::People,
            self::Care,
            self::Money,
            self::Configuration,
        ];
    }
}
