<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\ProviderResource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Orders\Services\OrderPresentation;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\Review;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

/** ملف الفني داخل سياق عرض يملكه العميل — SCR-C07 وBR-024. */
final class ProviderController
{
    public function show(
        Request $request,
        ProviderProfile $provider,
        OrderPresentation $presentation,
    ): ProviderResource {
        $order = $this->contextOrder($request, $provider);

        $provider->load([
            'user', 'categories:id,name,name_en', 'specialties:id,name,name_en', 'portfolioItems',
            'reviews' => fn ($query) => $query->visible()->with(['customer:id,name', 'photos', 'order.area'])->latest()->limit(10),
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

    public function image(Request $request, ProviderProfile $provider, string $image): StreamedResponse
    {
        $this->contextOrder($request, $provider);
        $path = $image === 'avatar' ? $provider->user->avatar_path : $provider->portfolioItems()->whereKey((int) $image)->value('image_path');
        abort_if($path === null || ! Storage::disk('local')->exists($path), 404);

        return Storage::disk('local')->response($path, headers: ['Cache-Control' => 'no-store, private', 'X-Content-Type-Options' => 'nosniff']);
    }

    public function reviewImage(Request $request, ProviderProfile $provider, Review $review, OrderMedia $media): StreamedResponse
    {
        $this->contextOrder($request, $provider);
        abort_unless($review->provider_profile_id === $provider->id && $review->hidden_at === null
            && $media->order_id === $review->order_id && $media->purpose === 'CUSTOMER_REVIEW' && $media->uploaded_by === $review->customer_id, 404);
        abort_unless(Storage::disk('local')->exists($media->path), 404);

        return Storage::disk('local')->response($media->path, headers: ['Cache-Control' => 'no-store, private', 'X-Content-Type-Options' => 'nosniff']);
    }

    private function contextOrder(Request $request, ProviderProfile $provider): Order
    {
        $data = $request->validate(['order_id' => ['required', 'integer']]);
        $order = Order::query()->with(['offers', 'pendingProposalRecord', 'review', 'customerRating', 'conversations'])
            ->whereKey($data['order_id'])->where('customer_id', $request->user()->getKey())->firstOrFail();
        abort_unless($order->provider_profile_id === $provider->getKey()
            || $order->offers->contains(fn ($offer): bool => $offer->provider_profile_id === $provider->getKey()), 404);

        return $order;
    }
}
