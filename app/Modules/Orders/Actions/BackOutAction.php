<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderDeadlines;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * T-06 / T-07 — اعتذار الفني قبل الوصول، أو إعادة فتح إدارية.
 *
 * BR-036: العرض ← `BACKED_OUT`، والطلب ← `OPEN` بنافذة جديدة، والعروض `NOT_SELECTED` تعود
 * `SUBMITTED` (وضع السوق)، ويُمنع المعتذر من التقديم على نفس الطلب — يحرسه صف `BACKED_OUT` نفسه.
 * في وضع الموظفين يعود الطلب إلى "بانتظار التعيين" بمهلة تعيين جديدة (39).
 */
final readonly class BackOutAction
{
    public function __construct(
        private OrderStateMachine $stateMachine,
        private OrderDeadlines $deadlines,
        private FeatureGate $features,
    ) {}

    public function execute(
        Order $order,
        ActorType $actor,
        ?int $actorId,
        CancelReason $reason,
        ?ProviderProfile $provider = null,
    ): Order {
        if ($actor === ActorType::Provider && $order->provider_profile_id !== $provider?->getKey()) {
            throw BusinessRuleViolationException::rule('BR-036', 'هذا الطلب غير مسنَد إليك.');
        }

        $previousProviderId = $order->provider_profile_id;

        return $this->stateMachine->apply(
            order: $order,
            action: 'backOut',
            actorType: $actor,
            actorId: $actorId,
            meta: [
                'reason_code' => $reason->value,
                'previous_provider_profile_id' => $previousProviderId,
            ],
            mutate: function (Order $fresh) use ($previousProviderId): void {
                Offer::query()
                    ->where('order_id', $fresh->getKey())
                    ->where('provider_profile_id', $previousProviderId)
                    ->where('status', OfferStatus::Accepted->value)
                    ->update(['status' => OfferStatus::BackedOut->value, 'decided_at' => now()]);

                // وضع السوق: العروض غير المختارة تعود نشطة (BR-036)
                if ($this->features->offersEnabled()) {
                    Offer::query()
                        ->where('order_id', $fresh->getKey())
                        ->where('status', OfferStatus::NotSelected->value)
                        ->update(['status' => OfferStatus::Submitted->value, 'decided_at' => null]);
                }

                $deadlines = $this->deadlines->for(
                    $fresh->operating_mode,
                    $fresh->timing_type,
                    $fresh->slot_start,
                );

                $fresh->provider_profile_id = null;
                $fresh->accepted_offer_id = null;
                $fresh->assigned_by_admin_id = null;
                $fresh->assigned_at = null;
                $fresh->confirmed_at = null;
                $fresh->trip_started_at = null;
                $fresh->offers_close_at = $deadlines['offers_close_at'];
                $fresh->selection_deadline_at = $deadlines['selection_deadline_at'];
                $fresh->reopen_count = $fresh->reopen_count + 1;
            },
        );
    }
}
