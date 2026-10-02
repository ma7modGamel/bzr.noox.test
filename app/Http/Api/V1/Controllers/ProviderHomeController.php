<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\OrderResource;
use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Actions\SetProviderAvailabilityAction;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

final class ProviderHomeController
{
    public function show(Request $request, FeatureGate $features): JsonResponse
    {
        return new JsonResponse([
            'data' => $this->payload($request, $this->activeProfile($request), $features),
        ]);
    }

    public function updateAvailability(
        Request $request,
        SetProviderAvailabilityAction $action,
        FeatureGate $features,
    ): JsonResponse {
        $data = $request->validate([
            'available_now' => ['required', 'boolean'],
        ]);

        /** @var User $user */
        $user = $request->user();
        $profile = $action->execute($user, (bool) $data['available_now']);

        return new JsonResponse([
            'data' => $this->payload($request, $profile, $features),
        ]);
    }

    /** @return array<string, mixed> */
    private function payload(Request $request, ProviderProfile $profile, FeatureGate $features): array
    {
        $marketplace = $features->offersEnabled();
        $duesBlocked = $marketplace && $profile->isBlockedByDues();
        $showAvailableRequests = $marketplace && $profile->available_now && ! $duesBlocked;
        $activeOrder = Order::query()
            ->with($this->resourceRelations())
            ->whereBelongsTo($profile, 'providerProfile')
            ->active()
            ->latest('id')
            ->first();
        $availableActions = ['set_availability', 'open_notifications', 'open_messages', 'open_earnings', 'open_provider_profile'];

        if ($activeOrder !== null) {
            $availableActions[] = 'open_assigned_order';
        }

        if ($showAvailableRequests) {
            $availableActions[] = 'open_available_requests';
        }

        if ($marketplace) {
            $availableActions[] = 'open_my_offers';
        }

        return [
            'operating_mode' => $features->mode()->value,
            'available_now' => $profile->available_now,
            'active_order' => $activeOrder === null
                ? null
                : (new OrderResource($activeOrder))->resolve($request),
            'show_available_requests' => $showAvailableRequests,
            'dues_blocked' => $duesBlocked,
            'unread_notifications' => NotificationController::unreadCount($request), // DEC-058
            'available_actions' => $availableActions,
        ];
    }

    private function activeProfile(Request $request): ProviderProfile
    {
        /** @var User $user */
        $user = $request->user();
        $profile = $user->providerProfile;

        if ($user->status !== UserStatus::Active || $profile?->status !== ProviderStatus::Active) {
            throw BusinessRuleViolationException::rule(
                'BR-022',
                'رئيسية الفني متاحة للفني النشط فقط.',
            );
        }

        return $profile;
    }

    /** @return list<string> */
    private function resourceRelations(): array
    {
        return [
            'category', 'problemType', 'area', 'city', 'customer', 'providerProfile.user',
            'offers', 'pendingProposalRecord', 'latestSuccessfulPayment', 'review',
            'customerRating', 'conversations', 'disputes',
        ];
    }
}
