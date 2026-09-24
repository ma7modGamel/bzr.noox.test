<?php

declare(strict_types=1);

use App\Http\Middleware\EnsureIdempotency;
use App\Http\Middleware\ResolveAppMode;
use App\Support\Exceptions\DomainException;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Auth\AuthenticationException;
use Illuminate\Database\Eloquent\ModelNotFoundException;
use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\ValidationException;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;
use Symfony\Component\HttpKernel\Exception\ThrottleRequestsException;

return Application::configure(basePath: dirname(__DIR__))
    ->withRouting(
        web: __DIR__.'/../routes/web.php',
        api: __DIR__.'/../routes/api.php',
        apiPrefix: 'api/v1',
        commands: __DIR__.'/../routes/console.php',
        health: '/up',
    )
    ->withMiddleware(function (Middleware $middleware): void {
        $middleware->validateCsrfTokens(except: [
            'webhooks/fawry',
        ]);

        $middleware->api(append: [
            ResolveAppMode::class,
            EnsureIdempotency::class,
        ]);
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );

        /*
        | جدول الأخطاء في 31-API-CONTRACT هو العقد الوحيد.
        | كل استجابة خطأ من الـ API تخرج بنفس الشكل: { error: { code, message, ... } }
        */
        $exceptions->render(function (DomainException $e, Request $request) {
            return $request->is('api/*') ? $e->render() : null;
        });

        $exceptions->render(function (ValidationException $e, Request $request) {
            return $request->is('api/*')
                ? new JsonResponse([
                    'error' => [
                        'code' => 'VALIDATION_FAILED',
                        'message' => 'بيانات غير صالحة.',
                        'fields' => $e->errors(),
                    ],
                ], 422)
                : null;
        });

        $exceptions->render(function (AuthenticationException $e, Request $request) {
            return $request->is('api/*')
                ? new JsonResponse(['error' => ['code' => 'UNAUTHENTICATED', 'message' => 'يلزم تسجيل الدخول.']], 401)
                : null;
        });

        /*
        | 23 §قاعدة الملكية — المورد غير المملوك يعيد 404 لا 403، لعدم كشف وجوده.
        | لارافيل يحوّل AuthorizationException إلى AccessDeniedHttpException قبل هذه المرحلة،
        | فيُلتقط النوعان معًا. الرفض الصريح بـ 403 (مثل ACCOUNT_BLOCKED) يخرج من DomainException لا من هنا.
        */
        $exceptions->render(function (AuthorizationException|AccessDeniedHttpException $e, Request $request) {
            return $request->is('api/*')
                ? new JsonResponse(['error' => ['code' => 'NOT_FOUND', 'message' => 'غير موجود.']], 404)
                : null;
        });

        $exceptions->render(function (ModelNotFoundException|NotFoundHttpException $e, Request $request) {
            return $request->is('api/*')
                ? new JsonResponse(['error' => ['code' => 'NOT_FOUND', 'message' => 'غير موجود.']], 404)
                : null;
        });

        $exceptions->render(function (ThrottleRequestsException $e, Request $request) {
            return $request->is('api/*')
                ? new JsonResponse(['error' => ['code' => 'RATE_LIMITED', 'message' => 'محاولات كثيرة. حاول لاحقًا.']], 429)
                : null;
        });
    })->create();
