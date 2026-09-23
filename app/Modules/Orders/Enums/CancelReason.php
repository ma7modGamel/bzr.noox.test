<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

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
        return match ($this) {
            self::FoundAnother => 'وجدت فنيًا آخر',
            self::NoLongerNeeded => 'لم أعد بحاجة للخدمة',
            self::PriceTooHigh => 'السعر مرتفع',
            self::ProviderLate => 'تأخر الفني',
            self::WrongDetails => 'بيانات الطلب غير صحيحة',
            self::Emergency => 'ظرف طارئ',
            self::CannotReach => 'تعذّر الوصول',
            self::OutOfScope => 'العمل خارج تخصصي',
            self::UnsafeSite => 'الموقع غير آمن',
            self::CustomerNoShow => 'العميل غير موجود',
            self::WrongAddress => 'عنوان خاطئ',
            self::AdminPolicy => 'قرار إداري',
            self::FraudSuspected => 'اشتباه احتيال',
            self::Duplicate => 'طلب مكرر',
            self::AccountBlocked => 'حظر الحساب',
            self::Other => 'سبب آخر',
        };
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
                self::UnsafeSite, self::CustomerNoShow, self::WrongAddress, self::Other,
            ],
            ActorType::Admin, ActorType::System => [
                self::AdminPolicy, self::FraudSuspected, self::Duplicate,
                self::AccountBlocked, self::Other,
            ],
        };
    }
}
