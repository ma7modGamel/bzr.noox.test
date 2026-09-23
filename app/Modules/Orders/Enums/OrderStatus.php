<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use App\Modules\Settings\Enums\OperatingMode;
use Filament\Support\Contracts\HasColor;
use Filament\Support\Contracts\HasLabel;

/**
 * STATE-ORDER-01..12 — 21-STATUS-AND-STATE-MODEL.
 * لا حالة جديدة في وضع الموظفين؛ الفرق في المداخل والمخارج فقط (39).
 */
enum OrderStatus: string implements HasColor, HasLabel
{
    case Open = 'OPEN';
    case Confirmed = 'CONFIRMED';
    case OnTheWay = 'ON_THE_WAY';
    case Arrived = 'ARRIVED';
    case AwaitingQuoteApproval = 'AWAITING_QUOTE_APPROVAL';
    case InProgress = 'IN_PROGRESS';
    case AwaitingPayment = 'AWAITING_PAYMENT';
    case AwaitingConfirmation = 'AWAITING_CONFIRMATION';
    case Disputed = 'DISPUTED';
    case Closed = 'CLOSED';
    case Cancelled = 'CANCELLED';
    case Expired = 'EXPIRED';

    public function getLabel(): string
    {
        return match ($this) {
            self::Open => 'استقبال العروض',
            self::Confirmed => 'تم التأكيد',
            self::OnTheWay => 'في الطريق',
            self::Arrived => 'وصل',
            self::AwaitingQuoteApproval => 'بانتظار الموافقة على عرض التنفيذ',
            self::InProgress => 'جاري التنفيذ',
            self::AwaitingPayment => 'بانتظار الدفع',
            self::AwaitingConfirmation => 'بانتظار تأكيد الإنهاء',
            self::Disputed => 'قيد المراجعة',
            self::Closed => 'مكتمل',
            self::Cancelled => 'ملغي',
            self::Expired => 'منتهي',
        };
    }

    /** الاسم يختلف في وضع الموظفين لحالة OPEN فقط (21). */
    public function labelFor(OperatingMode $mode): string
    {
        if ($this === self::Open && $mode === OperatingMode::Employee) {
            return 'بانتظار تعيين فني';
        }

        return $this->getLabel();
    }

    public function getColor(): string
    {
        return match ($this) {
            self::Open => 'warning',
            self::Confirmed, self::OnTheWay, self::Arrived => 'info',
            self::AwaitingQuoteApproval, self::AwaitingPayment, self::AwaitingConfirmation => 'warning',
            self::InProgress => 'primary',
            self::Disputed => 'danger',
            self::Closed => 'success',
            self::Cancelled, self::Expired => 'gray',
        };
    }

    public function isFinal(): bool
    {
        return in_array($this, [self::Closed, self::Cancelled, self::Expired], true);
    }

    /** الحالات التي يكون فيها للطلب فني مُسنَد. */
    public function hasAssignedProvider(): bool
    {
        return ! in_array($this, [self::Open, self::Expired], true);
    }

    /** من ARRIVED فصاعدًا يُتاح "فتح مشكلة" (BR-120). */
    public function allowsDispute(): bool
    {
        return in_array($this, [
            self::Arrived,
            self::AwaitingQuoteApproval,
            self::InProgress,
            self::AwaitingPayment,
            self::AwaitingConfirmation,
        ], true);
    }
}
