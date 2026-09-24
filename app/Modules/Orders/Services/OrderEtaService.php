<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Orders\Contracts\RouteEtaProvider;
use App\Modules\Orders\Data\EtaEstimate;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Geo\Distance;
use Illuminate\Http\Client\ConnectionException;
use Illuminate\Http\Client\RequestException;
use RuntimeException;

/** CFG-081 — التخزين المؤقت، شروط إعادة الحساب، والمسار التقريبي الاحتياطي. */
final readonly class OrderEtaService
{
    private const FALLBACK_SPEED_METERS_PER_MINUTE = 20_000 / 60;

    public function __construct(
        private RouteEtaProvider $routes,
        private SettingsRepository $settings,
    ) {}

    public function refreshIfDue(Order $order, float $lat, float $lng, bool $force = false): bool
    {
        if (! $force && ! $this->isDue($order, $lat, $lng)) {
            return false;
        }

        $estimate = $this->estimate($lat, $lng, (float) $order->lat, (float) $order->lng);

        $order->forceFill([
            'eta_minutes' => $estimate->minutes,
            'eta_approximate' => $estimate->approximate,
            'eta_calculated_at' => now(),
            'eta_origin_lat' => $lat,
            'eta_origin_lng' => $lng,
        ])->save();

        return true;
    }

    private function isDue(Order $order, float $lat, float $lng): bool
    {
        if ($order->eta_calculated_at === null || $order->eta_origin_lat === null || $order->eta_origin_lng === null) {
            return true;
        }

        if ($order->eta_calculated_at->lte(now()->subSeconds($this->settings->int(Cfg::EtaRecalcSeconds)))) {
            return true;
        }

        return Distance::meters(
            (float) $order->eta_origin_lat,
            (float) $order->eta_origin_lng,
            $lat,
            $lng,
        ) > $this->settings->int(Cfg::EtaRecalcDistanceMeters);
    }

    private function estimate(float $originLat, float $originLng, float $destinationLat, float $destinationLng): EtaEstimate
    {
        try {
            return $this->routes->estimate($originLat, $originLng, $destinationLat, $destinationLng);
        } catch (ConnectionException|RequestException|RuntimeException) {
            $distance = Distance::meters($originLat, $originLng, $destinationLat, $destinationLng);

            return new EtaEstimate(
                minutes: (int) ceil($distance / self::FALLBACK_SPEED_METERS_PER_MINUTE),
                approximate: true,
            );
        }
    }
}
