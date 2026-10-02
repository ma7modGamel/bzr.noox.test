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
        'name',
        'is_other',
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
            'employee_price_min' => 'decimal:2',
            'employee_price_max' => 'decimal:2',
            'is_active' => 'bool',
        ];
    }

    public function category(): BelongsTo
    {
        return $this->belongsTo(Category::class);
    }
}
