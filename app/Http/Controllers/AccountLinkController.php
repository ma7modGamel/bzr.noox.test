<?php

declare(strict_types=1);

namespace App\Http\Controllers;

use App\Modules\Identity\Models\User;
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
            'title' => $valid ? 'تم تفعيل البريد' : 'رابط التفعيل غير صالح',
            'success' => $valid,
            'body' => $valid
                ? 'يمكنك الآن العودة إلى التطبيق وتسجيل الدخول.'
                : 'اطلب رابطًا جديدًا من شاشة تفعيل البريد في التطبيق.',
        ]);
    }

    public function expired(): View
    {
        return view('public.account-result', [
            'title' => 'انتهت صلاحية الرابط',
            'success' => false,
            'body' => 'اطلب رابطًا جديدًا من التطبيق.',
        ]);
    }

    public function showReset(Request $request, string $token): View
    {
        return view('public.password-reset', [
            'title' => 'تعيين كلمة مرور جديدة',
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
            'password.min' => 'كلمة المرور 8 أحرف على الأقل.',
            'password.confirmed' => 'كلمتا المرور غير متطابقتين.',
            'email.email' => 'البريد غير صالح.',
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
                ->withErrors(['password' => 'الرابط غير صالح أو انتهت صلاحيته. اطلب رابطًا جديدًا من التطبيق.']);
        }

        return view('public.account-result', [
            'title' => 'تم تعيين كلمة المرور',
            'success' => true,
            'body' => 'سجّل الدخول في التطبيق بكلمة المرور الجديدة. تم تسجيل الخروج من كل الأجهزة.',
        ]);
    }
}
