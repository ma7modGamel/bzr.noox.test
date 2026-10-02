<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\User;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Support\Facades\DB;

final class SetProviderAvailabilityAction
{
    public function execute(User $user, bool $availableNow): ProviderProfile
    {
        return DB::transaction(function () use ($user, $availableNow): ProviderProfile {
            $profile = ProviderProfile::query()
                ->whereBelongsTo($user)
                ->lockForUpdate()
                ->first();

            if ($user->status !== UserStatus::Active || $profile?->status !== ProviderStatus::Active) {
                throw BusinessRuleViolationException::rule(
                    'BR-022',
                    'الإتاحة متاحة للفني النشط فقط.',
                );
            }

            $profile->update(['available_now' => $availableNow]);

            return $profile->refresh();
        }, attempts: 3);
    }
}
