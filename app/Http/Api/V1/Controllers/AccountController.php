<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Content\Services\TermsService;
use App\Modules\Identity\Actions\DeleteAccountAction;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\OrderMedia;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpFoundation\StreamedResponse;

/** C33 — البيانات الشخصية، الأمان، وحذف الحساب. */
final class AccountController
{
    public function show(Request $request): JsonResponse
    {
        return new JsonResponse(['user' => $this->payload($request->user())]);
    }

    public function update(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['sometimes', 'string', 'min:2', 'max:100'],
            'phone' => ['sometimes', 'regex:/^01[0-9]{9}$/'],
            'avatar_media_id' => ['sometimes', 'nullable', 'integer'],
            'rating_reminders_enabled' => ['sometimes', 'boolean'], // NTF-18 فقط (17، DEC-058)
        ]);

        /** @var User $user */
        $user = $request->user();
        if (array_key_exists('avatar_media_id', $data) && $data['avatar_media_id'] !== null) {
            $media = OrderMedia::query()
                ->whereKey($data['avatar_media_id'])
                ->where('uploaded_by', $user->getKey())
                ->whereNull('order_id')
                ->where('type', 'IMAGE')
                ->first();
            if ($media === null) {
                throw ValidationException::withMessages(['avatar_media_id' => 'الصورة غير صالحة أو غير مملوكة للحساب.']);
            }

            if ($user->avatar_path !== null && $user->avatar_path !== $media->path) {
                Storage::disk('local')->delete($user->avatar_path);
            }
            $data['avatar_path'] = $media->path;
            unset($data['avatar_media_id']);
            $media->delete();
        }

        $user->fill($data)->save();

        return new JsonResponse(['user' => $this->payload($user->refresh())]);
    }

    public function avatar(Request $request): StreamedResponse
    {
        /** @var User $user */
        $user = $request->user();
        abort_if($user->avatar_path === null || ! Storage::disk('local')->exists($user->avatar_path), 404);

        return Storage::disk('local')->response($user->avatar_path);
    }

    public function password(Request $request): JsonResponse
    {
        $data = $request->validate([
            'current_password' => ['required', 'string'],
            'password' => ['required', 'string', 'min:8', 'confirmed'],
        ]);

        /** @var User $user */
        $user = $request->user();
        if (! Hash::check($data['current_password'], $user->password)) {
            throw ValidationException::withMessages(['current_password' => 'كلمة المرور الحالية غير صحيحة.']);
        }

        $user->forceFill(['password' => $data['password']])->save();

        return new JsonResponse(status: 204);
    }

    public function destroy(Request $request, DeleteAccountAction $action): JsonResponse
    {
        if (! $action->execute($request->user())) {
            return new JsonResponse([
                'error' => [
                    'code' => 'ACTIVE_ORDER_EXISTS',
                    'message' => 'لا يمكن حذف الحساب قبل انتهاء الطلبات النشطة.',
                ],
            ], 409);
        }

        return new JsonResponse(status: 204);
    }

    /** @return array<string, mixed> */
    private function payload(User $user): array
    {
        return [
            'id' => $user->getKey(),
            'name' => $user->name,
            'email' => $user->email,
            'phone' => $user->phone,
            'avatar_url' => $user->avatar_path === null ? null : route('api.account.avatar'),
            'is_verified' => $user->isVerified(),
            'rating_reminders_enabled' => (bool) ($user->rating_reminders_enabled ?? true),
            // BR-018 — C05 يعرض الموافقة فقط عند نسخة شروط لم يوافق عليها العميل بعد.
            'terms' => [
                'current_version' => app(TermsService::class)->currentVersion(),
                'accepted_version' => $user->accepted_terms_version,
                'acceptance_required' => app(TermsService::class)->acceptanceRequired($user),
            ],
            'available_actions' => ['update_account', 'change_password', 'delete_account', 'logout'],
        ];
    }
}
