<?php

declare(strict_types=1);

namespace App\Http\Controllers;

use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Models\LegalPage;
use Illuminate\Http\Response;

/**
 * DEC-051 — `/terms` و`/privacy` و`/cancellation-policy` (و`/faq`) للمتاجر وروابط البريد.
 * تعرض النسخة السارية المنشورة فقط، بخط Cairo وRTL وألوان tokens.
 */
final class LegalPageController
{
    public function __invoke(string $slug): Response
    {
        $page = LegalPage::query()->where('slug', LegalPageSlug::from($slug)->value)->first();
        $version = $page?->currentVersion();

        if ($version === null) {
            return response()->view('public.legal-page', [
                'title' => LegalPageSlug::from($slug)->getLabel(),
                'version' => null,
            ], 404);
        }

        return response()->view('public.legal-page', [
            'title' => $version->title,
            'version' => $version,
        ]);
    }
}
