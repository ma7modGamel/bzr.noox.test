<?php

declare(strict_types=1);

namespace App\Modules\Content\Actions;

use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPageVersion;
use App\Modules\Identity\Models\Admin;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Support\Facades\DB;

/**
 * DEC-051 — نشر نسخة صفحة (المدير العام). نشر نسخة شروط يُنشئ terms_version جديدًا (BR-018):
 * العملاء الذين وافقوا على نسخة أقدم يُطلب منهم الموافقة عند أول طلب بعد سريانها.
 */
final class PublishLegalPageVersionAction
{
    public function execute(LegalPageVersion $version, Admin $admin): LegalPageVersion
    {
        if (! $admin->isSuper()) {
            throw new AuthorizationException('نشر الصفحات للمدير العام فقط.');
        }

        return DB::transaction(function () use ($version, $admin): LegalPageVersion {
            $fresh = LegalPageVersion::query()->lockForUpdate()->findOrFail($version->getKey());

            if ($fresh->status === LegalPageStatus::Published) {
                throw BusinessRuleViolationException::rule('DEC-051', 'هذه النسخة منشورة بالفعل.');
            }

            $effectiveAt = $fresh->effective_at ?? now();
            $termsVersion = null;

            if ($fresh->page->slug === LegalPageSlug::Terms) {
                $termsVersion = (int) (DB::table('terms_versions')->lockForUpdate()->max('version') ?? 0) + 1;
                DB::table('terms_versions')->insert([
                    'version' => $termsVersion,
                    'body' => $fresh->safeBody(),
                    'effective_at' => $effectiveAt,
                    'created_at' => now(),
                    'updated_at' => now(),
                ]);
            }

            $fresh->forceFill([
                'status' => LegalPageStatus::Published,
                'effective_at' => $effectiveAt,
                'requires_legal_review' => false,
                'terms_version' => $termsVersion,
                'published_at' => now(),
                'published_by' => $admin->getKey(),
            ])->save();

            return $fresh;
        });
    }
}
