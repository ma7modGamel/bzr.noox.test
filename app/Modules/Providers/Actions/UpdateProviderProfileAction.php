<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Geography\Models\Area;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Support\Facades\DB;
use Illuminate\Validation\ValidationException;

final class UpdateProviderProfileAction
{
    /** @param list<int> $specialtyIds @param list<int> $areaIds */
    public function execute(
        ProviderProfile $profile,
        int $experienceYears,
        ?string $bio,
        array $specialtyIds,
        array $areaIds,
    ): ProviderProfile {
        $validSpecialties = $profile->categories()
            ->with('problemTypes:id,category_id')
            ->get()
            ->flatMap->problemTypes
            ->pluck('id')
            ->map(static fn (mixed $id): int => (int) $id)
            ->all();

        if (array_diff($specialtyIds, $validSpecialties) !== []) {
            throw ValidationException::withMessages([
                'specialty_ids' => 'أحد التخصصات لا يتبع فئات الفني.',
            ]);
        }

        $validAreaIds = Area::query()->where('is_active', true)->whereKey($areaIds)->pluck('id')->all();
        if (count($validAreaIds) !== count(array_unique($areaIds))) {
            throw ValidationException::withMessages([
                'area_ids' => 'إحدى المناطق غير متاحة.',
            ]);
        }

        DB::transaction(function () use ($profile, $experienceYears, $bio, $specialtyIds, $areaIds): void {
            $profile->forceFill([
                'experience_years' => $experienceYears,
                'bio' => $bio,
            ])->save();
            $profile->specialties()->sync($specialtyIds);
            $profile->areas()->sync($areaIds);
        }, attempts: 3);

        return $profile->refresh();
    }
}
