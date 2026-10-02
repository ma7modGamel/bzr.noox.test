<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Services;

use App\Modules\Identity\Models\User;
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
            OrderEventCode::TripStarted => $this->toCustomer($order, NotificationCode::ProviderOnTheWay, 'الفني في الطريق', 'الفني في الطريق إليك لطلب '.$this->number($order).'. تابع موقعه ووقت الوصول.'),
            OrderEventCode::Arrived => $this->toCustomer($order, NotificationCode::ProviderArrived, 'وصل الفني', 'وصل الفني إلى عنوانك لطلب '.$this->number($order).'.'),
            OrderEventCode::WorkStarted => $this->toCustomer($order, NotificationCode::WorkStarted, 'بدأ التنفيذ', 'بدأ الفني العمل في طلب '.$this->number($order).'.'),
            OrderEventCode::ProposalSubmitted => $this->toCustomer($order, NotificationCode::ProposalAwaitingApproval, 'مقترح سعر بانتظار موافقتك', 'أرسل الفني مقترحًا بمبلغ '.$this->money($event->meta['amount'] ?? null).' لطلب '.$this->number($order).'. راجعه قبل انتهاء المهلة.'),
            OrderEventCode::ProposalApproved => $this->proposalDecided($order, 'وافق العميل على مقترحك'),
            OrderEventCode::ProposalRejected => $this->proposalDecided($order, 'رفض العميل مقترحك'),
            OrderEventCode::ProposalExpired => $this->proposalDecided($order, 'انتهت مهلة مقترحك'),
            OrderEventCode::WorkCompleted, OrderEventCode::InspectionOnlyCompleted => $this->amountDue($order),
            OrderEventCode::CashReceived => $this->toCustomer($order, NotificationCode::CashRecordedConfirm, 'أكّد إنهاء الطلب', 'سجّل الفني استلام '.$this->money($order->final_amount).' نقدًا لطلب '.$this->number($order).'. أكّد الإنهاء أو افتح مشكلة.'),
            OrderEventCode::ElectronicPaymentSucceeded => $this->toProvider($order, NotificationCode::ElectronicPaymentReceived, 'تم الدفع الإلكتروني', 'دفع العميل '.$this->money($order->final_amount).' لطلب '.$this->number($order).'.'),
            OrderEventCode::PaymentAttemptFailed => $this->paymentFailed($order, $event),
            OrderEventCode::DisputeOpened => $this->dispute($order, 'فُتحت مشكلة على طلب '.$this->number($order), 'نراجع المشكلة ونبلغك بالقرار.'),
            OrderEventCode::DisputeResolved => $this->dispute($order, 'صدر قرار في مشكلة طلب '.$this->number($order), 'افتح الطلب لمعرفة القرار.'),
            OrderEventCode::Cancelled, OrderEventCode::UnableToPerform, OrderEventCode::CustomerNoShow => $this->cancelled($order, $event),
            default => null,
        };

        if ($event->to_status === OrderStatus::Closed && $event->from_status !== OrderStatus::Closed) {
            $this->closed($order);
        }
    }

    /** NTF-04: فوري لأول عرض، ثم إشعار مجمّع واحد عند انتهاء كل نافذة 5 دقائق (DEC-058). */
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

        $this->sendOfferNotification($order, $this->offersPhrase($count).' لطلب '.$this->number($order).'.');
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
            'حان وقت التحرك',
            'لم تبدأ التحرك لطلب '.$this->number($order).' بعد. ابدأ الآن أو تواصل مع العميل.',
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
            'قيّم تجربتك',
            $asProvider
                ? 'قيّم العميل في طلب '.$this->number($order).'.'
                : 'شاركنا تقييمك للفني في طلب '.$this->number($order).'.',
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
            'رسالة جديدة',
            'رسالة جديدة بخصوص طلب '.$this->number($order).'.',
            ($recipientIsProvider ? 'provider/orders/' : 'orders/').$order->getKey().'/chat',
            $recipientIsProvider ? self::PROVIDER : self::CUSTOMER,
            $order->getKey(),
        ));
    }

    private function newRequest(Order $order): void
    {
        if ($order->operating_mode !== OperatingMode::Marketplace || $order->status !== OrderStatus::Open) {
            return;
        }

        $this->eligibility->query($order)->get()->each(fn ($provider) => $provider->user === null ? null : $this->notifier->send(
            $provider->user,
            new NotificationContent(
                NotificationCode::NewRequest,
                'طلب جديد في منطقتك',
                $order->category?->name.' — قدّم عرضك قبل انتهاء المهلة.',
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
            $order, 'انتهت مهلة العروض', 'لم يصل عرض لطلب '.$this->number($order).'. يمكنك تعديله أو إعادة نشره.',
        ));
    }

    private function expired(Order $order): void
    {
        $this->toCustomer($order, NotificationCode::ExpiredWithoutSelection, ...$this->noProviderTexts(
            $order, 'انتهى الطلب بلا اختيار', 'انتهت مهلة اختيار عرض لطلب '.$this->number($order).'. يمكنك إعادة نشره.',
        ));
    }

    /** نص وضع الموظفين لـNTF-02/03 (17 §اختلاف الإشعارات). @return array{string, string} */
    private function noProviderTexts(Order $order, string $title, string $body): array
    {
        return $order->operating_mode === OperatingMode::Employee
            ? ['لم يتوفر فني', 'لم يتوفر فني في الوقت المناسب لطلب '.$this->number($order).'. يمكنك إعادة نشره.']
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
            $this->sendOfferNotification($order, 'وصل عرض جديد لطلب '.$this->number($order).'.');
        }
    }

    private function sendOfferNotification(Order $order, string $body): void
    {
        $now = CarbonImmutable::now();
        $sent = $this->notifier->sendOnce($order->customer, new NotificationContent(
            NotificationCode::NewOffer,
            'عروض جديدة على طلبك',
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

        $this->toProvider($order, NotificationCode::OfferSelected, 'تم اختيار عرضك', 'اختار العميل عرضك لطلب '.$this->number($order).'. راجع الموعد والعنوان.');

        $this->offerOwners($order, [OfferStatus::NotSelected], exceptCurrent: true)->each(fn (User $user) => $this->notifier->send(
            $user,
            new NotificationContent(NotificationCode::OfferNotSelected, 'لم يُختر عرضك', 'اختار العميل عرضًا آخر لطلب '.$this->number($order).'.', 'provider/offers', self::PROVIDER, $order->getKey()),
        ));
    }

    private function providerAssigned(Order $order): void
    {
        if ($order->operating_mode !== OperatingMode::Employee) {
            return;
        }

        $this->toProvider($order, NotificationCode::ProviderAssignedToYou, 'طلب معيّن لك', 'عُيّن لك طلب '.$this->number($order).' — '.$order->category?->name.'. راجع الموعد والعنوان.');
        $this->toCustomer($order, NotificationCode::ProviderAssignedToOrder, 'تم تعيين فني لطلبك', 'عُيّن فني لطلب '.$this->number($order).'. يمكنك متابعته الآن.');
    }

    private function providerBackedOut(Order $order, OrderEvent $event): void
    {
        $marketplace = $order->operating_mode === OperatingMode::Marketplace;

        $this->toCustomer($order, NotificationCode::ProviderBackedOut, 'اعتذر الفني', $marketplace
            ? 'طلبك '.$this->number($order).' يستقبل عروضًا من جديد.'
            : 'جارٍ تعيين فني بديل لطلب '.$this->number($order).'.');

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
                new NotificationContent(NotificationCode::OfferReactivated, 'أُعيد تفعيل عرضك', 'عرضك على طلب '.$this->number($order).' متاح للعميل من جديد.', 'provider/offers', self::PROVIDER, $order->getKey()),
            ));
    }

    private function proposalDecided(Order $order, string $title): void
    {
        $this->toProvider($order, NotificationCode::ProposalDecided, $title, 'طلب '.$this->number($order).'.');
    }

    private function amountDue(Order $order): void
    {
        // المعاينة المجانية تُغلق مباشرة (T-28) ويصلها NTF-16 من الإغلاق.
        if ($order->status !== OrderStatus::AwaitingPayment) {
            return;
        }

        $this->toCustomer($order, NotificationCode::AmountDue, 'المبلغ المستحق '.$this->money($order->final_amount), 'انتهى العمل في طلب '.$this->number($order).'. راجع التفاصيل وادفع.');
    }

    private function paymentFailed(Order $order, OrderEvent $event): void
    {
        // NTF-30 لرفض تحويل إنستاباي فقط؛ فشل الدفع الإلكتروني يظهر في شاشة الدفع نفسها.
        if (($event->meta['gateway'] ?? null) !== PaymentGatewayChannel::InstapayManual->value) {
            return;
        }

        $reason = TransferRejectionReason::tryFrom((string) ($event->meta['reason'] ?? ''));

        $this->toCustomer($order, NotificationCode::InstapayTransferRejected, 'تعذّر تأكيد تحويل إنستاباي', ($reason?->getLabel() ?? 'تعذّر تأكيد التحويل').' — يمكنك إعادة المحاولة أو الدفع نقدًا للفني. طلب '.$this->number($order));
    }

    private function dispute(Order $order, string $title, string $body): void
    {
        $this->toCustomer($order, NotificationCode::DisputeUpdated, $title, $body, 'orders/'.$order->getKey().'/dispute');
        $this->toProvider($order, NotificationCode::DisputeUpdated, $title, $body);
    }

    private function cancelled(Order $order, OrderEvent $event): void
    {
        $title = 'أُلغي الطلب';
        $body = 'أُلغي طلب '.$this->number($order).'.';

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
        $this->toCustomer($order, NotificationCode::OrderClosed, 'اكتمل الطلب', 'اكتمل طلب '.$this->number($order).'. يمكنك تقييم الفني.');
        $this->toProvider($order, NotificationCode::OrderClosed, 'اكتمل الطلب', 'اكتمل طلب '.$this->number($order).'.');
    }

    private function toCustomer(Order $order, NotificationCode $code, string $title, string $body, ?string $path = null): void
    {
        if ($order->customer === null) {
            return;
        }

        $this->notifier->send($order->customer, new NotificationContent(
            $code, $title, $body, $path ?? 'orders/'.$order->getKey(), self::CUSTOMER, $order->getKey(),
        ));
    }

    private function toProvider(Order $order, NotificationCode $code, string $title, string $body): void
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

        return rtrim(rtrim(number_format($value, 2, '.', ''), '0'), '.').' جنيه';
    }

    /** العدد بصيغة عربية سليمة: عرض، عرضان، 3–10 عروض، 11+ عرضًا. */
    private function offersPhrase(int $count): string
    {
        return match (true) {
            $count === 1 => 'وصل عرض جديد',
            $count === 2 => 'وصل عرضان جديدان',
            $count <= 10 => 'وصلت '.$count.' عروض جديدة',
            default => 'وصل '.$count.' عرضًا جديدًا',
        };
    }
}
