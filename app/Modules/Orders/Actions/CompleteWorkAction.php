<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

/** T-17 / T-31 — الصور إلزامية، وزيارة الضمان مجانية عدا الخامات الموافق عليها. */
final readonly class CompleteWorkAction
{
    public function __construct(private OrderStateMachine $stateMachine, private OrderClosure $closure) {}

    public function execute(Order $order, ProviderProfile $provider): Order
    {
        return DB::transaction(function () use ($order, $provider): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            if ($fresh->provider_profile_id !== $provider->getKey()) {
                throw BusinessRuleViolationException::rule('BR-022', Localization::text('copy.this_request_is_not_assigned_to_you'));
            }
            if ($fresh->hasPendingProposal()) {
                throw BusinessRuleViolationException::rule('BR-045', Localization::text('copy.work_cannot_be_completed_while_a_price_proposal_is'));
            }
            $amounts = OrderAmounts::for($fresh);
            $freeWarranty = $fresh->isWarrantyVisit() && $amounts->isZero();
            if ($amounts->isZero() && ! $freeWarranty) {
                throw BusinessRuleViolationException::rule('BR-044', Localization::text('copy.a_request_cannot_be_completed_without_an_amount_due'));
            }

            return $this->stateMachine->apply(
                order: $fresh, action: $freeWarranty ? 'completeFreeWarrantyVisit' : 'completeWork',
                actorType: ActorType::Provider, actorId: $provider->getKey(), meta: $amounts->toColumns(),
                mutate: function (Order $locked) use ($amounts, $freeWarranty, $provider): void {
                    $photos = $locked->media()->where('uploaded_by', $provider->user_id)->where('type', 'IMAGE');
                    if ((clone $photos)->where('purpose', 'REPAIR_BEFORE')->doesntExist()
                        || (clone $photos)->where('purpose', 'REPAIR_AFTER')->doesntExist()) {
                        throw BusinessRuleViolationException::rule('BR-014', Localization::text('copy.repair_photos_required'));
                    }
                    $locked->fill($amounts->toColumns());
                    $locked->completed_at = now();
                    if ($freeWarranty) {
                        $locked->payment_status = OrderPaymentStatus::Waived;
                        $this->closure->apply($locked, withCommission: false);
                    }
                },
            );
        });
    }
}
