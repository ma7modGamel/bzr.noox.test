<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\OrderResource;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ArrivalVerification;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;

final class ArrivalVerificationController
{
    public function issue(Request $request, Order $order, ArrivalVerification $verification): JsonResponse
    {
        $data = $request->validate(['refresh' => ['sometimes', 'boolean']]);

        return new JsonResponse(['data' => $verification->issue($order, $request->user(), (bool) ($data['refresh'] ?? false))], headers: ['Cache-Control' => 'no-store, private']);
    }

    public function report(Request $request, Order $order, ArrivalVerification $verification): OrderResource
    {
        Gate::authorize('perform', $order);
        $data = $request->validate(['lat' => ['required', 'numeric', 'between:-90,90'], 'lng' => ['required', 'numeric', 'between:-180,180'],
            'note' => ['required', 'string', 'min:5', 'max:500'], 'expected_version' => ['required', 'integer', 'min:1']]);

        return new OrderResource($verification->report($order, $request->user()->providerProfile, (float) $data['lat'], (float) $data['lng'], trim($data['note']), $data['expected_version']));
    }
}
