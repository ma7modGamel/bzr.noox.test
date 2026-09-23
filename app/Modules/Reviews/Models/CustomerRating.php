<?php

declare(strict_types=1);

namespace App\Modules\Reviews\Models;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-091 — الفني يقيّم العميل بالنجوم فقط، مرة واحدة. */
final class CustomerRating extends Model
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

    public function customer(): BelongsTo
    {
        return $this->belongsTo(User::class, 'customer_id');
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }
}
