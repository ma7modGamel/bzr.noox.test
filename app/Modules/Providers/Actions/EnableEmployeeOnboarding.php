<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;

final class EnableEmployeeOnboarding
{
    public function execute(User $user, Admin $admin): ProviderProfile
    {
        abort_unless($admin->is_active, 403);

        return DB::transaction(function () use ($user, $admin): ProviderProfile {
            $user = User::query()->lockForUpdate()->findOrFail($user->getKey());
            if ($user->providerProfile()->exists()) {
                throw BusinessRuleViolationException::rule('BR-131', Localization::text('copy.employee_profile_already_exists'));
            }

            $profile = $user->providerProfile()->create([
                'employment_type' => EmploymentType::Employee,
                'status' => ProviderStatus::PendingReview,
                'available_now' => false,
            ]);
            Log::info('Employee onboarding enabled', ['admin_id' => $admin->getKey(), 'user_id' => $user->getKey(), 'provider_profile_id' => $profile->getKey()]);

            return $profile;
        }, attempts: 3);
    }
}
