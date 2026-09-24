<?php

declare(strict_types=1);

namespace App\Modules\Support\Actions;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Illuminate\Support\Facades\DB;

/**
 * T-22 — فتح مشكلة. BR-120:
 * قبل الإغلاق ← الطلب `DISPUTED`. بعد الإغلاق ← ملف نزاع يوقف التسوية دون تغيير حالة الطلب.
 */
final readonly class OpenDisputeAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderTermination $termination,
        private SettingsRepository $settings,
    ) {}

    public function execute(
        Order $order,
        ActorType $actor,
        int $actorId,
        string $reasonCode,
        string $description,
    ): Dispute {
        $this->assertWindow($order);

        if ($order->hasOpenDispute()) {
            throw BusinessRuleViolationException::rule('BR-120', 'يوجد نزاع مفتوح على هذا الطلب.');
        }

        $isPostClose = $order->status === OrderStatus::Closed;

        if ($isPostClose) {
            return $this->openPostCloseFile($order, $actor, $actorId, $reasonCode, $description);
        }

        $dispute = null;

        $this->stateMachine->apply(
            order: $order,
            action: 'openDispute',
            actorType: $actor,
            actorId: $actorId,
            meta: ['reason_code' => $reasonCode],
            mutate: function (Order $fresh) use (&$dispute, $actor, $actorId, $reasonCode, $description): void {
                $fresh->disputed_from_status = $fresh->status;
                $dispute = $this->createDispute($fresh, $actor, $actorId, $reasonCode, $description, false);

                // المقترح المعلّق يُسحب، والمحادثة تبقى مفتوحة حتى القرار
                $this->termination->apply($fresh, closeConversations: false);
            },
            refType: 'dispute',
        );

        return $dispute->refresh();
    }

    /** BR-120 — نزاع بعد الإغلاق يوقف التسوية ولا يغير الحالة. */
    private function openPostCloseFile(Order $order, ActorType $actor, int $actorId, string $reasonCode, string $description): Dispute
    {
        return DB::transaction(function () use ($order, $actor, $actorId, $reasonCode, $description): Dispute {
            $dispute = $this->createDispute($order, $actor, $actorId, $reasonCode, $description, true);

            $order->forceFill(['settlement_eligible_at' => null])->save();

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::DisputeOpened,
                'actor_type' => $actor,
                'actor_id' => $actorId,
                'from_status' => $order->status,
                'to_status' => $order->status,
                'ref_type' => 'dispute',
                'ref_id' => $dispute->getKey(),
                'meta' => ['reason_code' => $reasonCode, 'post_close' => true],
                'created_at' => now(),
            ]);

            return $dispute;
        });
    }

    private function createDispute(Order $order, ActorType $actor, int $actorId, string $reasonCode, string $description, bool $isPostClose): Dispute
    {
        return Dispute::query()->create([
            'order_id' => $order->getKey(),
            'opened_by_type' => $actor,
            'opened_by_id' => $actorId,
            'reason_code' => $reasonCode,
            'description' => $description,
            'is_post_close' => $isPostClose,
            'status' => DisputeStatus::Open,
        ]);
    }

    /** BR-120 — من ARRIVED حتى CFG-060 بعد الإغلاق (AC-DSP-02). */
    private function assertWindow(Order $order): void
    {
        if ($order->status->allowsDispute()) {
            return;
        }

        if ($order->status !== OrderStatus::Closed || $order->closed_at === null) {
            throw BusinessRuleViolationException::rule('BR-120', 'فتح المشكلة متاح من وصول الفني وحتى مهلة ما بعد الإغلاق.');
        }

        $deadline = $order->closed_at->addHours($this->settings->int(Cfg::DisputeWindowHours));

        if (CarbonImmutable::now()->gt($deadline)) {
            throw BusinessRuleViolationException::rule(
                'BR-120',
                'انتهت مهلة فتح المشكلة بعد إغلاق الطلب.',
            );
        }
    }
}
