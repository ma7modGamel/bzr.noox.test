<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

final class UpdateCompletionEstimateAction
{
    public function execute(Order $order, ProviderProfile $provider, ?int $minutes, int $expectedVersion): Order
    {
        return DB::transaction(function () use ($order, $provider, $minutes, $expectedVersion): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            abort_if($fresh->version !== $expectedVersion, 409, Localization::text('copy.this_request_has_changed_since_your_last_update_reload'));
            if ($fresh->provider_profile_id !== $provider->getKey() || $fresh->status !== OrderStatus::InProgress || $fresh->hasPendingNoShowReport()) {
                throw BusinessRuleViolationException::rule('BR-022', Localization::text('copy.completion_estimate_not_allowed'));
            }
            $fresh->forceFill(['estimated_completion_at' => $minutes === null ? null : now()->addMinutes($minutes),
                'completion_estimate_updated_at' => now(), 'version' => $fresh->version + 1])->save();
            OrderEvent::query()->create(['order_id' => $fresh->getKey(), 'event_code' => OrderEventCode::CompletionEstimateUpdated,
                'actor_type' => ActorType::Provider, 'actor_id' => $provider->getKey(), 'from_status' => $fresh->status,
                'to_status' => $fresh->status, 'meta' => ['estimated_at' => $fresh->estimated_completion_at?->toIso8601String()], 'created_at' => now()]);

            return $fresh;
        });
    }
}
