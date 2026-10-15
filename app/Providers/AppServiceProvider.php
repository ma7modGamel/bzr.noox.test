<?php

namespace App\Providers;

use App\Modules\Communication\Mail\ZeptoMailTransport;
use App\Modules\Notifications\Contracts\PushSender;
use App\Modules\Notifications\Push\FcmPushSender;
use App\Modules\Notifications\Push\LogPushSender;
use App\Modules\Orders\Contracts\RouteEtaProvider;
use App\Modules\Orders\Services\GoogleRoutesEtaProvider;
use App\Modules\Payments\Contracts\PaymentGateway;
use App\Modules\Payments\Services\FawryWebhookSignature;
use App\Modules\Payments\Services\StagingPaymentGateway;
use App\Support\Localization;
use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Notifications\Messages\MailMessage;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Facades\URL;
use Illuminate\Support\ServiceProvider;
use RuntimeException;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        $this->app->useLangPath(resource_path('lang'));

        $this->app->bind(RouteEtaProvider::class, GoogleRoutesEtaProvider::class);
        $this->app->singleton(FawryWebhookSignature::class, fn (): FawryWebhookSignature => new FawryWebhookSignature(
            (string) config('services.fawry.security_key'),
        ));
        $this->app->bind(PaymentGateway::class, function (): PaymentGateway {
            if (config('services.fawry.driver') !== 'staging') {
                throw new RuntimeException('The real Fawry gateway is not enabled until OD-10 is resolved.');
            }

            return app(StagingPaymentGateway::class);
        });
        // DEC-058 — `log` للتطوير والاختبار فقط؛ production يرفض أي مسار غير FCM.
        $this->app->bind(PushSender::class, function (): PushSender {
            $driver = (string) config('services.fcm.driver');

            if ($driver === 'fcm') {
                return app(FcmPushSender::class);
            }

            if ($this->app->environment('production')) {
                throw new RuntimeException('PUSH_DRIVER must be fcm in production (DEP-PUSH-01).');
            }

            return app(LogPushSender::class);
        });
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        // DEC-049 — ZeptoMail API transport (MAIL_MAILER=zeptomail).
        Mail::extend('zeptomail', fn (): ZeptoMailTransport => new ZeptoMailTransport(
            (string) config('services.zeptomail.url'),
            config('services.zeptomail.key'),
            (int) config('services.zeptomail.timeout'),
        ));

        // NTF-27 — روابط البريد تفتح صفحات W02 على نطاق المنتج، لا نقاط API.
        VerifyEmail::createUrlUsing(fn (object $notifiable): string => URL::temporarySignedRoute(
            'web.verification.verify',
            now()->addMinutes((int) config('auth.verification.expire', 60)),
            ['id' => $notifiable->getKey(), 'hash' => sha1($notifiable->getEmailForVerification()), 'locale' => $notifiable->preferredLocale()],
        ));
        VerifyEmail::toMailUsing(fn (object $notifiable, string $url): MailMessage => (new MailMessage)
            ->subject(Localization::text('messages.verify_subject', ['brand' => config('app.name')], $notifiable->preferredLocale()))
            ->greeting(Localization::text('messages.verify_greeting', ['brand' => config('app.name')], $notifiable->preferredLocale()))
            ->line(Localization::text('messages.verify_body', ['brand' => config('app.name')], $notifiable->preferredLocale()))
            ->action(Localization::text('messages.verify_action', locale: $notifiable->preferredLocale()), $url)
            ->line(Localization::text('messages.verify_expiry', ['minutes' => config('auth.verification.expire', 60)], $notifiable->preferredLocale()))
            ->salutation(Localization::text('messages.mail_team', ['brand' => config('app.name')], $notifiable->preferredLocale())));

        ResetPassword::createUrlUsing(fn (object $notifiable, string $token): string => route('password.reset', [
            'token' => $token,
            'locale' => $notifiable->preferredLocale(),
            'email' => $notifiable->getEmailForPasswordReset(),
        ]));
        ResetPassword::toMailUsing(fn (object $notifiable, string $token): MailMessage => (new MailMessage)
            ->subject(Localization::text('messages.reset_subject', ['brand' => config('app.name')], $notifiable->preferredLocale()))
            ->greeting(Localization::text('messages.reset_greeting', ['brand' => config('app.name')], $notifiable->preferredLocale()))
            ->line(Localization::text('messages.reset_body', ['brand' => config('app.name')], $notifiable->preferredLocale()))
            ->action(Localization::text('messages.reset_action', locale: $notifiable->preferredLocale()), route('password.reset', [
                'token' => $token,
                'locale' => $notifiable->preferredLocale(),
                'email' => $notifiable->getEmailForPasswordReset(),
            ]))
            ->line(Localization::text('messages.reset_expiry', ['minutes' => config('auth.passwords.users.expire', 60)], $notifiable->preferredLocale()))
            ->salutation(Localization::text('messages.mail_team', ['brand' => config('app.name')], $notifiable->preferredLocale())));
    }
}
