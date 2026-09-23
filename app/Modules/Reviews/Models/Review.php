<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-090 — ثلاثة أبعاد + نص اختياري، غير قابل للتعديل. */
final class Review extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return ['hidden_at' => 'immutable_datetime'];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }

    public function customer(): BelongsTo
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    public function hiddenBy(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'hidden_by');
    }

    /** المخفي يُستبعد من المتوسط (BR-092). */
    public function scopeVisible(Builder $query): void
    {
        $query->whereNull('hidden_at');
    }

    public function average(): float
    {
        return round(($this->quality + $this->punctuality + $this->conduct) / 3, 2);
    }
}
