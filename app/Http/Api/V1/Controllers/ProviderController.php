<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\ProviderResource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderPresentation;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Http\Request;

/** ملف الفني داخل سياق عرض يملكه العميل — SCR-C07 وBR-024. */
final class ProviderController
{
    public function show(
        Request $request,
        ProviderProfile $provider,
        OrderPresentation $presentation,
    ): ProviderResource {
        $data = $request->validate([
            'order_id' => ['required', 'integer'],
        ]);

        $order = Order::query()
            ->with(['offers', 'pendingProposalRecord', 'review', 'customerRating', 'conversations'])
            ->whereKey($data['order_id'])
            ->where('customer_id', $request->user()->getKey())
            ->firstOrFail();

        $belongsToOrder = $order->provider_profile_id === $provider->getKey()
            || $order->offers->contains(fn ($offer): bool => $offer->provider_profile_id === $provider->getKey());

        abort_unless($belongsToOrder, 404);

        $provider->load([
            'user', 'categories:id,name', 'specialties:id,name', 'portfolioItems',
            'reviews' => fn ($query) => $query->visible()->with('customer:id,name')->latest()->limit(10),
        ]);

        $actions = $presentation->availableActions($order, $request->user(), ActorType::Customer);
        $providerOfferIsSelectable = $order->offers->contains(
            fn ($offer): bool => $offer->provider_profile_id === $provider->getKey()
                && $offer->status === OfferStatus::Submitted,
        );

        $contextActions = array_values(array_intersect($actions, ['chat']));
        if ($providerOfferIsSelectable && in_array('accept_offer', $actions, true)) {
            $contextActions[] = 'accept_offer';
        }
        $contextActions[] = 'report_provider';

        return new ProviderResource($provider, array_values(array_unique($contextActions)));
    }
}
