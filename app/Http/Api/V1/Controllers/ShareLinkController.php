<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\ShareLink;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\URL;

final class ShareLinkController
{
    public function store(Request $request, Order $order): JsonResponse
    {
        abort_unless($order->customer_id === $request->user()->getKey(), 404);

        $link = DB::transaction(function () use ($order): ShareLink {
            $order = Order::query()->lockForUpdate()->findOrFail($order->id);
            if ($order->status === OrderStatus::Open || $order->status->isFinal() || $order->provider_profile_id === null) {
                throw BusinessRuleViolationException::rule('DEC-037', Localization::text('copy.sharing_is_only_available_during_the_visit'));
            }

            return ShareLink::query()
                ->where('order_id', $order->getKey())
                ->whereNull('revoked_at')
                ->where('expires_at', '>', now())
                ->latest('id')
                ->first() ?? ShareLink::query()->create([
                    'order_id' => $order->getKey(),
                    'token' => $this->token(),
                    'expires_at' => now()->addHours(ShareLink::LIFETIME_HOURS),
                ]);
        });

        return new JsonResponse(['data' => $this->serialize($link)], 201);
    }

    public function destroy(Request $request, ShareLink $shareLink): JsonResponse
    {
        abort_unless($shareLink->order()->value('customer_id') === $request->user()->getKey(), 404);

        $shareLink->forceFill(['revoked_at' => now()])->save();

        return new JsonResponse(status: 204);
    }

    private function token(): string
    {
        return rtrim(strtr(base64_encode(random_bytes(32)), '+/', '-_'), '=');
    }

    /** @return array<string, mixed> */
    private function serialize(ShareLink $link): array
    {
        return [
            'id' => $link->getKey(),
            'url' => URL::route('share.visit', ['token' => $link->token]),
            'expires_at' => $link->expires_at?->toIso8601String(),
            'revoked_at' => $link->revoked_at?->toIso8601String(),
        ];
    }
}
