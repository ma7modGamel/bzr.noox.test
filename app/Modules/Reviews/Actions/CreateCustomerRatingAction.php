<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\CustomerRating;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Facades\DB;

final readonly class CreateCustomerRatingAction
{
    public function __construct(private SettingsRepository $settings) {}

    public function execute(Order $order, ProviderProfile $provider, int $stars): CustomerRating
    {
        return DB::transaction(function () use ($order, $provider, $stars): CustomerRating {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            $deadline = $fresh->closed_at?->addDays($this->settings->int(Cfg::RatingWindowDays));

            if ($fresh->provider_profile_id !== $provider->getKey()
                || $fresh->status !== OrderStatus::Closed
                || $deadline === null
                || now()->isAfter($deadline)
                || $fresh->customerRating()->exists()) {
                throw BusinessRuleViolationException::rule('BR-091', 'التقييم غير متاح لهذا الطلب.');
            }

            $rating = CustomerRating::query()->create([
                'order_id' => $fresh->getKey(),
                'customer_id' => $fresh->customer_id,
                'provider_profile_id' => $provider->getKey(),
                'stars' => $stars,
            ]);

            $ratings = CustomerRating::query()
                ->where('customer_id', $fresh->customer_id)
                ->whereNull('hidden_at')
                ->get();

            $customer = User::query()->lockForUpdate()->findOrFail($fresh->customer_id);
            $customer->forceFill([
                'customer_rating_avg' => round((float) $ratings->avg('stars'), 2),
                'customer_rating_count' => $ratings->count(),
            ])->save();

            return $rating;
        });
    }
}
