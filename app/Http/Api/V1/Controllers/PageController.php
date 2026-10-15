<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Content\Models\LegalPageVersion;
use App\Support\Localization;
use Illuminate\Http\JsonResponse;

/**
 * DEC-051 — `GET /pages/{slug}`: النسخة السارية المنشورة من اللوحة (C34، روابط C05، شاشات الفني).
 * لا تُعاد المسودات أبدًا.
 */
final class PageController
{
    public function show(string $slug): JsonResponse
    {
        $version = $this->current(LegalPageSlug::tryFrom($slug));

        if ($version === null) {
            return new JsonResponse(['error' => ['code' => 'PAGE_NOT_PUBLISHED', 'message' => Localization::text('messages.page_not_published')]], 404);
        }

        return new JsonResponse(['data' => self::payload($version)]);
    }

    /** `GET /terms/current` — الشروط وسياسة الإلغاء معًا كما يعرضهما C34. */
    public function terms(): JsonResponse
    {
        $terms = $this->current(LegalPageSlug::Terms);

        if ($terms === null) {
            return new JsonResponse(['error' => ['code' => 'PAGE_NOT_PUBLISHED', 'message' => Localization::text('messages.page_not_published')]], 404);
        }

        $cancellation = $this->current(LegalPageSlug::Cancellation);
        $privacy = $this->current(LegalPageSlug::Privacy);

        return new JsonResponse(['data' => [
            'version' => $terms->terms_version ?? $terms->version,
            'locale' => $terms->locale,
            'title' => $terms->title,
            'body' => $terms->safeBody(),
            'effective_at' => $terms->effective_at?->toIso8601String(),
            'cancellation_policy' => $cancellation === null ? null : self::payload($cancellation),
            'privacy_policy' => $privacy === null ? null : self::payload($privacy),
        ]]);
    }

    /** @return array<string, mixed> */
    public static function payload(LegalPageVersion $version): array
    {
        return [
            'slug' => $version->page->slug->value,
            'locale' => $version->locale,
            'title' => $version->title,
            'version' => $version->version,
            'body_html' => $version->safeBody(),
            'effective_at' => $version->effective_at?->toIso8601String(),
            'web_url' => route('legal.show', ['slug' => $version->page->slug->value, 'locale' => $version->locale]),
        ];
    }

    private function current(?LegalPageSlug $slug): ?LegalPageVersion
    {
        return $slug === null ? null : LegalPage::query()->where('slug', $slug->value)->first()?->currentVersion();
    }
}
