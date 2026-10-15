<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

final readonly class ResolveCustomerNoShowAction
{
    public function __construct(private CancelOrderAction $cancel) {}

    public function execute(Order $order, Admin $admin, bool $customerAbsent, string $contactNote): Order
    {
        $contactNote = trim($contactNote);

        if (! $admin->is_active || mb_strlen($contactNote) < 5 || mb_strlen($contactNote) > 500) {
            throw BusinessRuleViolationException::rule('BR-071', Localization::text('copy.admin_contact_result_required'));
        }

        return DB::transaction(function () use ($order, $admin, $customerAbsent, $contactNote): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());

            if (! $fresh->hasPendingNoShowReport()) {
                throw BusinessRuleViolationException::rule('BR-071', Localization::text('copy.no_pending_absence_report'));
            }

            OrderEvent::query()->create([
                'order_id' => $fresh->getKey(),
                'event_code' => OrderEventCode::CustomerNoShowResolved,
                'actor_type' => ActorType::Admin,
                'actor_id' => $admin->getKey(),
                'from_status' => $fresh->status,
                'to_status' => $fresh->status,
                'meta' => ['customer_absent' => $customerAbsent, 'contact_note' => $contactNote],
                'created_at' => now(),
            ]);

            $fresh->unsetRelation('latestNoShowEvent');

            if ($customerAbsent) {
                return $this->cancel->execute($fresh, ActorType::Admin, $admin->getKey(), CancelReason::CustomerNoShow, $contactNote);
            }

            return $fresh;
        });
    }
}
