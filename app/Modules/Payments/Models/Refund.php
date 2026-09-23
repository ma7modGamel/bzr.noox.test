<?php

declare(strict_types=1);

namespace App\Modules\Payments\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Models\Order;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** DEC-019 — بقرار الإدارة داخل نزاع فقط، وللمدفوعات الإلكترونية فقط. */
final class Refund extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'amount' => 'decimal:2',
            'labor_amount' => 'decimal:2',
            'materials_amount' => 'decimal:2',
        ];
    }

    public function order(): BelongsTo
    {
        return $this->belongsTo(Order::class);
    }

    public function payment(): BelongsTo
    {
        return $this->belongsTo(Payment::class);
    }

    public function admin(): BelongsTo
    {
        return $this->belongsTo(Admin::class);
    }
}
