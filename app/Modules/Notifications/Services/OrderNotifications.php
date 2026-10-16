<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Filament\Resources\Orders\OrderResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Data\LocalizedMessage;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Enums\NotificationCode;
use App\Modules\Notifications\Jobs\FlushOfferNotifications;
use App\Modules\Notifications\Models\NotificationDispatch;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Payments\Enums\PaymentGatewayChannel;
use App\Modules\Payments\Enums\TransferRejectionReason;
use App\Modules\Settings\Enums\OperatingMode;
use Carbon\CarbonImmutable;
use Filament\Actions\Action;
use Filament\Notifications\Notification;
use Illuminate\Support\Collection;

/**
 * إشعارات الطلب — 17 مربوطة بأحداث 24. تُستدعى بعد حفظ معاملة الحدث فقط (afterCommit).
 * النص والرابط يحددهما الخادم هنا؛ التطبيق لا يبني رابطًا من الرمز.
 */
final class OrderNotifications
{
    public const OFFER_BUNDLE_MINUTES = 5;

    private const CUSTOMER = NotificationContent::CUSTOMER;

    private const PROVIDER = NotificationContent::PROVIDER;

    public function __construct(
        private readonly Notifier $notifier,
        private readonly ProviderEligibility $eligibility,
    ) {}

    public function handle(OrderEvent $event): void
    {
        $order = $event->order()->with(['customer', 'providerProfile.user', 'category'])->first();

        if ($order === null) {
            return;
        }

        match ($event->event_code) {
            OrderEventCode::Published, OrderEventCode::Republished => $this->newRequest($order),
            OrderEventCode::OffersWindowClosed => $this->offersWindowClosed($order),
            OrderEventCode::ExpiredWithoutSelection => $this->expired($order),
            OrderEventCode::OfferSubmitted => $this->offerSubmitted($order),
            OrderEventCode::OfferAccepted => $this->offerAccepted($order),
            OrderEventCode::ProviderAssigned => $this->providerAssigned($order),
            OrderEventCode::ProviderBackedOut => $this->providerBackedOut($order, $event),
            OrderEventCode::TripStarted => $this->toCustomer($order, NotificationCode::ProviderOnTheWay, new LocalizedMessage('notifications.trip_title'), new LocalizedMessage('notifications.trip_body', ['order' => $this->number($order)])),
            OrderEventCode::Arrived => $this->toCustomer($order, NotificationCode::ProviderArrived, new LocalizedMessage('notifications.arrived_title'), new LocalizedMessage('notifications.arrived_body', ['order' => $this->number($order)])),
            OrderEventCode::CustomerNoShowReported => $this->absenceReported($order),
            OrderEventCode::WorkStarted => $this->toCustomer($order, NotificationCode::WorkStarted, new LocalizedMessage('notifications.work_title'), new LocalizedMessage('notifications.work_body', ['order' => $this->number($order)])),
            OrderEventCode::ProposalSubmitted => $this->toCustomer($order, NotificationCode::ProposalAwaitingApproval, new LocalizedMessage('notifications.proposal_title'), new LocalizedMessage('notifications.proposal_body', ['amount' => $this->money($event->meta['amount'] ?? null), 'order' => $this->number($order)])),
            OrderEventCode::ProposalApproved => $this->proposalDecided($order, new LocalizedMessage('notifications.proposal_approved')),
            OrderEventCode::ProposalRejected => $this->proposalDecided($order, new LocalizedMessage('notifications.proposal_rejected')),
            OrderEventCode::ProposalExpired => $this->proposalDecided($order, new LocalizedMessage('notifications.proposal_expired')),
            OrderEventCode::WorkCompleted, OrderEventCode::InspectionOnlyCompleted => $this->amountDue($order),
            OrderEventCode::CashReceived => $this->toCustomer($order, NotificationCode::CashRecordedConfirm, new LocalizedMessage('notifications.cash_title'), new LocalizedMessage('notifications.cash_body', ['amount' => $this->money($order->final_amount), 'order' => $this->number($order)])),
            OrderEventCode::ElectronicPaymentSucceeded => $this->toProvider($order, NotificationCode::ElectronicPaymentReceived, new LocalizedMessage('notifications.paid_title'), new LocalizedMessage('notifications.paid_body', ['amount' => $this->money($order->final_amount), 'order' => $this->number($order)])),
            OrderEventCode::PaymentAttemptFailed => $this->paymentFailed($order, $event),
            OrderEventCode::DisputeOpened => $this->dispute($order, new LocalizedMessage('notifications.dispute_opened', ['order' => $this->number($order)]), new LocalizedMessage('notifications.dispute_review')),
            OrderEventCode::DisputeResolved => $this->dispute($order, new LocalizedMessage('notifications.dispute_resolved', ['order' => $this->number($order)]), new LocalizedMessage('notifications.dispute_details')),
            OrderEventCode::Cancelled, OrderEventCode::UnableToPerform, OrderEventCode::CustomerNoShow => $this->cancelled($order, $event),
            default => null,
        };

        if ($event->to_status === OrderStatus::Closed && $event->from_status !== OrderStatus::Closed) {
            $this->closed($order);
        }
    }

    /** NTF-04: فوري لأول عرض، ثم إشعار مجمّع واحد عند انتهاء كل نافذة 5 دقائق (DEC-058). */
    private function absenceReported(Order $order): void
    {
        Notification::make()->warning()
            ->title('بلاغ غياب العميل — طلب #'.$order->number)
            ->body('الفني ينتظر. تواصل مع العميل ثم احسم بلاغ الغياب من صفحة الطلب.')
            ->actions([
                Action::make('view')->label('متابعة البلاغ')
                    ->url(OrderResource::getUrl('view', ['record' => $order->getKey()], panel: 'admin')),
            ])
            ->sendToDatabase(Admin::query()->where('is_active', true)->get());
    }

    public function flushOfferBundle(int $orderId, CarbonImmutable $since): void
    {
        $order = Order::query()->with('customer')->find($orderId);

        if ($order === null || $order->status !== OrderStatus::Open || $order->customer === null) {
            return;
        }

        $count = Offer::query()
            ->where('order_id', $orderId)
            ->where('status', OfferStatus::Submitted->value)
            ->where('submitted_at', '>', $since)
            ->count();

        if ($count === 0) {
            return;
        }

        $this->sendOfferNotification($order, new LocalizedMessage('notifications.offers_count', ['count' => $count, 'order' => $this->number($order)]));
    }

    /** NTF-07 — CFG-031. مرة واحدة لكل تعيين. */
    public function tripStartReminder(Order $order): void
    {
        $user = $order->providerProfile?->user;

        if ($user === null) {
            return;
        }

        $this->notifier->sendOnce($user, new NotificationContent(
            NotificationCode::TripStartReminder,
            new LocalizedMessage('notifications.reminder_title'),
            new LocalizedMessage('notifications.reminder_body', ['order' => $this->number($order)]),
            'provider/orders/'.$order->getKey(),
            self::PROVIDER,
            $order->getKey(),
        ), 'NTF-07:'.$order->getKey().':'.$order->providerProfile->getKey().':'.$order->confirmed_at?->timestamp);
    }

    /** NTF-18 — CFG-072. مرة واحدة لكل طرف، ولمن لم يوقفه من C33. */
    public function ratingReminder(Order $order, User $user, bool $asProvider): void
    {
        if (! $user->rating_reminders_enabled) {
            return;
        }

        $this->notifier->sendOnce($user, new NotificationContent(
            NotificationCode::RatingReminder,
            new LocalizedMessage('notifications.rating_title'),
            $asProvider
                ? new LocalizedMessage('notifications.rating_customer', ['order' => $this->number($order)])
                : new LocalizedMessage('notifications.rating_provider', ['order' => $this->number($order)]),
            $asProvider ? 'provider/orders/'.$order->getKey().'/rating' : 'orders/'.$order->getKey().'/rating',
            $asProvider ? self::PROVIDER : self::CUSTOMER,
            $order->getKey(),
        ), 'NTF-18:'.$order->getKey().':'.$user->getKey());
    }

    /** NTF-25 — نص الرسالة لا يُرسل أبدًا (DEC-058). */
    public function newMessage(Order $order, User $recipient, bool $recipientIsProvider): void
    {
        $this->notifier->send($recipient, new NotificationContent(
            NotificationCode::NewMessage,
            new LocalizedMessage('notifications.message_title'),
            new LocalizedMessage('notifications.message_body', ['order' => $this->number($order)]),
            ($recipientIsProvider ? 'provider/orders/' : 'orders/').$order->getKey().'/chat',
            $recipientIsProvider ? self::PROVIDER : self::CUSTOMER,
            $order->getKey(),
        ));
    }

    private function newRequest(Order $order): void
    {
        if ($order->status !== OrderStatus::Open) {
            return;
        }
        if ($order->operating_mode === OperatingMode::Employee) {
            Notification::make()->warning()
                ->title('طلب جديد بانتظار التعيين — #'.$order->number)
                ->body('عيّن موظفًا مؤهلًا من صفحة الطلب.')
                ->actions([
                    Action::make('assign')->label('فتح الطلب والتعيين')
                        ->url(OrderResource::getUrl('view', ['record' => $order->getKey()], panel: 'admin'))
                        ->markAsRead(),
                ])
                ->sendToDatabase(Admin::query()->where('is_active', true)->get());

            return;
        }
        if ($order->operating_mode !== OperatingMode::Marketplace) {
            return;
        }

        $this->eligibility->query($order)->get()->each(fn ($provider) => $provider->user === null ? null : $this->notifier->send(
            $provider->user,
            new NotificationContent(
                NotificationCode::NewRequest,
                new LocalizedMessage('notifications.request_title'),
                new LocalizedMessage('notifications.request_body', ['category' => LocalizedMessage::field($order->category)]),
                'provider/requests/'.$order->getKey(),
                self::PROVIDER,
                $order->getKey(),
            ),
        ));
    }

    private function offersWindowClosed(Order $order): void
    {
        if ($order->offers()->exists()) {
            return;
        }

        $this->toCustomer($order, NotificationCode::OffersWindowClosedEmpty, ...$this->noProviderTexts(
            $order, new LocalizedMessage('notifications.window_title'), new LocalizedMessage('notifications.window_body', ['order' => $this->number($order)]),
        ));
    }

    private function expired(Order $order): void
    {
        $this->toCustomer($order, NotificationCode::ExpiredWithoutSelection, ...$this->noProviderTexts(
            $order, new LocalizedMessage('notifications.expired_title'), new LocalizedMessage('notifications.expired_body', ['order' => $this->number($order)]),
        ));
    }

    /** نص وضع الموظفين لـNTF-02/03 (17 §اختلاف الإشعارات). @return array{string, string} */
    private function noProviderTexts(Order $order, string|LocalizedMessage $title, string|LocalizedMessage $body): array
    {
        return $order->operating_mode === OperatingMode::Employee
            ? [new LocalizedMessage('notifications.unavailable_title'), new LocalizedMessage('notifications.unavailable_body', ['order' => $this->number($order)])]
            : [$title, $body];
    }

    private function offerSubmitted(Order $order): void
    {
        if ($order->operating_mode !== OperatingMode::Marketplace || $order->customer === null) {
            return;
        }

        $windowOpen = NotificationDispatch::query()
            ->where('order_id', $order->getKey())
            ->where('code', NotificationCode::NewOffer->value)
            ->where('created_at', '>', now()->subMinutes(self::OFFER_BUNDLE_MINUTES))
            ->exists();

        // داخل النافذة: مهمة التفريغ المجدولة ستجمع هذا العرض.
        if (! $windowOpen) {
            $this->sendOfferNotification($order, new LocalizedMessage('notifications.offer_body', ['order' => $this->number($order)]));
        }
    }

    private function sendOfferNotification(Order $order, string|LocalizedMessage $body): void
    {
        $now = CarbonImmutable::now();
        $sent = $this->notifier->sendOnce($order->customer, new NotificationContent(
            NotificationCode::NewOffer,
            new LocalizedMessage('notifications.offers_title'),
            $body,
            'orders/'.$order->getKey().'/offers',
            self::CUSTOMER,
            $order->getKey(),
        ), 'NTF-04:'.$order->getKey().':'.$now->format('YmdHisu'));

        if ($sent !== null) {
            FlushOfferNotifications::dispatch($order->getKey(), $now)
                ->delay($now->addMinutes(self::OFFER_BUNDLE_MINUTES));
        }
    }

    private function offerAccepted(Order $order): void
    {
        if ($order->operating_mode !== OperatingMode::Marketplace) {
            return;
        }

        $this->toProvider($order, NotificationCode::OfferSelected, new LocalizedMessage('notifications.selected_title'), new LocalizedMessage('notifications.selected_body', ['order' => $this->number($order)]));

        $this->offerOwners($order, [OfferStatus::NotSelected], exceptCurrent: true)->each(fn (User $user) => $this->notifier->send(
            $user,
            new NotificationContent(NotificationCode::OfferNotSelected, new LocalizedMessage('notifications.not_selected_title'), new LocalizedMessage('notifications.not_selected_body', ['order' => $this->number($order)]), 'provider/offers', self::PROVIDER, $order->getKey()),
        ));
    }

    private function providerAssigned(Order $order): void
    {
        if ($order->operating_mode !== OperatingMode::Employee) {
            return;
        }

        $this->toProvider($order, NotificationCode::ProviderAssignedToYou, new LocalizedMessage('notifications.assigned_provider_title'), new LocalizedMessage('notifications.assigned_provider_body', ['order' => $this->number($order), 'category' => LocalizedMessage::field($order->category)]));
        $this->toCustomer($order, NotificationCode::ProviderAssignedToOrder, new LocalizedMessage('notifications.assigned_customer_title'), new LocalizedMessage('notifications.assigned_customer_body', ['order' => $this->number($order)]));
    }

    private function providerBackedOut(Order $order, OrderEvent $event): void
    {
        $marketplace = $order->operating_mode === OperatingMode::Marketplace;

        $this->toCustomer($order, NotificationCode::ProviderBackedOut, new LocalizedMessage('notifications.backed_out_title'), $marketplace
            ? new LocalizedMessage('notifications.backed_out_market', ['order' => $this->number($order)])
            : new LocalizedMessage('notifications.backed_out_employee', ['order' => $this->number($order)]));

        if (! $marketplace) {
            return;
        }

        // NTF-21 — العروض التي عادت SUBMITTED بعد الاعتذار (BR-036).
        $backedOutProviderId = $event->meta['previous_provider_profile_id'] ?? null;
        Offer::query()
            ->with('providerProfile.user')
            ->where('order_id', $order->getKey())
            ->where('status', OfferStatus::Submitted->value)
            ->when($backedOutProviderId !== null, fn ($query) => $query->where('provider_profile_id', '!=', $backedOutProviderId))
            ->get()
            ->each(fn (Offer $offer) => $offer->providerProfile?->user === null ? null : $this->notifier->send(
                $offer->providerProfile->user,
                new NotificationContent(NotificationCode::OfferReactivated, new LocalizedMessage('notifications.reactivated_title'), new LocalizedMessage('notifications.reactivated_body', ['order' => $this->number($order)]), 'provider/offers', self::PROVIDER, $order->getKey()),
            ));
    }

    private function proposalDecided(Order $order, string|LocalizedMessage $title): void
    {
        $this->toProvider($order, NotificationCode::ProposalDecided, $title, new LocalizedMessage('notifications.request_reference', ['order' => $this->number($order)]));
    }

    private function amountDue(Order $order): void
    {
        // المعاينة المجانية تُغلق مباشرة (T-28) ويصلها NTF-16 من الإغلاق.
        if ($order->status !== OrderStatus::AwaitingPayment) {
            return;
        }

        $this->toCustomer($order, NotificationCode::AmountDue, new LocalizedMessage('notifications.due_title', ['amount' => $this->money($order->final_amount)]), new LocalizedMessage('notifications.due_body', ['order' => $this->number($order)]));
    }

    private function paymentFailed(Order $order, OrderEvent $event): void
    {
        // NTF-30 لرفض تحويل إنستاباي فقط؛ فشل الدفع الإلكتروني يظهر في شاشة الدفع نفسها.
        if (($event->meta['gateway'] ?? null) !== PaymentGatewayChannel::InstapayManual->value) {
            return;
        }

        $reason = TransferRejectionReason::tryFrom((string) ($event->meta['reason'] ?? ''));

        $this->toCustomer($order, NotificationCode::InstapayTransferRejected, new LocalizedMessage('notifications.transfer_title'), new LocalizedMessage('notifications.transfer_body', ['reason' => $reason === null ? ['ar' => 'تعذّر تأكيد التحويل', 'en' => 'The transfer could not be confirmed'] : LocalizedMessage::enum($reason), 'order' => $this->number($order)]));
    }

    private function dispute(Order $order, string|LocalizedMessage $title, string|LocalizedMessage $body): void
    {
        $this->toCustomer($order, NotificationCode::DisputeUpdated, $title, $body, 'orders/'.$order->getKey().'/dispute');
        $this->toProvider($order, NotificationCode::DisputeUpdated, $title, $body);
    }

    private function cancelled(Order $order, OrderEvent $event): void
    {
        $title = new LocalizedMessage('notifications.cancelled_title');
        $body = new LocalizedMessage('notifications.cancelled_body', ['order' => $this->number($order)]);

        if ($event->actor_type !== ActorType::Customer) {
            $this->toCustomer($order, NotificationCode::OrderCancelled, $title, $body);
        }

        if ($event->actor_type !== ActorType::Provider) {
            $this->toProvider($order, NotificationCode::OrderCancelled, $title, $body);
        }

        // أصحاب العروض غير المختارة في وضع السوق.
        if ($order->operating_mode === OperatingMode::Marketplace) {
            $this->offerOwners($order, [OfferStatus::Submitted, OfferStatus::NotSelected, OfferStatus::Closed], exceptCurrent: true)
                ->each(fn (User $user) => $this->notifier->send($user, new NotificationContent(
                    NotificationCode::OrderCancelled, $title, $body, 'provider/offers', self::PROVIDER, $order->getKey(),
                )));
        }
    }

    private function closed(Order $order): void
    {
        $this->toCustomer($order, NotificationCode::OrderClosed, new LocalizedMessage('notifications.closed_title'), new LocalizedMessage('notifications.closed_customer', ['order' => $this->number($order)]));
        $this->toProvider($order, NotificationCode::OrderClosed, new LocalizedMessage('notifications.closed_title'), new LocalizedMessage('notifications.closed_provider', ['order' => $this->number($order)]));
    }

    private function toCustomer(Order $order, NotificationCode $code, string|LocalizedMessage $title, string|LocalizedMessage $body, ?string $path = null): void
    {
        if ($order->customer === null) {
            return;
        }

        $this->notifier->send($order->customer, new NotificationContent(
            $code, $title, $body, $path ?? 'orders/'.$order->getKey(), self::CUSTOMER, $order->getKey(),
        ));
    }

    private function toProvider(Order $order, NotificationCode $code, string|LocalizedMessage $title, string|LocalizedMessage $body): void
    {
        $user = $order->providerProfile?->user;

        if ($user === null) {
            return;
        }

        $this->notifier->send($user, new NotificationContent(
            $code, $title, $body, 'provider/orders/'.$order->getKey(), self::PROVIDER, $order->getKey(),
        ));
    }

    /**
     * @param  list<OfferStatus>  $statuses
     * @return Collection<int, User>
     */
    private function offerOwners(Order $order, array $statuses, bool $exceptCurrent): Collection
    {
        return Offer::query()
            ->with('providerProfile.user')
            ->where('order_id', $order->getKey())
            ->whereIn('status', array_map(static fn (OfferStatus $status): string => $status->value, $statuses))
            ->when($exceptCurrent && $order->provider_profile_id !== null, fn ($query) => $query->where('provider_profile_id', '!=', $order->provider_profile_id))
            ->get()
            ->map(static fn (Offer $offer): ?User => $offer->providerProfile?->user)
            ->filter()
            ->unique(static fn (User $user): int => $user->getKey())
            ->values();
    }

    private function number(Order $order): string
    {
        return '#'.$order->number;
    }

    private function money(mixed $amount): string
    {
        $value = is_numeric($amount) ? (float) $amount : 0.0;

        return rtrim(rtrim(number_format($value, 2, '.', ''), '0'), '.');
    }
}
