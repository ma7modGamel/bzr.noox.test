<?php

declare(strict_types=1);

namespace App\Modules\Offers\Enums;

use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** O-01..O-07 — 09-OFFER-LIFECYCLE. */
enum OfferStatus: string implements HasColor, HasLabel
{
    case Submitted = 'SUBMITTED';
    case Withdrawn = 'WITHDRAWN';
    case Accepted = 'ACCEPTED';
    case NotSelected = 'NOT_SELECTED';
    case Closed = 'CLOSED';
    case BackedOut = 'BACKED_OUT';

    public function getLabel(): string
    {
        return match ($this) {
            self::Submitted => 'مقدَّم',
            self::Withdrawn => 'مسحوب',
            self::Accepted => 'مختار',
            self::NotSelected => 'لم يُختر',
            self::Closed => 'مغلق',
            self::BackedOut => 'اعتذر الفني',
        };
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Submitted => 'warning',
            self::Accepted => 'success',
            self::BackedOut => 'danger',
            default => 'gray',
        };
    }
}
