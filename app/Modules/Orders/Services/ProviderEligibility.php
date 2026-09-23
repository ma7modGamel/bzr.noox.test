<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Database\Eloquent\Builder;

/**
 * أهلية مقدم الخدمة — BR-022.
 *
 * في وضع الموظفين تُطبَّق البنود 1، 2، 3، 5، 6 فقط؛ البند 4 (المنع بسبب المستحقات)
 * خاص بالعروض ولا يُطبق (BR-065). وشرط الموعد يُفحص عند التعيين لا عند العرض (08).
 */
final class ProviderEligibility
{
    public function __construct(private readonly FeatureGate $features) {}

    /** استعلام المؤهلين لطلب — يخدم التوزيع (وضع السوق) ولوحة التعيين (وضع الموظفين). */
    public function query(Order $order): Builder
    {
        $query = ProviderProfile::query()
            ->with('user')
            ->where('status', ProviderStatus::Active->value)
            ->whereHas('user', fn (Builder $q) => $q->where('status', 'ACTIVE'))
            ->whereHas('categories', fn (Builder $q) => $q->where('categories.id', $order->category_id))
            ->whereHas('areas', fn (Builder $q) => $q->where('areas.id', $order->area_id))
            ->where('user_id', '!=', $order->customer_id); // البند 5 — لا يقدم على طلبه

        // البند 4 — المنع بسبب المستحقات: وضع السوق فقط (BR-063، BR-065)
        if ($this->features->offersEnabled()) {
            $query->whereNull('dues_blocked_at');
        }

        // البند 6 — طلب NOW: متاح الآن ولا طلب NOW نشط
        if ($order->timing_type === TimingType::Now) {
            $query->where('available_now', true)
                ->whereDoesntHave('orders', fn (Builder $q) => $q
                    ->where('timing_type', TimingType::Now->value)
                    ->whereNotIn('status', $this->finalStatuses()));
        } elseif ($order->slot_start !== null && $order->slot_end !== null) {
            // لا فترات مجدولة متداخلة (ASM-10، BR-034)
            $query->whereDoesntHave('orders', fn (Builder $q) => $q
                ->whereNotIn('status', $this->finalStatuses())
                ->where('slot_start', '<', $order->slot_end)
                ->where('slot_end', '>', $order->slot_start));
        }

        return $query;
    }

    public function isEligible(Order $order, ProviderProfile $provider): bool
    {
        return $this->query($order)->whereKey($provider->getKey())->exists();
    }

    public function assertAssignable(Order $order, ProviderProfile $provider): void
    {
        if (! $this->isEligible($order, $provider)) {
            throw BusinessRuleViolationException::rule(
                'BR-007',
                'مقدم الخدمة غير مؤهل لهذا الطلب: راجع الفئة والمنطقة والتوفر وتعارض المواعيد.',
            );
        }
    }

    /** @return list<string> */
    private function finalStatuses(): array
    {
        return [
            OrderStatus::Closed->value,
            OrderStatus::Cancelled->value,
            OrderStatus::Expired->value,
        ];
    }
}
