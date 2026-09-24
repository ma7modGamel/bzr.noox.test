<?php

declare(strict_types=1);

namespace App\Modules\Support\Models;

use App\Modules\Orders\Models\OrderMedia;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

final class DisputeAttachment extends Model
{
    protected $guarded = ['id'];

    public function dispute(): BelongsTo
    {
        return $this->belongsTo(Dispute::class);
    }

    public function media(): BelongsTo
    {
        return $this->belongsTo(OrderMedia::class, 'order_media_id');
    }
}
