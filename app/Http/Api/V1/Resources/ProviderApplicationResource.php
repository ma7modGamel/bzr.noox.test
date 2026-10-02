<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/** @mixin ProviderProfile|null */
final class ProviderApplicationResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        /** @var ProviderProfile|null $profile */
        $profile = $this->resource;
        if ($profile === null) {
            return [
                'status' => 'NOT_STARTED',
                'display_status' => 'provider.application.status.NOT_STARTED',
                'available_actions' => ['start_provider_application', 'submit_provider_application'],
                'profile_photo_uploaded' => false,
                'documents' => ['id_front' => false, 'id_back' => false],
            ];
        }

        $profile->loadMissing(['user', 'categories:id,name', 'specialties:id,name', 'areas:id,name', 'documents']);

        return [
            'id' => $profile->getKey(),
            'status' => $profile->status->value,
            'display_status' => 'provider.application.status.'.$profile->status->value,
            'rejection_reason' => $profile->rejection_reason,
            'suspension_reason' => $profile->suspension_reason,
            'experience_years' => $profile->experience_years,
            'bio' => $profile->bio,
            'category_ids' => $profile->categories->modelKeys(),
            'specialty_ids' => $profile->specialties->modelKeys(),
            'area_ids' => $profile->areas->modelKeys(),
            'payout_method' => $profile->payout_method,
            'payout_details' => $profile->status === ProviderStatus::Rejected ? $profile->payout_details : null,
            'profile_photo_uploaded' => $profile->user->avatar_path !== null,
            'documents' => [
                'id_front' => $profile->documents->contains('type', 'ID_FRONT'),
                'id_back' => $profile->documents->contains('type', 'ID_BACK'),
            ],
            'submitted_at' => $profile->submitted_at?->toIso8601String(),
            'available_actions' => $this->availableActions($profile->status),
        ];
    }

    /** @return list<string> */
    private function availableActions(ProviderStatus $status): array
    {
        return match ($status) {
            ProviderStatus::Rejected => ['resubmit_provider_application', 'submit_provider_application'],
            ProviderStatus::Active => ['open_provider_home'],
            ProviderStatus::PendingReview, ProviderStatus::Suspended => [],
        };
    }
}
