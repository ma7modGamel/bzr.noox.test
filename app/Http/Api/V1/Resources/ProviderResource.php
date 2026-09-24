<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** @mixin ProviderProfile */
final class ProviderResource extends JsonResource
{
    /** @param list<string> $availableActions */
    public function __construct($resource, private readonly array $availableActions = [])
    {
        parent::__construct($resource);
    }

    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        return [
            'id' => $this->id,
            'name' => $this->user->name,
            'avatar_path' => $this->user->avatar_path,
            'bio' => $this->bio,
            'is_verified' => $this->isVerified(),
            'available_now' => $this->available_now,
            'experience_years' => $this->experience_years,
            'rating_avg' => $this->rating_avg,
            'ratings_count' => $this->ratings_count,
            'completed_orders' => $this->completed_orders_count,
            'rating_breakdown' => [
                'quality' => $this->rating_quality_avg,
                'punctuality' => $this->rating_punctuality_avg,
                'conduct' => $this->rating_conduct_avg,
            ],
            'categories' => $this->categories->map->only(['id', 'name'])->values(),
            'specialties' => $this->specialties->map->only(['id', 'name'])->values(),
            'portfolio' => $this->portfolioItems->map->only(['id', 'image_path', 'caption'])->values(),
            'reviews' => $this->reviews->map(fn ($review): array => [
                'id' => $review->id,
                'customer_name' => $review->customer->shortName(),
                'quality' => $review->quality,
                'punctuality' => $review->punctuality,
                'conduct' => $review->conduct,
                'comment' => $review->comment,
                'created_at' => $review->created_at?->toIso8601String(),
            ])->values(),
            'available_actions' => $this->availableActions,
        ];
    }
}
