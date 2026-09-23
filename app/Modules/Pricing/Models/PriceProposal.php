<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Models;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** مقترح السعر — 12، BR-040..BR-045. مقترح معلّق واحد فقط على الطلب. */
final class PriceProposal extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'type' => ProposalType::class,
            'status' => ProposalStatus::class,
            'decided_by_type' => ActorType::class,
            'amount' => 'decimal:2',
            'expires_at' => 'immutable_datetime',
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

    public function isPending(): bool
    {
        return $this->status === ProposalStatus::Pending;
    }
}
