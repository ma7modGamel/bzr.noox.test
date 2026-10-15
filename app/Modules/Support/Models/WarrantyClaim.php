<?php

declare(strict_types=1);

namespace App\Modules\Support\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Factories\WarrantyClaimFactory;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasOne;

final class WarrantyClaim extends Model
{
    /** @use HasFactory<WarrantyClaimFactory> */
    use HasFactory;

    protected $guarded = ['id'];

    protected function casts(): array
    {
        return ['scheduled_at' => 'immutable_datetime', 'assigned_at' => 'immutable_datetime'];
    }

    protected static function newFactory(): WarrantyClaimFactory
    {
        return WarrantyClaimFactory::new();
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }

    public function visit(): HasOne
    {
        return $this->hasOne(Order::class, 'warranty_claim_id');
    }

    public function assignedBy(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'assigned_by_admin_id');
    }
}
