<?php

declare(strict_types=1);

namespace App\Modules\Content\Models;

use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Enums\LegalPageStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** صفحة قانونية بسجل نسخ (DEC-051). */
final class LegalPage extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return ['slug' => LegalPageSlug::class];
    }

    public function versions(): HasMany
    {
        return $this->hasMany(LegalPageVersion::class)->orderByDesc('version');
    }

    /** النسخة السارية: آخر نسخة منشورة بلغ تاريخ سريانها. */
    public function currentVersion(): ?LegalPageVersion
    {
        return $this->hasMany(LegalPageVersion::class)
            ->where('status', LegalPageStatus::Published->value)
            ->where('effective_at', '<=', now())
            ->orderByDesc('effective_at')
            ->orderByDesc('version')
            ->first();
    }
}
