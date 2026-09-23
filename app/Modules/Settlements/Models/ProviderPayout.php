<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * تحويل من المنصة لمقدم الخدمة.
 * وضع السوق: مستحقاته الأسبوعية (BR-064). وضع الموظفين: رد قيمة الخامات (BR-065).
 */
final class ProviderPayout extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return ['amount' => 'decimal:2', 'paid_at' => 'immutable_datetime'];
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }

    public function admin(): BelongsTo
    {
        return $this->belongsTo(Admin::class);
    }
}
