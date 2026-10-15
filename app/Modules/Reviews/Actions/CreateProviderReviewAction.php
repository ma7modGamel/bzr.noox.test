<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\Review;
use App\Modules\Reviews\Models\ReviewTag;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

final readonly class CreateProviderReviewAction
{
    public function __construct(private SettingsRepository $settings) {}

    /** @param list<int> $mediaIds */
    public function execute(
        Order $order,
        User $customer,
        int $quality,
        int $punctuality,
        int $conduct,
        ?string $comment,
        array $mediaIds = [],
        array $tags = [],
    ): Review {
        return DB::transaction(function () use ($order, $customer, $quality, $punctuality, $conduct, $comment, $mediaIds, $tags): Review {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            $this->assertCanRate($fresh, $customer);
            $allowedTags = ReviewTag::query()->whereIn('code', $tags)->where('is_active', true)->lockForUpdate()->get(['code'])->count();
            if (count($tags) > 10 || count(array_unique($tags)) !== count($tags) || $allowedTags !== count($tags)) {
                throw ValidationException::withMessages(['tags' => Localization::text('copy.invalid_service_review_tags')]);
            }

            $photos = OrderMedia::query()->whereIn('id', $mediaIds)->orderBy('id')->lockForUpdate()->get();
            if (count($mediaIds) > 5 || $photos->count() !== count($mediaIds) || $photos->contains(
                fn (OrderMedia $media): bool => $media->uploaded_by !== $customer->getKey()
                    || $media->order_id !== null || $media->type !== 'IMAGE' || (bool) $media->expires_at?->isPast(),
            )) {
                throw BusinessRuleViolationException::rule('BR-014', Localization::text('copy.the_photo_is_invalid_or_does_not_belong_to'));
            }

            $review = Review::query()->create([
                'order_id' => $fresh->getKey(),
                'provider_profile_id' => $fresh->provider_profile_id,
                'customer_id' => $customer->getKey(),
                'quality' => $quality,
                'punctuality' => $punctuality,
                'conduct' => $conduct,
                'comment' => $comment,
                'tags' => array_values($tags),
            ]);

            $this->refreshProviderRating($fresh->providerProfile);

            foreach ($photos as $photo) {
                $photo->update(['order_id' => $fresh->getKey(), 'purpose' => 'CUSTOMER_REVIEW', 'expires_at' => null]);
            }

            return $review;
        });
    }

    private function assertCanRate(Order $order, User $customer): void
    {
        $deadline = $order->closed_at?->addDays($this->settings->int(Cfg::RatingWindowDays));

        if ($order->customer_id !== $customer->getKey()
            || $order->status !== OrderStatus::Closed
            || $order->provider_profile_id === null
            || $deadline === null
            || now()->isAfter($deadline)
            || $order->review()->exists()) {
            throw BusinessRuleViolationException::rule('BR-090', Localization::text('copy.rating_is_unavailable_for_this_request'));
        }
    }

    private function refreshProviderRating(ProviderProfile $provider): void
    {
        $provider = ProviderProfile::query()->lockForUpdate()->findOrFail($provider->getKey());
        $reviews = Review::query()->visible()->where('provider_profile_id', $provider->getKey())->get();

        $provider->forceFill([
            'rating_avg' => round((float) $reviews->avg(fn (Review $review) => $review->average()), 2),
            'rating_quality_avg' => round((float) $reviews->avg('quality'), 2),
            'rating_punctuality_avg' => round((float) $reviews->avg('punctuality'), 2),
            'rating_conduct_avg' => round((float) $reviews->avg('conduct'), 2),
            'ratings_count' => $reviews->count(),
        ])->save();
    }
}
