<?php

declare(strict_types=1);

namespace App\Modules\Orders\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-110..BR-112 — تُحذف عند الإغلاق/الإلغاء؛ نقطة الوصول وحدها تُحفظ. */
final class TrackingPoint extends Model
{
    public $timestamps = false;

    protected $fillable = ['order_id', 'lat', 'lng', 'recorded_at'];

    protected function casts(): array
    {
        return ['lat' => 'decimal:7', 'lng' => 'decimal:7', 'recorded_at' => 'immutable_datetime'];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }
}
