<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Orders\Contracts\RouteEtaProvider;
use App\Modules\Orders\Data\EtaEstimate;
use Illuminate\Support\Facades\Http;
use RuntimeException;

/** CFG-081 — حساب وقت القيادة من Google يتم من الخادم فقط. */
final readonly class GoogleRoutesEtaProvider implements RouteEtaProvider
{
    public function estimate(float $originLat, float $originLng, float $destinationLat, float $destinationLng): EtaEstimate
    {
        $key = (string) config('services.google.routes_key');

        if ($key === '') {
            throw new RuntimeException('Google Routes API key is not configured.');
        }

        $response = Http::acceptJson()
            ->withHeaders([
                'X-Goog-Api-Key' => $key,
                'X-Goog-FieldMask' => 'routes.duration',
            ])
            ->connectTimeout(2)
            ->timeout(5)
            ->post((string) config('services.google.routes_url'), [
                'origin' => ['location' => ['latLng' => ['latitude' => $originLat, 'longitude' => $originLng]]],
                'destination' => ['location' => ['latLng' => ['latitude' => $destinationLat, 'longitude' => $destinationLng]]],
                'travelMode' => 'DRIVE',
                'routingPreference' => 'TRAFFIC_AWARE',
                'computeAlternativeRoutes' => false,
                'units' => 'METRIC',
            ])
            ->throw();

        $duration = $response->json('routes.0.duration');

        if (! is_string($duration) || preg_match('/^(\d+(?:\.\d+)?)s$/', $duration, $matches) !== 1) {
            throw new RuntimeException('Google Routes API returned no usable duration.');
        }

        return new EtaEstimate((int) ceil((float) $matches[1] / 60));
    }
}
