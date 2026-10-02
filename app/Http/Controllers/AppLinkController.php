<?php

declare(strict_types=1);

namespace App\Http\Controllers;

use Illuminate\Http\JsonResponse;
use Illuminate\Http\Response;

/**
 * روابط البريد `https://{APP_DOMAIN}/app/*` (DEC-058): ملفا التحقق لـApp Links وUniversal Links،
 * وصفحة بديلة بلا بيانات الطلب لمن فتح الرابط بدون التطبيق.
 */
final class AppLinkController
{
    public function assetLinks(): JsonResponse
    {
        $fingerprints = (array) config('services.app_links.android_sha256');

        return new JsonResponse($fingerprints === [] ? [] : [[
            'relation' => ['delegate_permission/common.handle_all_urls'],
            'target' => [
                'namespace' => 'android_app',
                'package_name' => config('services.app_links.package'),
                'sha256_cert_fingerprints' => $fingerprints,
            ],
        ]]);
    }

    public function appleAppSiteAssociation(): JsonResponse
    {
        $teamId = (string) config('services.app_links.apple_team_id');

        return new JsonResponse([
            'applinks' => [
                'details' => $teamId === '' ? [] : [[
                    'appIDs' => [$teamId.'.'.config('services.app_links.bundle_id')],
                    'components' => [['/' => '/app/*']],
                ]],
            ],
        ]);
    }

    public function fallback(): Response
    {
        return response()->view('public.account-result', [
            'title' => 'افتح الرابط من تطبيق '.config('app.name'),
            'body' => 'هذا الرابط يفتح داخل تطبيق '.config('app.name').'. افتحه من هاتف عليه التطبيق وسجّل الدخول بنفس الحساب.',
            'success' => true,
        ])->header('Cache-Control', 'no-store')->header('X-Robots-Tag', 'noindex, nofollow');
    }
}
