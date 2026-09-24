<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\Review;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Facades\DB;

final readonly class CreateProviderReviewAction
{
    public function __construct(private SettingsRepository $settings) {}

    public function execute(
        Order $order,
        User $customer,
        int $quality,
        int $punctuality,
        int $conduct,
        ?string $comment,
    ): Review {
        return DB::transaction(function () use ($order, $customer, $quality, $punctuality, $conduct, $comment): Review {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            $this->assertCanRate($fresh, $customer);

            $review = Review::query()->create([
                'order_id' => $fresh->getKey(),
                'provider_profile_id' => $fresh->provider_profile_id,
                'customer_id' => $customer->getKey(),
                'quality' => $quality,
                'punctuality' => $punctuality,
                'conduct' => $conduct,
                'comment' => $comment,
            ]);

            $this->refreshProviderRating($fresh->providerProfile);

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
            throw BusinessRuleViolationException::rule('BR-090', 'التقييم غير متاح لهذا الطلب.');
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
