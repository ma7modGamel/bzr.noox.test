<?php

declare(strict_types=1);

namespace App\Modules\Settings\Enums;

use Filament\Support\Contracts\HasLabel;

enum CfgGroup: string implements HasLabel
{
    case OperatingMode = 'operating_mode';
    case Offers = 'offers';
    case Scheduling = 'scheduling';
    case Assignment = 'assignment';
    case Execution = 'execution';
    case Pricing = 'pricing';
    case Payments = 'payments';
    case Settlements = 'settlements';
    case Ratings = 'ratings';
    case Tracking = 'tracking';

    public function getLabel(): string
    {
        return match ($this) {
            self::OperatingMode => 'وضع التشغيل',
            self::Offers => 'العروض',
            self::Scheduling => 'المواعيد والجدولة',
            self::Assignment => 'التعيين (وضع الموظفين)',
            self::Execution => 'التنفيذ والتتبع',
            self::Pricing => 'الأسعار والمقترحات',
            self::Payments => 'الدفع',
            self::Settlements => 'العمولة والتسويات',
            self::Ratings => 'التقييم والتواصل',
            self::Tracking => 'الموقع والخصوصية',
        };
    }
}
