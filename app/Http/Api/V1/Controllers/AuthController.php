<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\User;
use Illuminate\Auth\Events\PasswordReset;
use Illuminate\Auth\Events\Registered;
use Illuminate\Auth\Events\Verified;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Password;
use Illuminate\Support\Str;
use Illuminate\Validation\ValidationException;

/**
 * الدخول والتسجيل — DEC-025 (بريد وكلمة مرور، بلا SMS).
 * البريد يجب توثيقه قبل نشر أي طلب (BR-001)، والمحظور لا يدخل (BR-003).
 */
final class AuthController
{
    public function register(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:100'],
            'email' => ['required', 'email', 'max:255', 'unique:users,email'],
            'phone' => ['required', 'regex:/^01[0-9]{9}$/'],   // BR-002 — موبايل مصري
            'password' => ['required', 'string', 'min:8'],
        ]);

        $user = User::query()->create($data + ['status' => UserStatus::Active]);
        event(new Registered($user));

        return new JsonResponse([
            'user' => $this->userPayload($user),
            'token' => $user->createToken('mobile')->plainTextToken,
            'email_verification_required' => true,
        ], 201);
    }

    public function login(Request $request): JsonResponse
    {
        $credentials = $request->validate([
            'email' => ['required', 'email'],
            'password' => ['required', 'string'],
        ]);

        /** @var User|null $user */
        $user = User::query()->where('email', $credentials['email'])->first();

        if ($user === null || ! Hash::check($credentials['password'], $user->password)) {
            throw ValidationException::withMessages(['email' => 'بيانات الدخول غير صحيحة.']);
        }

        // BR-003 — المحظور لا يستطيع الدخول وتُلغى رموزه (AC-ACC-02)
        if ($user->isBlocked()) {
            $user->tokens()->delete();

            return new JsonResponse([
                'error' => ['code' => 'ACCOUNT_BLOCKED', 'message' => 'الحساب محظور.'],
            ], 403);
        }

        return new JsonResponse([
            'user' => $this->userPayload($user),
            'token' => $user->createToken('mobile')->plainTextToken,
        ]);
    }

    public function logout(Request $request): JsonResponse
    {
        $request->user()->currentAccessToken()->delete();

        return new JsonResponse(status: 204);
    }

    public function resendVerification(Request $request): JsonResponse
    {
        /** @var User $user */
        $user = $request->user();

        if ($user->hasVerifiedEmail()) {
            return new JsonResponse([
                'error' => ['code' => 'EMAIL_ALREADY_VERIFIED', 'message' => 'البريد مفعّل بالفعل.'],
            ], 409);
        }

        $user->sendEmailVerificationNotification();

        return new JsonResponse(status: 204);
    }

    public function verifyEmail(Request $request, int $id, string $hash): JsonResponse
    {
        /** @var User $user */
        $user = User::query()->findOrFail($id);

        if (! hash_equals($hash, sha1($user->getEmailForVerification()))) {
            return new JsonResponse([
                'error' => ['code' => 'INVALID_VERIFICATION_LINK', 'message' => 'رابط التفعيل غير صالح.'],
            ], 403);
        }

        if ($user->markEmailAsVerified()) {
            event(new Verified($user));
        }

        return new JsonResponse(['message' => 'تم تفعيل البريد.']);
    }

    public function forgotPassword(Request $request): JsonResponse
    {
        $data = $request->validate(['email' => ['required', 'email']]);

        Password::sendResetLink(['email' => $data['email']]);

        return new JsonResponse(status: 204);
    }

    public function resetPassword(Request $request): JsonResponse
    {
        $data = $request->validate([
            'email' => ['required', 'email'],
            'token' => ['required', 'string'],
            'password' => ['required', 'string', 'min:8'],
        ]);

        $status = Password::reset(
            $data,
            function (User $user, string $password): void {
                $user->forceFill([
                    'password' => $password,
                    'remember_token' => Str::random(60),
                ])->save();
                $user->tokens()->delete();
                event(new PasswordReset($user));
            },
        );

        if ($status !== Password::PasswordReset) {
            throw ValidationException::withMessages(['email' => 'تعذّرت استعادة كلمة المرور.']);
        }

        return new JsonResponse(status: 204);
    }

    public function me(Request $request): JsonResponse
    {
        return new JsonResponse(['user' => $this->userPayload($request->user())]);
    }

    /** @return array<string, mixed> */
    private function userPayload(User $user): array
    {
        $profile = $user->providerProfile;

        return [
            'id' => $user->getKey(),
            'name' => $user->name,
            'email' => $user->email,
            'phone' => $user->phone,
            'is_verified' => $user->isVerified(),          // BR-005
            'rating_avg' => $user->customer_rating_avg,
            'provider' => $profile === null ? null : [
                'id' => $profile->getKey(),
                'status' => $profile->status->value,
                'available_now' => $profile->available_now,
                'is_verified' => $profile->isVerified(),
            ],
        ];
    }
}
