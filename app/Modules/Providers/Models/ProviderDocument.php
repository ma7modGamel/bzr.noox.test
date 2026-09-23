<?php

declare(strict_types=1);

namespace App\Modules\Providers\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** مستندات الهوية — تخزين خاص، تراها الإدارة فقط (15، 32). */
final class ProviderDocument extends Model
{
    protected $fillable = ['provider_profile_id', 'type', 'path'];

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }
}
