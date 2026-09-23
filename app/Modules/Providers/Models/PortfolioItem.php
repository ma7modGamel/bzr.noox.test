<?php

declare(strict_types=1);

namespace App\Modules\Providers\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** معرض الأعمال — حتى 20 صورة (15). */
final class PortfolioItem extends Model
{
    protected $fillable = ['provider_profile_id', 'image_path', 'caption', 'sort'];

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }
}
