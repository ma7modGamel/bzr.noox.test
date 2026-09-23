<?php

declare(strict_types=1);

namespace App\Modules\Providers\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/**
 * صفة التعاقد — 39. بيان تعاقدي لا حالة: لا يؤثر على أي انتقال.
 * وضع التشغيل على مستوى المنصة، والصفة على مستوى الملف.
 */
enum EmploymentType: string implements HasColor, HasLabel
{
    case Employee = 'EMPLOYEE';
    case Independent = 'INDEPENDENT';

    public function getLabel(): string
    {
        return match ($this) {
            self::Employee => 'موظف لدى المنصة',
            self::Independent => 'مقدم خدمة مستقل',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Employee => 'info',
            self::Independent => 'primary',
        };
    }
}
