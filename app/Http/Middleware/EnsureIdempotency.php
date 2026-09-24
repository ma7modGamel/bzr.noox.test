<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Symfony\Component\HttpFoundation\Response;

/**
 * `Idempotency-Key` — 31 §الاتفاقيات: نفس المفتاح خلال 24 ساعة يعيد نفس النتيجة.
 *
 * يحمي من الضغط المزدوج ومن إعادة المحاولة بعد انقطاع الشبكة (EC-25)،
 * وهو ضروري لأن التطبيقات لا تخزّن إجراءات للإرسال لاحقًا (DEC-042).
 */
final class EnsureIdempotency
{
    private const TTL_HOURS = 24;

    public function handle(Request $request, Closure $next): Response
    {
        $key = $request->header('Idempotency-Key');

        if ($key === null || ! $request->isMethod('POST')) {
            return $next($request);
        }

        $cacheKey = 'idempotency:'.sha1(implode('|', [
            $key,
            (string) $request->user()?->getAuthIdentifier(),
            $request->path(),
        ]));

        $cached = Cache::get($cacheKey);

        if (is_array($cached)) {
            return response($cached['body'], $cached['status'])
                ->header('Content-Type', 'application/json')
                ->header('Idempotent-Replay', 'true');
        }

        $response = $next($request);

        if ($response->getStatusCode() < 400) {
            Cache::put($cacheKey, [
                'status' => $response->getStatusCode(),
                'body' => $response->getContent(),
            ], now()->addHours(self::TTL_HOURS));
        }

        return $response;
    }
}
