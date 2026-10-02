<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Providers\Actions\UpdateProviderProfileAction;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

final class ProviderProfileController
{
    public function show(Request $request): JsonResponse
    {
        return new JsonResponse(['data' => $this->payload($this->profile($request))]);
    }

    public function update(Request $request, UpdateProviderProfileAction $action): JsonResponse
    {
        $data = $request->validate([
            'experience_years' => ['required', 'integer', 'between:0,50'],
            'bio' => ['nullable', 'string', 'max:300'],
            'specialty_ids' => ['required', 'array', 'min:1'],
            'specialty_ids.*' => ['integer', 'distinct'],
            'area_ids' => ['required', 'array', 'min:1'],
            'area_ids.*' => ['integer', 'distinct'],
        ]);

        $profile = $action->execute(
            $this->profile($request),
            (int) $data['experience_years'],
            $data['bio'] ?? null,
            array_map('intval', $data['specialty_ids']),
            array_map('intval', $data['area_ids']),
        );

        return new JsonResponse(['data' => $this->payload($profile)]);
    }

    /** @return array<string, mixed> */
    private function payload(ProviderProfile $profile): array
    {
        $profile->load(['user', 'categories:id,name', 'categories.problemTypes:id,category_id,name', 'specialties:id,name', 'areas.city:id,name', 'portfolioItems']);

        return [
            'id' => $profile->getKey(),
            'name' => $profile->user->name,
            'avatar_path' => $profile->user->avatar_path,
            'bio' => $profile->bio,
            'experience_years' => $profile->experience_years,
            'rating_avg' => $profile->rating_avg,
            'ratings_count' => $profile->ratings_count,
            'completed_orders' => $profile->completed_orders_count,
            'avg_response_minutes' => $profile->avg_response_minutes,
            'categories' => $profile->categories->map(fn ($category): array => [
                'id' => $category->getKey(),
                'name' => $category->name,
                'specialties' => $category->problemTypes->map->only(['id', 'name'])->values(),
            ])->values(),
            'specialty_ids' => $profile->specialties->modelKeys(),
            'areas' => $profile->areas->map(fn ($area): array => [
                'id' => $area->getKey(),
                'name' => $area->name,
                'city' => $area->city->name,
            ])->values(),
            'area_ids' => $profile->areas->modelKeys(),
            'payout_method' => $profile->payout_method,
            'portfolio' => $profile->portfolioItems->map->only(['id', 'image_path', 'caption'])->values(),
            'available_actions' => ['update_provider_profile', 'add_portfolio_item', 'delete_portfolio_item', 'contact_support'],
        ];
    }

    private function profile(Request $request): ProviderProfile
    {
        return $request->user()->providerProfile
            ?? throw BusinessRuleViolationException::rule('BR-022', 'لا يوجد ملف فني لهذا الحساب.');
    }
}
