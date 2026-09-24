<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * T-11 — بدء التنفيذ مباشرة بعد الوصول.
 * لطلب التنفيذ فقط؛ طلب المعاينة يمر بعرض تنفيذ (T-12) أو ينتهي بلا تنفيذ (T-13/T-28).
 */
final readonly class StartWorkAction
{
    public function __construct(private OrderStateMachine $stateMachine) {}

    public function execute(Order $order, ProviderProfile $provider): Order
    {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-022', 'هذا الطلب غير مسنَد إليك.');
        }

        if ($order->pricing_mode !== PricingMode::Execution) {
            throw BusinessRuleViolationException::rule(
                'BR-042',
                'طلب المعاينة يبدأ التنفيذ بعد موافقة العميل على عرض التنفيذ.',
            );
        }

        return $this->stateMachine->apply(
            order: $order,
            action: 'startWork',
            actorType: ActorType::Provider,
            actorId: $provider->getKey(),
            mutate: fn (Order $fresh) => $fresh->work_started_at = now(),
        );
    }
}
