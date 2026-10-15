<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Orders\Models\Order;
use App\Modules\Reviews\Actions\CreateCustomerRatingAction;
use App\Modules\Reviews\Actions\CreateProviderReviewAction;
use App\Modules\Reviews\Enums\CustomerRatingTag;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

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
            'tags' => ['sometimes', 'array', 'max:10'],
            'tags.*' => ['required', 'string', 'max:48', 'distinct', Rule::exists('review_tags', 'code')->where('is_active', true)],
            'media_ids' => ['sometimes', 'array', 'max:5'],
            'media_ids.*' => ['integer', 'distinct'],
        ]);

        $review = $action->execute(
            $order,
            $request->user(),
            (int) $data['quality'],
            (int) $data['punctuality'],
            (int) $data['conduct'],
            $data['comment'] ?? null,
            $data['media_ids'] ?? [],
            $data['tags'] ?? [],
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
            throw BusinessRuleViolationException::rule('BR-091', Localization::text('copy.there_is_no_technician_profile_for_this_account'));
        }

        $data = $request->validate([
            'stars' => ['required', 'integer', 'between:1,5'],
            'tags' => ['sometimes', 'array', 'max:6'],
            'tags.*' => ['required', 'string', 'distinct', Rule::enum(CustomerRatingTag::class)],
            'comment' => ['nullable', 'string', 'max:500'],
        ]);
        $rating = $action->execute($order, $provider, (int) $data['stars'], $data['tags'] ?? [], $data['comment'] ?? null);

        return new JsonResponse(['data' => [
            'id' => $rating->getKey(),
            'stars' => $rating->stars,
            'tags' => $rating->tags,
            'comment' => $rating->comment,
        ]], 201);
    }
}
