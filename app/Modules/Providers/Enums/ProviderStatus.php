<?php

declare(strict_types=1);

namespace App\Modules\Providers\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/** 21 §4 — 15-PROVIDER-ONBOARDING. */
enum ProviderStatus: string implements HasColor, HasLabel
{
    case PendingReview = 'PENDING_REVIEW';
    case Active = 'ACTIVE';
    case Rejected = 'REJECTED';
    case Suspended = 'SUSPENDED';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }

    public function getColor(): string
    {
        return match ($this) {
            self::PendingReview => 'warning',
            self::Active => 'success',
            self::Rejected => 'danger',
            self::Suspended => 'gray',
        };
    }
}
