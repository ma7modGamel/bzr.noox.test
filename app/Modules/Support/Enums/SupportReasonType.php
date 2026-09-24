<?php

declare(strict_types=1);

namespace App\Modules\Support\Enums;

use Filament\Support\Contracts\HasLabel;

/** مجموعات أسباب الدعم التي يديرها فريق التشغيل من لوحة الإدارة. */
enum SupportReasonType: string implements HasLabel
{
    case Dispute = 'DISPUTE';
    case ProviderReport = 'PROVIDER_REPORT';

    public function getLabel(): string
    {
        return match ($this) {
            self::Dispute => 'فتح مشكلة في طلب',
            self::ProviderReport => 'بلاغ عن فني',
        };
    }
}
