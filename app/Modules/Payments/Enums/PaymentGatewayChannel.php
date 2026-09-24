<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

use App\Modules\Settings\Enums\Cfg;
use Filament\Support\Contracts\HasLabel;

/**
 * قنوات `PaymentGateway` (DEC-050، BR-051). كل قناة تُفعَّل من اللوحة وتصل التطبيق في
 * `GET /config`، والتطبيق يعرض المفعّل فقط.
 */
enum PaymentGatewayChannel: string implements HasLabel
{
    case Cash = 'CASH';
    case InstapayManual = 'INSTAPAY_MANUAL';
    case Fawry = 'FAWRY';

    public function getLabel(): string
    {
        return match ($this) {
            self::Cash => 'نقدي للفني',
            self::InstapayManual => 'تحويل إنستاباي',
            self::Fawry => 'فوري (بطاقة، محفظة، كود منافذ)',
        };
    }

    public function setting(): Cfg
    {
        return match ($this) {
            self::Cash => Cfg::ChannelCashEnabled,
            self::InstapayManual => Cfg::ChannelInstapayEnabled,
            self::Fawry => Cfg::ChannelFawryEnabled,
        };
    }

    /** طريقة الدفع على الطلب (BR-050) التي تنتمي لها القناة. */
    public function method(): PaymentMethod
    {
        return $this === self::Cash ? PaymentMethod::Cash : PaymentMethod::Electronic;
    }
}
