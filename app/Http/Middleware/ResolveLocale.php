<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class ResolveLocale
{
    /**
     * Handle an incoming request.
     *
     * @param  Closure(Request): (Response)  $next
     */
    public function handle(Request $request, Closure $next, ?string $mode = null): Response
    {
        $previous = app()->getLocale();
        $locale = trim((string) $request->header('Accept-Language')) !== ''
            ? ($request->getPreferredLanguage(['ar', 'en']) ?? 'ar')
            : ($request->user('sanctum')?->preferredLocale() ?? 'ar');
        if ($mode === 'query' && in_array($request->query('locale'), ['ar', 'en'], true)) {
            $locale = $request->query('locale');
        }
        $request->attributes->set('locale', $locale);
        app()->setLocale($locale);

        try {
            $response = $next($request);
            $response->headers->set('Content-Language', $locale);
            $response->setVary('Accept-Language', false);

            return $response;
        } finally {
            app()->setLocale($previous);
        }
    }
}
