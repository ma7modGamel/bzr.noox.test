<?php

declare(strict_types=1);

namespace App\Modules\Support\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** BR-120 — قبل الإغلاق يحوّل الطلب إلى DISPUTED؛ بعده ملف يوقف التسوية بلا تغيير حالة. */
final class Dispute extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'opened_by_type' => ActorType::class,
            'status' => DisputeStatus::class,
            'resolution' => DisputeResolution::class,
            'is_post_close' => 'bool',
            'resolved_at' => 'immutable_datetime',
        ];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function attachments(): HasMany
    {
        return $this->hasMany(DisputeAttachment::class);
    }

    public function resolvedBy(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'resolved_by');
    }
}
