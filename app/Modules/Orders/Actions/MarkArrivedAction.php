<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\TrackingPoint;
use App\Modules\Orders\Services\ArrivalVerification;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Geo\Distance;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

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
        private ArrivalVerification $verification,
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
        ?string $arrivalCode = null,
        ?int $expectedVersion = null,
        ?Admin $exceptionAdmin = null,
        ?string $exceptionNote = null,
    ): Order {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-022', Localization::text('copy.this_request_is_not_assigned_to_you'));
        }

        $distance = Distance::meters((float) $order->lat, (float) $order->lng, $lat, $lng);
        $threshold = $this->settings->int(Cfg::ArrivalWarningDistanceMeters);

        // BR-111 — تحذير للفني قبل التأكيد، وعلامة للإدارة بعده
        if ($distance > $threshold && ! $confirmedFarArrival) {
            throw BusinessRuleViolationException::rule(
                'BR-111',
                Localization::text('dynamic.arrival_distance', ['distance' => $distance, 'threshold' => $threshold]),
            );
        }

        $result = DB::transaction(function () use ($order, $provider, $lat, $lng, $distance, $threshold, $arrivalCode, $expectedVersion, $exceptionAdmin, $exceptionNote): Order|string {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->id);
            abort_unless($fresh->provider_profile_id === $provider->id, 404);
            abort_if($expectedVersion !== null && $fresh->version !== $expectedVersion, 409);
            if ($fresh->status !== OrderStatus::OnTheWay) {
                throw BusinessRuleViolationException::rule('ARRIVAL-CODE', Localization::text('copy.arrival_code_unavailable'));
            }
            if ($exceptionAdmin !== null) {
                abort_unless($exceptionAdmin->is_active && in_array($exceptionAdmin->role->value, ['SUPER', 'OPERATIONS'], true), 403);
                $report = $fresh->arrival_exception_report;
                if (! $this->verification->required($fresh) || $report === null || ($report['provider_id'] ?? null) !== $provider->id
                    || mb_strlen(trim((string) $exceptionNote)) < 5 || mb_strlen((string) $exceptionNote) > 500) {
                    throw BusinessRuleViolationException::rule('ARRIVAL-CODE', Localization::text('copy.arrival_exception_note_required'));
                }
            } elseif (($error = $this->verification->attempt($fresh, $arrivalCode)) !== null) {
                return $error;
            }

            $verifiesArrival = $exceptionAdmin !== null || $this->verification->required($fresh);

            return $this->stateMachine->apply(
                order: $fresh,
                action: $exceptionAdmin === null ? 'markArrived' : 'confirmArrivalException',
                actorType: $exceptionAdmin === null ? ActorType::Provider : ActorType::Admin,
                actorId: $exceptionAdmin?->id ?? $provider->getKey(),
                meta: ['distance_m' => $distance, 'far_arrival' => $distance > $threshold,
                    'arrival_verification' => $exceptionAdmin !== null ? 'ADMIN_EXCEPTION' : ($verifiesArrival ? 'CUSTOMER_CODE' : 'LOCATION_ONLY'), 'contact_note' => $exceptionNote],
                mutate: function (Order $fresh) use ($lat, $lng, $distance, $verifiesArrival): void {
                    $fresh->arrival_verified_at = $verifiesArrival ? now() : null;
                    $fresh->arrival_code = null;
                    $fresh->arrival_code_expires_at = null;
                    $fresh->arrival_exception_report = null;
                    $fresh->arrived_at = now();
                    $fresh->arrived_lat = (string) $lat;
                    $fresh->arrived_lng = (string) $lng;
                    $fresh->arrival_distance_m = $distance;
                    TrackingPoint::query()->where('order_id', $fresh->getKey())->delete();
                    Offer::query()->where('order_id', $fresh->getKey())
                        ->whereIn('status', [OfferStatus::Submitted->value, OfferStatus::NotSelected->value])
                        ->update(['status' => OfferStatus::Closed->value, 'decided_at' => now()]);
                },
            );
        });
        if (is_string($result)) {
            throw BusinessRuleViolationException::rule('ARRIVAL-CODE', $result);
        }

        return $result;
    }
}
