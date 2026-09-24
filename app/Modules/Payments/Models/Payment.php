<?php

declare(strict_types=1);

namespace App\Modules\Payments\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** محاولة دفع — 19. محاولة PENDING واحدة لكل طلب (قيد `pending_key`). */
final class Payment extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'method' => PaymentMethod::class,
            'status' => PaymentStatus::class,
            'recorded_by_type' => ActorType::class,
            'amount' => 'decimal:2',
            'expires_at' => 'immutable_datetime',
            'paid_at' => 'immutable_datetime',
            'submitted_at' => 'immutable_datetime',
            'verified_at' => 'immutable_datetime',
        ];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function receipt(): BelongsTo
    {
        return $this->belongsTo(OrderMedia::class, 'receipt_media_id');
    }

    public function verifiedBy(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'verified_by_admin_id');
    }

    public function refunds(): HasMany
    {
        return $this->hasMany(Refund::class);
    }

    /** @return array<string, mixed> الحقول الآمنة التي تحتاجها C22؛ لا مفاتيح ولا توقيع. */
    public function mobilePayload(): array
    {
        return [
            'id' => $this->getKey(),
            'status' => $this->status->value,
            'amount' => $this->amount,
            'gateway' => $this->gateway,
            'channel' => $this->channel,
            'transfer_reference' => $this->transfer_reference,
            'submitted_at' => $this->submitted_at?->toIso8601String(),
            'failure_reason' => $this->failure_reason,
            'reference_number' => $this->fawry_reference_number,
            'checkout_url' => $this->checkout_url,
            'expires_at' => $this->expires_at?->toIso8601String(),
        ];
    }
}
