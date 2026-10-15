<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\ReviewTag;
use App\Support\Localization;
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
            'avatar_url' => $this->user->avatar_path === null ? null : route('api.providers.image', ['provider' => $this->id, 'image' => 'avatar', 'order_id' => $request->integer('order_id')]),
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
            'categories' => $this->categories->map(fn ($category): array => ['id' => $category->id, 'name' => Localization::field($category)])->values(),
            'specialties' => $this->specialties->map(fn ($specialty): array => ['id' => $specialty->id, 'name' => Localization::field($specialty)])->values(),
            'portfolio' => $this->portfolioItems->map(fn ($item): array => [
                'id' => $item->id, 'caption' => $item->caption, 'image_path' => $item->image_path,
                'image_url' => route('api.providers.image', ['provider' => $this->id, 'image' => $item->id, 'order_id' => $request->integer('order_id')]),
            ])->values(),
            'tag_statistics' => ReviewTag::statisticsForProvider($this->id),
            'reviews' => $this->reviews->map(fn ($review): array => [
                'id' => $review->id,
                'customer_name' => $review->customer->shortName(),
                'quality' => $review->quality,
                'punctuality' => $review->punctuality,
                'conduct' => $review->conduct,
                'comment' => $review->comment,
                'tags' => $review->tags ?? [],
                'area' => Localization::field($review->order?->area),
                'photos' => $review->photos->where('uploaded_by', $review->customer_id)->map(fn ($media): array => ['id' => $media->id,
                    'content_url' => route('api.providers.review-image', ['provider' => $this->id, 'review' => $review->id, 'media' => $media->id, 'order_id' => $request->integer('order_id')])])->values(),
                'created_at' => $review->created_at?->toIso8601String(),
            ])->values(),
            'available_actions' => $this->availableActions,
        ];
    }
}
