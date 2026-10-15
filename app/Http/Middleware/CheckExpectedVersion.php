<?php

declare(strict_types=1);

namespace App\Http\Middleware;

use App\Modules\Orders\Models\Order;
use App\Support\Localization;
use Closure;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

/**
 * `expected_version` — 31: القفل المتفائل على الطلب.
 * اختلاف النسخة ← `409 CONFLICT` ليعيد التطبيق جلب الطلب ويعرض الحالة الجديدة (42 §الشبكة).
 */
final class CheckExpectedVersion
{
    public function handle(Request $request, Closure $next): Response
    {
        $expected = $request->input('expected_version');
        $order = $request->route('order');

        if ($expected !== null && $order instanceof Order && (int) $expected !== $order->version) {
            return new JsonResponse([
                'error' => [
                    'code' => 'CONFLICT',
                    'message' => Localization::text('copy.this_request_has_changed_since_your_last_update_reload'),
                    'current_version' => $order->version,
                ],
            ], 409);
        }

        return $next($request);
    }
}
