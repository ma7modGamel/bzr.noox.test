<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\TrackingPoint;
use App\Modules\Orders\Services\OrderEtaService;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;

/**
 * T-05 — بدء التحرك (10).
 * للطلب المجدول: لا يبدأ التحرك قبل CFG-022 من بداية الفترة (AC-EXE-02).
 * صلاحية الموقع شرط في الواجهة (EC-21)؛ الخادم يتحقق من وصول الموقع مع الطلب.
 */
final readonly class StartTripAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private SettingsRepository $settings,
        private OrderEtaService $eta,
    ) {}

    public function execute(Order $order, ProviderProfile $provider, ?float $lat = null, ?float $lng = null): Order
    {
        $this->assertOwnership($order, $provider);
        $this->assertScheduleWindow($order);

        $order = $this->stateMachine->apply(
            order: $order,
            action: 'startTrip',
            actorType: ActorType::Provider,
            actorId: $provider->getKey(),
            mutate: fn (Order $fresh) => $fresh->trip_started_at = now(),
        );

        if ($lat !== null && $lng !== null) {
            TrackingPoint::query()->create([
                'order_id' => $order->getKey(),
                'lat' => $lat,
                'lng' => $lng,
                'recorded_at' => now(),
            ]);
            $this->eta->refreshIfDue($order, $lat, $lng, force: true);
        }

        return $order;
    }

    private function assertOwnership(Order $order, ProviderProfile $provider): void
    {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-022', 'هذا الطلب غير مسنَد إليك.');
        }
    }

    /** BR-017/AC-EXE-02 — التحرك المبكر جدًا للطلب المجدول مرفوض. */
    private function assertScheduleWindow(Order $order): void
    {
        if ($order->timing_type !== TimingType::Scheduled || $order->slot_start === null) {
            return;
        }

        $earliest = $order->slot_start->subMinutes($this->settings->int(Cfg::MinLeadMinutesBeforeSlot));

        if (CarbonImmutable::now()->lt($earliest)) {
            throw BusinessRuleViolationException::rule(
                'BR-017',
                'بدء التحرك متاح قبل بداية الفترة بـ '
                    .$this->settings->int(Cfg::MinLeadMinutesBeforeSlot).' دقيقة على الأكثر.',
            );
        }
    }
}
