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

    protected $fillable = ['category_id', 'name', 'is_other', 'sort', 'is_active'];

    protected function casts(): array
    {
        return ['is_other' => 'bool', 'is_active' => 'bool'];
    }

    public function category(): BelongsTo
    {
        return $this->belongsTo(Category::class);
    }
}
