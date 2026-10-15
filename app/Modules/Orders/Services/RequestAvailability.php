<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Database\Eloquent\Builder;

final readonly class RequestAvailability
{
    public function __construct(
        private SchedulingCalendar $calendar,
        private ProviderEligibility $eligibility,
        private FeatureGate $features,
    ) {}

    public function nowAvailable(User $customer, CustomerAddress $address, int $categoryId): bool
    {
        $address->loadMissing(['city', 'area']);

        if (! $address->city->is_active || ! $address->area->is_active
            || ! $this->calendar->isWithinServiceHours($address->city)) {
            return false;
        }

        $categoryAvailable = Category::query()->whereKey($categoryId)->where('is_active', true)
            ->whereHas('cities', fn (Builder $query): Builder => $query
                ->where('cities.id', $address->city_id)->where('city_categories.is_active', true))
            ->exists();

        if (! $categoryAvailable) {
            return false;
        }

        if ($this->features->offersEnabled()) {
            return true;
        }

        $draft = new Order([
            'customer_id' => $customer->getKey(),
            'category_id' => $categoryId,
            'area_id' => $address->area_id,
            'timing_type' => TimingType::Now,
        ]);

        return $this->eligibility->query($draft)->exists();
    }

    public function assertNowAvailable(User $customer, CustomerAddress $address, int $categoryId): void
    {
        if (! $this->nowAvailable($customer, $address, $categoryId)) {
            throw BusinessRuleViolationException::rule('BR-017', Localization::text('copy.no_employee_available_now'));
        }
    }
}
