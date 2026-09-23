<?php

declare(strict_types=1);

namespace App\Modules\Offers\Enums;

use Filament\Support\Contracts\HasLabel;

/**
 * BR-007 — التعيين في وضع الموظفين يُكتب في نفس جدول العروض بحالة ACCEPTED
 * وسعر 0.00 ومصدر ADMIN_ASSIGNMENT، فيبقى نموذج البيانات واحدًا بلا كيان جديد.
 * صف التعيين لا تسري عليه انتقالات O-01..O-07 ولا يظهر لأي طرف كعرض.
 */
enum OfferSource: string implements HasLabel
{
    case Provider = 'PROVIDER';
    case AdminAssignment = 'ADMIN_ASSIGNMENT';

    public function getLabel(): string
    {
        return match ($this) {
            self::Provider => 'عرض من مقدم الخدمة',
            self::AdminAssignment => 'تعيين إداري',
        };
    }

    public function isAssignment(): bool
    {
        return $this === self::AdminAssignment;
    }
}
