<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * سداد من مقدم الخدمة للمنصة.
 * وضع السوق: سداد العمولات (BR-063). وضع الموظفين: التوريد اليومي للنقدية (BR-065، CFG-094).
 */
final class ProviderRemittance extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return ['amount' => 'decimal:2', 'received_at' => 'immutable_datetime'];
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
