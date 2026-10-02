<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Notifications\Models\DeviceToken;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;

/** رموز FCM للأجهزة — 31 §`/me/devices`، DEC-058. */
final class DeviceController
{
    /** ينشئ الرمز أو ينقله للحساب الحالي (جهاز غيّر حسابه). */
    public function store(Request $request): JsonResponse
    {
        $data = $request->validate([
            'token' => ['required', 'string', 'min:20', 'max:4096'],
            'platform' => ['required', Rule::in(DeviceToken::PLATFORMS)],
        ]);

        DeviceToken::query()->updateOrCreate(
            ['token' => $data['token']],
            [
                'user_id' => $request->user()->getKey(),
                'platform' => $data['platform'],
                'app_mode' => $request->attributes->get('app_mode', 'CUSTOMER'),
                'last_used_at' => now(),
            ],
        );

        return new JsonResponse(status: 204);
    }

    /** الخروج: يحذف الرمز إن كان للحساب الحالي فقط. */
    public function destroy(Request $request): JsonResponse
    {
        $data = $request->validate(['token' => ['required', 'string', 'max:4096']]);

        DeviceToken::query()
            ->where('token', $data['token'])
            ->where('user_id', $request->user()->getKey())
            ->delete();

        return new JsonResponse(status: 204);
    }
}
