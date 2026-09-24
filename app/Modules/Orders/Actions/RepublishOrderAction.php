<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Services\OrderDeadlines;
use App\Modules\Settings\Enums\OperatingMode;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;

final readonly class RepublishOrderAction
{
    public function __construct(private OrderDeadlines $deadlines) {}

    public function execute(Order $order, User $customer): Order
    {
        if (! $this->canExecute($order, $customer)) {
            throw BusinessRuleViolationException::rule('BR-021', 'إعادة نشر الطلب غير متاحة في حالته الحالية.');
        }

        $publishedAt = CarbonImmutable::now();
        $deadlines = $this->deadlines->for(
            $order->operating_mode,
            $order->timing_type,
            $order->slot_start,
            $publishedAt,
        );

        return DB::transaction(function () use ($order, $customer, $publishedAt, $deadlines): Order {
            $target = $order->status === OrderStatus::Expired
                ? $this->copyExpiredOrder($order)
                : $order;

            $target->forceFill([
                'status' => OrderStatus::Open,
                'offers_close_at' => $deadlines['offers_close_at'],
                'selection_deadline_at' => $deadlines['selection_deadline_at'],
                'expired_at' => null,
                'reopen_count' => $order->reopen_count + 1,
            ])->save();

            OrderEvent::query()->create([
                'order_id' => $target->getKey(),
                'event_code' => OrderEventCode::Republished,
                'actor_type' => ActorType::Customer,
                'actor_id' => $customer->getKey(),
                'from_status' => $target === $order ? OrderStatus::Open : OrderStatus::Expired,
                'to_status' => OrderStatus::Open,
                'ref_type' => $target === $order ? null : Order::class,
                'ref_id' => $target === $order ? null : $order->getKey(),
                'created_at' => $publishedAt,
            ]);

            return $target->refresh();
        });
    }

    public function canExecute(Order $order, User $customer): bool
    {
        if ($order->customer_id !== $customer->getKey()
            || $order->operating_mode !== OperatingMode::Marketplace
            || $order->offers()->where('status', 'SUBMITTED')->exists()) {
            return false;
        }

        return $order->status === OrderStatus::Expired
            || ($order->status === OrderStatus::Open && $order->offers_close_at?->isPast());
    }

    private function copyExpiredOrder(Order $order): Order
    {
        $copy = $order->replicate([
            'provider_profile_id', 'assigned_by_admin_id', 'accepted_offer_id', 'payment_method',
            'payment_status', 'commission_rate', 'labor_total', 'materials_total', 'final_amount',
            'commission_amount', 'labor_refunded', 'refunded_total', 'confirmed_at', 'trip_started_at',
            'arrived_at', 'work_started_at', 'completed_at', 'closed_at', 'cancelled_at', 'expired_at',
            'cancel_reason_code', 'cancel_note', 'disputed_from_status', 'version',
        ]);
        $copy->number = DB::table('order_numbers')->insertGetId([]);
        $copy->republished_from_id = $order->getKey();
        $copy->save();

        foreach ($order->media as $media) {
            $mediaCopy = $media->replicate();
            $mediaCopy->order_id = $copy->getKey();
            $mediaCopy->save();
        }

        return $copy;
    }
}
