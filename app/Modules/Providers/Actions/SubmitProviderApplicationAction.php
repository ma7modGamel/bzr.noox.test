<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Data\ProviderApplicationData;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\EmailNotVerifiedException;
use App\Support\Exceptions\ProviderApplicationNotEditableException;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\ValidationException;

final class SubmitProviderApplicationAction
{
    public function execute(User $user, ProviderApplicationData $data): ProviderProfile
    {
        if (! $user->hasVerifiedEmail()) {
            throw EmailNotVerifiedException::make();
        }

        /** @var array{profile: ProviderProfile, obsolete_paths: list<string>} $result */
        $result = DB::transaction(function () use ($user, $data): array {
            $lockedUser = User::query()->lockForUpdate()->findOrFail($user->getKey());
            $profile = ProviderProfile::query()
                ->where('user_id', $lockedUser->getKey())
                ->lockForUpdate()
                ->first();

            if ($profile !== null && $profile->status !== ProviderStatus::Rejected) {
                throw ProviderApplicationNotEditableException::make();
            }

            $profile ??= new ProviderProfile([
                'user_id' => $lockedUser->getKey(),
                'employment_type' => EmploymentType::Independent,
            ]);
            $profile->loadMissing('documents');

            $existingDocuments = $profile->documents->keyBy('type');
            $profilePhoto = $this->ownedImage($lockedUser, $data->profilePhotoMediaId, 'profile_photo_media_id');
            $idFront = $this->ownedImage($lockedUser, $data->idFrontMediaId, 'id_front_media_id');
            $idBack = $this->ownedImage($lockedUser, $data->idBackMediaId, 'id_back_media_id');

            $this->requireStoredImage($profilePhoto, $lockedUser->avatar_path, 'profile_photo_media_id');
            $this->requireStoredImage($idFront, $existingDocuments->get('ID_FRONT')?->path, 'id_front_media_id');
            $this->requireStoredImage($idBack, $existingDocuments->get('ID_BACK')?->path, 'id_back_media_id');

            $obsoletePaths = [];
            if ($profilePhoto !== null) {
                if ($lockedUser->avatar_path !== null && $lockedUser->avatar_path !== $profilePhoto->path) {
                    $obsoletePaths[] = $lockedUser->avatar_path;
                }
                $lockedUser->forceFill(['avatar_path' => $profilePhoto->path])->save();
            }

            $profile->forceFill([
                'bio' => $data->bio,
                'experience_years' => $data->experienceYears,
                'payout_method' => $data->payoutMethod->value,
                'payout_details' => $data->payoutDetails,
                'status' => ProviderStatus::PendingReview,
                'available_now' => false,
                'submitted_at' => now(),
                'reviewed_by' => null,
                'reviewed_at' => null,
                'rejection_reason' => null,
                'suspension_reason' => null,
            ])->save();

            foreach (['ID_FRONT' => $idFront, 'ID_BACK' => $idBack] as $type => $media) {
                if ($media === null) {
                    continue;
                }
                $existingPath = $existingDocuments->get($type)?->path;
                if ($existingPath !== null && $existingPath !== $media->path) {
                    $obsoletePaths[] = $existingPath;
                }
                $profile->documents()->updateOrCreate(['type' => $type], ['path' => $media->path]);
            }

            $profile->categories()->sync($data->categoryIds);
            $profile->specialties()->sync($data->specialtyIds);
            $profile->areas()->sync($data->areaIds);

            collect([$profilePhoto, $idFront, $idBack])
                ->filter()
                ->each(fn (OrderMedia $media) => $media->delete());

            return ['profile' => $profile, 'obsolete_paths' => array_values(array_unique($obsoletePaths))];
        }, attempts: 3);

        foreach ($result['obsolete_paths'] as $path) {
            Storage::disk('local')->delete($path);
        }

        return $result['profile']->refresh()->load(['categories:id,name', 'specialties:id,name', 'areas:id,name', 'documents']);
    }

    private function ownedImage(User $user, ?int $mediaId, string $field): ?OrderMedia
    {
        if ($mediaId === null) {
            return null;
        }

        $media = OrderMedia::query()
            ->whereKey($mediaId)
            ->where('uploaded_by', $user->getKey())
            ->whereNull('order_id')
            ->where('type', 'IMAGE')
            ->where('expires_at', '>', now())
            ->lockForUpdate()
            ->first();

        if ($media === null) {
            throw ValidationException::withMessages([$field => 'الصورة غير صالحة أو غير مملوكة للحساب.']);
        }

        return $media;
    }

    private function requireStoredImage(?OrderMedia $media, ?string $storedPath, string $field): void
    {
        if ($media === null && ($storedPath === null || ! Storage::disk('local')->exists($storedPath))) {
            throw ValidationException::withMessages([$field => 'الصورة مطلوبة.']);
        }
    }
}
