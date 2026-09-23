<?php

declare(strict_types=1);

namespace App\Modules\Orders\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-014 — حتى 5 صور، فيديو واحد، تسجيل صوتي واحد. */
final class OrderMedia extends Model
{
    protected $table = 'order_media';

    protected $fillable = ['order_id', 'uploaded_by', 'expires_at', 'type', 'path', 'size_bytes', 'duration_sec'];

    protected function casts(): array
    {
        return ['expires_at' => 'immutable_datetime'];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }
}
