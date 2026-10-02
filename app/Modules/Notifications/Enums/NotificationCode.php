<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Enums;

/**
 * كتالوج الإشعارات — 17. القناة والبريد لكل رمز من الجدول نفسه.
 */
enum NotificationCode: string
{
    case NewRequest = 'NTF-01';
    case OffersWindowClosedEmpty = 'NTF-02';
    case ExpiredWithoutSelection = 'NTF-03';
    case NewOffer = 'NTF-04';
    case OfferSelected = 'NTF-05';
    case OfferNotSelected = 'NTF-06';
    case TripStartReminder = 'NTF-07';
    case ProviderOnTheWay = 'NTF-08';
    case ProviderArrived = 'NTF-09';
    case ProposalAwaitingApproval = 'NTF-10';
    case WorkStarted = 'NTF-11';
    case ProposalDecided = 'NTF-12';
    case AmountDue = 'NTF-13';
    case ElectronicPaymentReceived = 'NTF-14';
    case CashRecordedConfirm = 'NTF-15';
    case OrderClosed = 'NTF-16';
    case DisputeUpdated = 'NTF-17';
    case RatingReminder = 'NTF-18';
    case OrderCancelled = 'NTF-19';
    case ProviderBackedOut = 'NTF-20';
    case OfferReactivated = 'NTF-21';
    case ApplicationReviewed = 'NTF-22';
    case PayoutSent = 'NTF-23';
    case DuesLimitExceeded = 'NTF-24';
    case NewMessage = 'NTF-25';
    case AccountStatusChanged = 'NTF-26';
    case ProviderAssignedToYou = 'NTF-28';
    case ProviderAssignedToOrder = 'NTF-29';
    case InstapayTransferRejected = 'NTF-30';

    /** رموز لها بريد إضافي في جدول 17. */
    public function sendsMail(): bool
    {
        return in_array($this, [
            self::OrderClosed, self::DisputeUpdated, self::ApplicationReviewed, self::PayoutSent,
            self::DuesLimitExceeded, self::AccountStatusChanged, self::InstapayTransferRejected,
        ], true);
    }

    /** NTF-26 بريد فقط (17). */
    public function sendsPush(): bool
    {
        return $this !== self::AccountStatusChanged;
    }

    /** قنوات Android في 17 §حمولة Push. */
    public function androidChannel(): string
    {
        return match ($this) {
            self::NewMessage => 'messages',
            self::ApplicationReviewed, self::PayoutSent, self::DuesLimitExceeded, self::AccountStatusChanged => 'account',
            default => 'orders',
        };
    }
}
