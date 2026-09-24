<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Communication\Enums\ConversationStatus;
use App\Modules\Communication\Models\Conversation;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\TrackingPoint;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Models\PriceProposal;

/**
 * آثار خروج الطلب إلى حالة نهائية أو إلى نزاع (10 §آثار جانبية، 21 §اتساق دورات الحياة):
 * إغلاق العروض (O-06)، سحب المقترح المعلّق، تحويل المحادثات للقراءة فقط، إيقاف التتبع.
 */
final class OrderTermination
{
    /** يُستدعى داخل معاملة آلة الحالات على الصف المقفول. */
    public function apply(Order $order, bool $closeConversations = true): void
    {
        Offer::query()
            ->where('order_id', $order->getKey())
            ->whereIn('status', [OfferStatus::Submitted->value, OfferStatus::NotSelected->value])
            ->update(['status' => OfferStatus::Closed->value, 'decided_at' => now()]);

        PriceProposal::query()
            ->where('order_id', $order->getKey())
            ->where('status', ProposalStatus::Pending->value)
            ->update([
                'status' => ProposalStatus::Withdrawn->value,
                'decided_at' => now(),
            ]);

        if ($closeConversations) {
            Conversation::query()
                ->where('order_id', $order->getKey())
                ->update(['status' => ConversationStatus::ReadOnly->value]);
        }

        TrackingPoint::query()->where('order_id', $order->getKey())->delete();
    }
}
