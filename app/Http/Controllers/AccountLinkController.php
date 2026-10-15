<?php

declare(strict_types=1);

namespace App\Http\Controllers;

use App\Modules\Identity\Models\User;
use App\Support\Localization;
use Illuminate\Auth\Events\PasswordReset;
use Illuminate\Auth\Events\Verified;
use Illuminate\Contracts\View\View;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Password;
use Illuminate\Support\Str;

/**
 * SCR-W02 — صفحات روابط البريد (NTF-27) على نطاق المنتج: تفعيل البريد وتعيين كلمة مرور جديدة.
 * نفس قواعد نقاط API (DEC-025)، وبعد تعيين كلمة المرور تُلغى كل الجلسات.
 */
final class AccountLinkController
{
    public function verify(Request $request, int $id, string $hash): View
    {
        if (! $request->hasValidSignature()) {
            return $this->expired();
        }

        $user = User::query()->find($id);
        $valid = $user !== null && hash_equals($hash, sha1($user->getEmailForVerification()));

        if ($valid && $user->markEmailAsVerified()) {
            event(new Verified($user));
        }

        return view('public.account-result', [
            'title' => $valid ? Localization::text('web.verified_title') : Localization::text('web.verify_invalid'),
            'success' => $valid,
            'body' => $valid
                ? Localization::text('web.return_login')
                : Localization::text('web.verify_retry'),
        ]);
    }

    public function expired(): View
    {
        return view('public.account-result', [
            'title' => Localization::text('web.link_expired'),
            'success' => false,
            'body' => Localization::text('web.link_retry'),
        ]);
    }

    public function showReset(Request $request, string $token): View
    {
        return view('public.password-reset', [
            'title' => Localization::text('web.new_password'),
            'token' => $token,
            'email' => (string) $request->query('email', ''),
        ]);
    }

    public function reset(Request $request): View|RedirectResponse
    {
        $data = $request->validate([
            'token' => ['required', 'string'],
            'email' => ['required', 'email'],
            'password' => ['required', 'string', 'min:8', 'confirmed'],
        ], [
            'password.min' => Localization::text('web.password_min'),
            'password.confirmed' => Localization::text('web.password_mismatch'),
            'email.email' => Localization::text('web.email_invalid'),
        ]);

        $status = Password::reset(
            $data,
            function (User $user, string $password): void {
                $user->forceFill(['password' => $password, 'remember_token' => Str::random(60)])->save();
                $user->tokens()->delete();
                event(new PasswordReset($user));
            },
        );

        if ($status !== Password::PasswordReset) {
            return back()->withInput($request->only('email'))
                ->withErrors(['password' => Localization::text('web.reset_invalid')]);
        }

        return view('public.account-result', [
            'title' => Localization::text('web.reset_done'),
            'success' => true,
            'body' => Localization::text('web.reset_done_body'),
        ]);
    }
}
