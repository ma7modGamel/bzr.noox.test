<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Orders\Models\Order;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;

final class TrackingController
{
    public function show(Request $request, Order $order): JsonResponse
    {
        Gate::authorize('track', $order);

        $order->loadMissing('latestTrackingPoint');
        $point = $order->latestTrackingPoint;

        return new JsonResponse([
            'last_location' => $point === null ? null : [
                'lat' => $point->lat,
                'lng' => $point->lng,
                'recorded_at' => $point->recorded_at?->toIso8601String(),
            ],
            'eta_minutes' => $order->eta_minutes,
            'eta_approximate' => $order->eta_approximate,
            'eta_calculated_at' => $order->eta_calculated_at?->toIso8601String(),
        ]);
    }
}
