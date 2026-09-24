<?php

declare(strict_types=1);

namespace App\Modules\Identity\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;

/** حذف ناعم مع إخفاء PII والاحتفاظ بسجل الطلبات المحاسبي. */
final readonly class DeleteAccountAction
{
    public function hasActiveOrders(User $user): bool
    {
        $finalStatuses = array_map(
            static fn (OrderStatus $status): string => $status->value,
            [OrderStatus::Closed, OrderStatus::Cancelled, OrderStatus::Expired],
        );

        if ($user->orders()->whereNotIn('status', $finalStatuses)->exists()) {
            return true;
        }

        return $user->providerProfile?->orders()->whereNotIn('status', $finalStatuses)->exists() ?? false;
    }

    public function execute(User $user): bool
    {
        if ($this->hasActiveOrders($user)) {
            return false;
        }

        DB::transaction(function () use ($user): void {
            $profile = $user->providerProfile;
            if ($profile !== null) {
                Storage::disk('local')->delete($profile->documents()->pluck('path')->all());
                Storage::disk('local')->delete($profile->portfolioItems()->pluck('image_path')->all());
                $profile->documents()->delete();
                $profile->portfolioItems()->delete();
                $profile->forceFill([
                    'bio' => null,
                    'payout_method' => null,
                    'payout_details' => null,
                    'available_now' => false,
                ])->save();
            }

            if ($user->avatar_path !== null) {
                Storage::disk('local')->delete($user->avatar_path);
            }

            $user->addresses()->delete();
            $user->tokens()->delete();
            $user->notifications()->delete();
            DB::table('device_tokens')->where('user_id', $user->getKey())->delete();
            DB::table('sessions')->where('user_id', $user->getKey())->delete();

            $user->forceFill([
                'name' => 'حساب محذوف',
                'email' => sprintf('deleted-%d-%s@invalid.local', $user->getKey(), Str::lower(Str::random(12))),
                'phone' => 'deleted-'.$user->getKey(),
                'avatar_path' => null,
                'password' => Str::random(64),
                'remember_token' => null,
            ])->save();
            $user->delete();
        });

        return true;
    }
}
