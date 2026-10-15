<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Pricing\Enums\ProposalType;
use App\Support\Localization;

final class OrderExecutionPresentation
{
    /** @return list<array{key: string, label: string, state: string, occurred_at: ?string}> */
    public function steps(Order $order): array
    {
        if ($order->work_started_at === null && $order->status !== OrderStatus::InProgress) {
            return [];
        }
        $order->loadMissing('events');
        $events = $order->events->filter(fn (OrderEvent $event): bool => $order->confirmed_at === null || $event->created_at->gte($order->confirmed_at));
        $quote = $events->last(fn (OrderEvent $event): bool => $event->event_code === OrderEventCode::ProposalSubmitted && ($event->meta['type'] ?? null) === ProposalType::ExecutionQuote->value);
        $approval = $quote === null ? null : $events->last(fn (OrderEvent $event): bool => $event->event_code === OrderEventCode::ProposalApproved && $event->ref_type === 'proposal' && $event->ref_id === $quote->ref_id);
        $steps = [['arrived', $order->arrived_at, $order->arrived_at === null ? 'pending' : 'done']];
        if (! $order->isWarrantyVisit() && $quote !== null) {
            $steps[] = ['inspected', $quote->created_at, 'done'];
            $steps[] = ['approved', $approval?->created_at, $approval === null ? 'pending' : 'done'];
        }
        $steps[] = ['repairing', $order->work_started_at, $order->completed_at === null ? 'active' : 'done'];
        $steps[] = ['completed', $order->completed_at, $order->completed_at === null ? 'pending' : 'done'];

        return array_map(fn (array $step): array => ['key' => $step[0], 'label' => Localization::text('copy.execution_step_'.$step[0]),
            'state' => $step[2], 'occurred_at' => $step[1]?->toIso8601String()], $steps);
    }

    /** @return array{estimated_at: string, updated_at: ?string, overdue: bool}|null */
    public function estimate(Order $order): ?array
    {
        if ($order->status !== OrderStatus::InProgress || $order->estimated_completion_at === null) {
            return null;
        }

        return ['estimated_at' => $order->estimated_completion_at->toIso8601String(),
            'updated_at' => $order->completion_estimate_updated_at?->toIso8601String(), 'overdue' => $order->estimated_completion_at->isPast()];
    }
}
