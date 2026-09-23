<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** 21 §3 — انتهاء المهلة = رفض (BR-041، DEC-029). */
enum ProposalStatus: string implements HasColor, HasLabel
{
    case Pending = 'PENDING';
    case Approved = 'APPROVED';
    case Rejected = 'REJECTED';
    case Expired = 'EXPIRED';
    case Withdrawn = 'WITHDRAWN';

    public function getLabel(): string
    {
        return match ($this) {
            self::Pending => 'بانتظار الرد',
            self::Approved => 'موافق عليه',
            self::Rejected => 'مرفوض',
            self::Expired => 'انتهت مهلته',
            self::Withdrawn => 'مسحوب',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Pending => 'warning',
            self::Approved => 'success',
            self::Rejected, self::Expired => 'danger',
            self::Withdrawn => 'gray',
        };
    }

    /** المقترحات التي تدخل في حساب المبالغ (BR-044). */
    public function countsTowardTotals(): bool
    {
        return $this === self::Approved;
    }
}
