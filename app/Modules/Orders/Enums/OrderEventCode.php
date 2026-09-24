<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use Filament\Support\Contracts\HasLabel;

/**
 * سجل أحداث الطلب — 24-ORDER-EVENT-TIMELINE.
 * يُكتب في نفس معاملة الإجراء، ولا يُعدّل ولا يُحذف.
 */
enum OrderEventCode: string implements HasLabel
{
    case Published = 'EVT-001';
    case Updated = 'EVT-002';
    case Republished = 'EVT-003';
    case OffersWindowClosed = 'EVT-004';
    case ExpiredWithoutSelection = 'EVT-005';
    case OfferSubmitted = 'EVT-006';
    case OfferWithdrawn = 'EVT-007';
    case OfferAccepted = 'EVT-010';
    case ProviderAssigned = 'EVT-011';
    case ProviderBackedOut = 'EVT-012';
    case PaymentMethodChanged = 'EVT-013';
    case TripStarted = 'EVT-020';
    case DelayAlert = 'EVT-021';
    case Arrived = 'EVT-022';
    case WorkStarted = 'EVT-030';
    case InspectionOnlyCompleted = 'EVT-035';
    case ClosedFreeInspection = 'EVT-036';
    case ProposalSubmitted = 'EVT-040';
    case ProposalApproved = 'EVT-041';
    case ProposalRejected = 'EVT-042';
    case ProposalExpired = 'EVT-043';
    case ProposalWithdrawn = 'EVT-044';
    case WorkCompleted = 'EVT-050';
    case PaymentAttemptCreated = 'EVT-060';
    case CashReceived = 'EVT-061';
    case ElectronicPaymentSucceeded = 'EVT-062';
    case PaymentAttemptFailed = 'EVT-063';
    case InstapayTransferSubmitted = 'EVT-064';
    case CompletionConfirmed = 'EVT-070';
    case AutoClosed = 'EVT-071';
    case ClosedWithoutPayment = 'EVT-072';
    case DisputeOpened = 'EVT-080';
    case DisputeResolved = 'EVT-081';
    case RefundRecorded = 'EVT-082';
    case Cancelled = 'EVT-090';
    case UnableToPerform = 'EVT-091';
    case CustomerNoShow = 'EVT-092';
    case AdminNote = 'EVT-095';
    case RatingSubmitted = 'EVT-100';

    public function getLabel(): string
    {
        return match ($this) {
            self::Published => 'نشر الطلب',
            self::Updated => 'تعديل الطلب',
            self::Republished => 'إعادة النشر',
            self::OffersWindowClosed => 'انتهاء نافذة العروض',
            self::ExpiredWithoutSelection => 'انتهاء الطلب بلا اختيار',
            self::OfferSubmitted => 'تقديم عرض',
            self::OfferWithdrawn => 'سحب عرض',
            self::OfferAccepted => 'قبول عرض',
            self::ProviderAssigned => 'تعيين مقدم خدمة',
            self::ProviderBackedOut => 'اعتذار الفني / إعادة فتح إدارية',
            self::PaymentMethodChanged => 'تغيير طريقة الدفع',
            self::TripStarted => 'بدء التحرك',
            self::DelayAlert => 'تنبيه تأخر',
            self::Arrived => 'وصل',
            self::WorkStarted => 'بدء التنفيذ',
            self::InspectionOnlyCompleted => 'إنهاء بالمعاينة فقط',
            self::ClosedFreeInspection => 'إغلاق بمعاينة مجانية بلا تنفيذ',
            self::ProposalSubmitted => 'تقديم مقترح',
            self::ProposalApproved => 'موافقة على مقترح',
            self::ProposalRejected => 'رفض مقترح',
            self::ProposalExpired => 'انتهاء مهلة مقترح',
            self::ProposalWithdrawn => 'سحب مقترح',
            self::WorkCompleted => 'تم الإنهاء',
            self::PaymentAttemptCreated => 'إنشاء محاولة دفع',
            self::CashReceived => 'تأكيد استلام نقدي',
            self::ElectronicPaymentSucceeded => 'دفع إلكتروني ناجح',
            self::PaymentAttemptFailed => 'فشل/انتهاء محاولة دفع',
            self::InstapayTransferSubmitted => 'إرسال تحويل إنستاباي للتأكيد',
            self::CompletionConfirmed => 'تأكيد العميل للإنهاء',
            self::AutoClosed => 'إغلاق تلقائي',
            self::ClosedWithoutPayment => 'إغلاق بدون دفع',
            self::DisputeOpened => 'فتح مشكلة',
            self::DisputeResolved => 'قرار النزاع',
            self::RefundRecorded => 'تسجيل استرداد',
            self::Cancelled => 'إلغاء',
            self::UnableToPerform => 'تعذّر التنفيذ',
            self::CustomerNoShow => 'العميل غير موجود',
            self::AdminNote => 'ملاحظة إدارية',
            self::RatingSubmitted => 'تقييم مُقدم',
        };
    }

    /** الأحداث الداخلية لا تظهر للعميل والفني في الملخص المبسط (24 §العرض). */
    public function isInternal(): bool
    {
        return in_array($this, [
            self::OffersWindowClosed,
            self::DelayAlert,
            self::PaymentAttemptCreated,
            self::PaymentAttemptFailed,
            self::AdminNote,
        ], true);
    }
}
