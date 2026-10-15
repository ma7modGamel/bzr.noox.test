<?php

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

final class AttachWorkPhotoAction
{
    public function execute(Order $order, ProviderProfile $provider, int $mediaId, string $purpose): Order
    {
        return DB::transaction(function () use ($order, $provider, $mediaId, $purpose): Order {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            $allowed = $purpose === 'REPAIR_BEFORE'
                ? [OrderStatus::Arrived, OrderStatus::AwaitingQuoteApproval, OrderStatus::InProgress]
                : [OrderStatus::InProgress];

            if (! in_array($purpose, ['REPAIR_BEFORE', 'REPAIR_AFTER'], true)
                || $fresh->provider_profile_id !== $provider->getKey()
                || ! in_array($fresh->status, $allowed, true)
                || $fresh->hasPendingNoShowReport()) {
                throw BusinessRuleViolationException::rule('BR-014', Localization::text('copy.work_photo_unavailable'));
            }

            $media = OrderMedia::query()->lockForUpdate()->find($mediaId);

            if ($media === null || $media->uploaded_by !== $provider->user_id || $media->type !== 'IMAGE'
                || $media->expires_at?->isPast()
                || ($media->order_id !== null && ($media->order_id !== $fresh->getKey() || $media->purpose !== $purpose))) {
                throw BusinessRuleViolationException::rule('BR-014', Localization::text('copy.the_photo_is_invalid_or_does_not_belong_to'));
            }

            if ($media->order_id === $fresh->getKey()) {
                return $fresh;
            }

            if ($fresh->media()->where('purpose', $purpose)->count() >= 5) {
                throw BusinessRuleViolationException::rule('BR-014', Localization::text('copy.work_photo_limit'));
            }

            $media->update(['order_id' => $fresh->getKey(), 'purpose' => $purpose, 'expires_at' => null]);
            $fresh->increment('version');

            return $fresh;
        });
    }
}
