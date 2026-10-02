<?php

declare(strict_types=1);

namespace App\Modules\Orders\Models;

use App\Modules\Notifications\Services\OrderEventObserver;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use Illuminate\Database\Eloquent\Attributes\ObservedBy;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * سجل تشغيلي خفيف — 24. يُكتب في نفس معاملة الإجراء، ولا يُعدّل ولا يُحذف.
 * إشعارات 17 تُطلق منه بعد حفظ المعاملة (DEC-058).
 */
#[ObservedBy(OrderEventObserver::class)]
final class OrderEvent extends Model
{
    public $timestamps = false;

    protected $fillable = [
        'order_id', 'event_code', 'actor_type', 'actor_id',
        'from_status', 'to_status', 'ref_type', 'ref_id', 'meta', 'created_at',
    ];

    protected function casts(): array
    {
        return [
            'event_code' => OrderEventCode::class,
            'actor_type' => ActorType::class,
            'from_status' => OrderStatus::class,
            'to_status' => OrderStatus::class,
            'meta' => 'array',
            'created_at' => 'immutable_datetime',
        ];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }
}
