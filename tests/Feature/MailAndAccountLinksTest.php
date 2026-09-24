<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\Client\Request as HttpRequest;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Facades\Password;
use Illuminate\Support\Facades\URL;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** DEC-048 / DEC-049 / SCR-W02 — البريد من نطاق المنتج بالعربية، وروابطه تفتح صفحات الويب. */
final class MailAndAccountLinksTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function رسالة_التفعيل_عربية_وتشير_إلى_صفحة_الويب(): void
    {
        Notification::fake();
        $user = User::factory()->unverified()->create();
        $user->sendEmailVerificationNotification();

        Notification::assertSentTo($user, VerifyEmail::class, function (VerifyEmail $notification) use ($user): bool {
            $mail = $notification->toMail($user);
            $this->assertStringContainsString('تفعيل', (string) $mail->subject);
            $this->assertStringContainsString('/email/verify/'.$user->id.'/', (string) $mail->actionUrl);
            $this->assertStringNotContainsString('/api/', (string) $mail->actionUrl);

            return true;
        });
    }

    #[Test]
    public function صفحة_التفعيل_تفعل_البريد_وترفض_الرابط_المنتهي(): void
    {
        $user = User::factory()->unverified()->create();
        $url = URL::temporarySignedRoute('web.verification.verify', now()->addHour(), ['id' => $user->id, 'hash' => sha1($user->email)]);

        $this->get($url)->assertOk()->assertSee('تم تفعيل البريد');
        $this->assertTrue($user->refresh()->hasVerifiedEmail());

        $this->get(route('web.verification.verify', ['id' => $user->id, 'hash' => sha1($user->email)]))
            ->assertOk()
            ->assertSee('انتهت صلاحية الرابط');
    }

    #[Test]
    public function استعادة_كلمة_المرور_عبر_صفحة_الويب_تلغي_الجلسات(): void
    {
        Notification::fake();
        $user = User::factory()->create(['email' => 'aya@example.test']);
        $user->createToken('phone');

        $this->postJson('/api/v1/auth/password/forgot', ['email' => $user->email])->assertNoContent();

        $token = null;
        Notification::assertSentTo($user, ResetPassword::class, function (ResetPassword $notification) use ($user, &$token): bool {
            $mail = $notification->toMail($user);
            $this->assertStringContainsString('استعادة كلمة المرور', (string) $mail->subject);
            $this->assertStringContainsString('/password/reset/', (string) $mail->actionUrl);
            $token = $notification->token;

            return true;
        });

        $this->get(route('password.reset', ['token' => $token, 'email' => $user->email]))->assertOk()->assertSee('تعيين كلمة مرور جديدة');
        $this->post(route('password.update'), [
            'token' => $token,
            'email' => $user->email,
            'password' => 'new-password-1',
            'password_confirmation' => 'new-password-1',
        ])->assertOk()->assertSee('تم تعيين كلمة المرور');

        $this->assertSame(0, $user->tokens()->count());
        $this->assertTrue(Password::getRepository()->recentlyCreatedToken($user) === false);
    }

    #[Test]
    public function عناوين_البريد_مشتقة_من_نطاق_المنتج(): void
    {
        $this->assertSame('no-reply@'.config('app.domain'), config('mail.from.address'));
        $this->assertSame('support@'.config('app.domain'), config('mail.support_address'));
    }

    #[Test]
    public function محول_zepto_mail_يرسل_بالمفتاح_والمحتوى_العربي(): void
    {
        config([
            'mail.default' => 'zeptomail',
            'services.zeptomail.key' => 'test-key',
            'services.zeptomail.url' => 'https://api.zeptomail.test/v1.1/email',
        ]);
        Http::fake(['api.zeptomail.test/*' => Http::response(['data' => []], 201)]);

        Mail::raw('مرحبًا من بريمو', function ($message): void {
            $message->to('customer@example.test', 'سارة')->subject('اختبار');
        });

        Http::assertSent(function (HttpRequest $request): bool {
            return $request->url() === 'https://api.zeptomail.test/v1.1/email'
                && $request->hasHeader('Authorization', 'Zoho-enczapikey test-key')
                && $request['to'][0]['email_address']['address'] === 'customer@example.test'
                && $request['from']['address'] === config('mail.from.address')
                && str_contains((string) $request['textbody'], 'مرحبًا من بريمو');
        });
    }
}
