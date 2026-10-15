<?php

declare(strict_types=1);

namespace App\Modules\Catalog\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-012 — لكل فئة نوع "مشكلة أخرى" يجعل الوصف إلزاميًا (BR-013). */
final class ProblemType extends Model
{
    use HasFactory;

    protected $fillable = [
        'category_id',
        'name', 'name_en',
        'is_other',
        'customer_price_min', 'customer_price_max',
        'employee_price_min',
        'employee_price_max',
        'employee_price_notes',
        'sort',
        'is_active',
    ];

    protected function casts(): array
    {
        return [
            'is_other' => 'bool',
            'customer_price_min' => 'decimal:2',
            'customer_price_max' => 'decimal:2',
            'employee_price_min' => 'decimal:2',
            'employee_price_max' => 'decimal:2',
            'is_active' => 'bool',
        ];
    }

    /** @return array{minimum: string, maximum: string, suggested_amounts: list<string>}|null */
    public function customerPriceGuide(): ?array
    {
        if ($this->customer_price_min === null || $this->customer_price_max === null
            || (float) $this->customer_price_min <= 0 || (float) $this->customer_price_max < (float) $this->customer_price_min) {
            return null;
        }

        $middle = number_format(((float) $this->customer_price_min + (float) $this->customer_price_max) / 2, 2, '.', '');

        return ['minimum' => $this->customer_price_min, 'maximum' => $this->customer_price_max,
            'suggested_amounts' => array_values(array_unique([$this->customer_price_min, $middle, $this->customer_price_max]))];
    }

    public function category(): BelongsTo
    {
        return $this->belongsTo(Category::class);
    }
}
