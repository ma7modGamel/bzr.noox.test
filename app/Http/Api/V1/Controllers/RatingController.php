<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Orders\Models\Order;
use App\Modules\Reviews\Actions\CreateCustomerRatingAction;
use App\Modules\Reviews\Actions\CreateProviderReviewAction;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

final class RatingController
{
    public function review(Request $request, Order $order, CreateProviderReviewAction $action): JsonResponse
    {
        abort_unless($order->customer_id === $request->user()->getKey(), 404);

        $data = $request->validate([
            'quality' => ['required', 'integer', 'between:1,5'],
            'punctuality' => ['required', 'integer', 'between:1,5'],
            'conduct' => ['required', 'integer', 'between:1,5'],
            'comment' => ['nullable', 'string', 'max:500'],
        ]);

        $review = $action->execute(
            $order,
            $request->user(),
            (int) $data['quality'],
            (int) $data['punctuality'],
            (int) $data['conduct'],
            $data['comment'] ?? null,
        );

        return new JsonResponse(['data' => [
            'id' => $review->getKey(),
            'average' => $review->average(),
        ]], 201);
    }

    public function customerRating(Request $request, Order $order, CreateCustomerRatingAction $action): JsonResponse
    {
        $provider = $request->user()->providerProfile;

        if ($provider === null) {
            throw BusinessRuleViolationException::rule('BR-091', 'لا يوجد ملف فني لهذا الحساب.');
        }

        $data = $request->validate(['stars' => ['required', 'integer', 'between:1,5']]);
        $rating = $action->execute($order, $provider, (int) $data['stars']);

        return new JsonResponse(['data' => [
            'id' => $rating->getKey(),
            'stars' => $rating->stars,
        ]], 201);
    }
}
