<?php

declare(strict_types=1);

namespace App\Modules\Support\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

final class DisputeAttachment extends Model
{
    protected $guarded = ['id'];

    public function dispute(): BelongsTo
    {
        return $this->belongsTo(Dispute::class);
    }
}
