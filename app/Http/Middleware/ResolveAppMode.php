<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * `X-App-Mode: CUSTOMER | PROVIDER` — 31 §الاتفاقيات.
 * يحدد **ما يُعرض** فقط؛ الصلاحية تبقى من الخادم عبر السياسات (23).
 */
final class ResolveAppMode
{
    public const CUSTOMER = 'CUSTOMER';

    public const PROVIDER = 'PROVIDER';

    public function handle(Request $request, Closure $next): Response
    {
        $mode = strtoupper((string) $request->header('X-App-Mode', self::CUSTOMER));

        $request->attributes->set(
            'app_mode',
            in_array($mode, [self::CUSTOMER, self::PROVIDER], true) ? $mode : self::CUSTOMER,
        );

        return $next($request);
    }
}
