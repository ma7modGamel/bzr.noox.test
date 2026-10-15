<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use App\Support\Localization;
use Filament\Support\Contracts\HasLabel;

/** رموز أسباب الإلغاء — 13-CANCELLATION-FLOW. لا رسوم ولا غرامات (DEC-016). */
enum CancelReason: string implements HasLabel
{
    // العميل
    case FoundAnother = 'FOUND_ANOTHER';
    case NoLongerNeeded = 'NO_LONGER_NEEDED';
    case PriceTooHigh = 'PRICE_TOO_HIGH';
    case ProviderLate = 'PROVIDER_LATE';
    case WrongDetails = 'WRONG_DETAILS';

    // الفني (اعتذار / تعذّر)
    case Emergency = 'EMERGENCY';
    case CannotReach = 'CANNOT_REACH';
    case OutOfScope = 'OUT_OF_SCOPE';
    case UnsafeSite = 'UNSAFE_SITE';
    case CustomerNoShow = 'CUSTOMER_NO_SHOW';
    case WrongAddress = 'WRONG_ADDRESS';

    // الإدارة
    case AdminPolicy = 'ADMIN_POLICY';
    case FraudSuspected = 'FRAUD_SUSPECTED';
    case Duplicate = 'DUPLICATE';
    case AccountBlocked = 'ACCOUNT_BLOCKED';

    case Other = 'OTHER';

    public function getLabel(): string
    {
        return Localization::enum($this);
    }

    /** @return list<self> */
    public static function forActor(ActorType $actor): array
    {
        return match ($actor) {
            ActorType::Customer => [
                self::FoundAnother, self::NoLongerNeeded, self::PriceTooHigh,
                self::ProviderLate, self::WrongDetails, self::Other,
            ],
            ActorType::Provider => [
                self::Emergency, self::CannotReach, self::OutOfScope,
                self::UnsafeSite, self::WrongAddress, self::Other,
            ],
            ActorType::Admin, ActorType::System => [
                self::AdminPolicy, self::FraudSuspected, self::Duplicate,
                self::AccountBlocked, self::Other,
            ],
        };
    }
}
