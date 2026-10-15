<?php

declare(strict_types=1);

namespace App\Modules\Orders\Enums;

use App\Support\Localization;
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
    case ArrivalExceptionReported = 'EVT-023';
    case WorkStarted = 'EVT-030';
    case CompletionEstimateUpdated = 'EVT-031';
    case InspectionOnlyCompleted = 'EVT-035';
    case ClosedFreeInspection = 'EVT-036';
    case ProposalSubmitted = 'EVT-040';
    case ProposalApproved = 'EVT-041';
    case ProposalRejected = 'EVT-042';
    case ProposalExpired = 'EVT-043';
    case ProposalWithdrawn = 'EVT-044';
    case WorkCompleted = 'EVT-050';
    case WarrantyWorkCompleted = 'EVT-051';
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
    case CustomerNoShowReported = 'EVT-093';
    case CustomerNoShowResolved = 'EVT-094';
    case AdminNote = 'EVT-095';
    case RatingSubmitted = 'EVT-100';

    public function getLabel(): string
    {
        return Localization::enum($this);
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
            self::CustomerNoShowResolved,
        ], true);
    }
}
