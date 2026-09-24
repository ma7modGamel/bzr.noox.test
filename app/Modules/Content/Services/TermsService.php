<?php

declare(strict_types=1);

namespace App\Modules\Content\Services;

use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Identity\Models\User;
use Illuminate\Support\Facades\DB;

/** BR-018 — النسخة السارية من الشروط وحاجة العميل لموافقة جديدة. */
final class TermsService
{
    /** آخر نسخة شروط بلغ تاريخ سريانها (terms_versions)، أو 1 قبل أول نشر. */
    public function currentVersion(): int
    {
        return (int) (DB::table('terms_versions')->where('effective_at', '<=', now())->max('version') ?? 1);
    }

    public function acceptanceRequired(User $user): bool
    {
        return $user->accepted_terms_version === null || $user->accepted_terms_version < $this->currentVersion();
    }

    public function recordAcceptance(User $user, int $version): void
    {
        $user->forceFill(['accepted_terms_version' => $version, 'terms_accepted_at' => now()])->save();
    }

    public function page(LegalPageSlug $slug): ?LegalPage
    {
        return LegalPage::query()->where('slug', $slug->value)->first();
    }
}
