<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\TrackingPoint;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Geo\Distance;

/**
 * T-10 — تسجيل الوصول (10).
 *
 * الآثار الجانبية عند الدخول إلى ARRIVED (10 §آثار جانبية):
 * حفظ نقطة الوصول ومسافتها (BR-111)، حذف نقاط المسار (BR-112)، إغلاق العروض غير المختارة (O-06).
 */
final readonly class MarkArrivedAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private SettingsRepository $settings,
    ) {}

    /**
     * @param  bool  $confirmedFarArrival  تأكيد الفني بعد رؤية تحذير البعد (AC-EXE-03)
     */
    public function execute(
        Order $order,
        ProviderProfile $provider,
        float $lat,
        float $lng,
        bool $confirmedFarArrival = false,
    ): Order {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-022', 'هذا الطلب غير مسنَد إليك.');
        }

        $distance = Distance::meters((float) $order->lat, (float) $order->lng, $lat, $lng);
        $threshold = $this->settings->int(Cfg::ArrivalWarningDistanceMeters);

        // BR-111 — تحذير للفني قبل التأكيد، وعلامة للإدارة بعده
        if ($distance > $threshold && ! $confirmedFarArrival) {
            throw BusinessRuleViolationException::rule(
                'BR-111',
                "موقعك يبعد {$distance} مترًا عن عنوان الطلب (الحد {$threshold}). أكّد الوصول إن كنت في المكان الصحيح.",
            );
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'markArrived',
            actorType: ActorType::Provider,
            actorId: $provider->getKey(),
            meta: ['distance_m' => $distance, 'far_arrival' => $distance > $threshold],
            mutate: function (Order $fresh) use ($lat, $lng, $distance): void {
                $fresh->arrived_at = now();
                $fresh->arrived_lat = (string) $lat;
                $fresh->arrived_lng = (string) $lng;
                $fresh->arrival_distance_m = $distance;

                // BR-112 — نقاط المسار مؤقتة وتُحذف بانتهاء الحاجة إليها
                TrackingPoint::query()->where('order_id', $fresh->getKey())->delete();

                // O-06 — العروض غير المختارة تُغلق عند الوصول
                Offer::query()
                    ->where('order_id', $fresh->getKey())
                    ->whereIn('status', [OfferStatus::Submitted->value, OfferStatus::NotSelected->value])
                    ->update(['status' => OfferStatus::Closed->value, 'decided_at' => now()]);
            },
        );
    }
}
