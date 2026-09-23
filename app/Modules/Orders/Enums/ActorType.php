<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use Filament\Support\Contracts\HasLabel;

/** `order_events.actor_type` — 24. */
enum ActorType: string implements HasLabel
{
    case Customer = 'CUSTOMER';
    case Provider = 'PROVIDER';
    case Admin = 'ADMIN';
    case System = 'SYSTEM';

    public function getLabel(): string
    {
        return match ($this) {
            self::Customer => 'العميل',
            self::Provider => 'مقدم الخدمة',
            self::Admin => 'الإدارة',
            self::System => 'النظام',
        };
    }
}
