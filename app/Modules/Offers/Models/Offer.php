<?php

declare(strict_types=1);

namespace App\Modules\Offers\Models;

use App\Modules\Offers\Enums\OfferSource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * العرض — 09. ويحمل نفس الجدول صف **التعيين الإداري** في وضع الموظفين (BR-007):
 * حالته ACCEPTED وسعره 0.00 ومصدره ADMIN_ASSIGNMENT، ولا تسري عليه O-01..O-07.
 */
final class Offer extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'source' => OfferSource::class,
            'status' => OfferStatus::class,
            'price' => 'decimal:2',
            'inspection_fee_deductible' => 'bool',
            'submitted_at' => 'immutable_datetime',
            'withdrawn_at' => 'immutable_datetime',
            'decided_at' => 'immutable_datetime',
        ];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }

    public function previousOffer(): BelongsTo
    {
        return $this->belongsTo(self::class, 'previous_offer_id');
    }

    public function isAssignment(): bool
    {
        return $this->source->isAssignment();
    }

    /** لا يظهر صف التعيين لأي طرف كعرض (BR-007). */
    public function scopeVisible(Builder $query): void
    {
        $query->where('source', OfferSource::Provider->value);
    }
}
