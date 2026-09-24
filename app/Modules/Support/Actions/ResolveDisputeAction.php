<?php

declare(strict_types=1);

namespace App\Modules\Support\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderClosure;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * T-23 / T-24 — قرار النزاع (16 §قرار النزاع). كل قرار يُسجَّل باسم المسؤول (AC-ADM-03).
 * الاسترداد نفسه من صلاحية المدير العام ويُسجَّل مستقلًا (DEC-019).
 */
final readonly class ResolveDisputeAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderClosure $closure,
        private OrderTermination $termination,
    ) {}

    public function execute(
        Order $order,
        Dispute $dispute,
        Admin $admin,
        DisputeResolution $resolution,
        string $note,
    ): Order {
        if ($dispute->order_id !== $order->getKey() || $dispute->status !== DisputeStatus::Open) {
            throw BusinessRuleViolationException::rule('BR-120', 'النزاع غير مفتوح على هذا الطلب.');
        }

        if (mb_strlen(trim($note)) < 5) {
            throw BusinessRuleViolationException::rule('BR-120', 'ملاحظة القرار إلزامية.');
        }

        $cancels = $resolution === DisputeResolution::CancelFullRefund;

        return $this->stateMachine->apply(
            order: $order,
            action: $cancels ? 'resolveDisputeCancel' : 'resolveDisputeClose',
            actorType: ActorType::Admin,
            actorId: $admin->getKey(),
            meta: ['resolution' => $resolution->value, 'note' => $note],
            mutate: function (Order $fresh) use ($dispute, $admin, $resolution, $note, $cancels): void {
                $dispute->forceFill([
                    'status' => DisputeStatus::Resolved,
                    'resolution' => $resolution,
                    'resolution_note' => $note,
                    'resolved_by' => $admin->getKey(),
                    'resolved_at' => now(),
                ])->save();

                if ($cancels) {
                    $fresh->cancelled_at = now();
                    $fresh->cancelled_by_type = ActorType::Admin->value;
                    $fresh->cancelled_by_id = $admin->getKey();
                    $fresh->cancel_note = $note;
                    $this->termination->apply($fresh);

                    return;
                }

                $this->closure->apply(
                    $fresh,
                    withCommission: $resolution !== DisputeResolution::CloseWithoutPayment,
                );
            },
            refType: 'dispute',
            refId: $dispute->getKey(),
        );
    }
}
