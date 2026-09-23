<?php

declare(strict_types=1);

namespace App\Modules\Geography\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-010 — عنوان الطلب يجب أن يقع في منطقة مفعّلة في مدينة مفعّلة. */
final class Area extends Model
{
    use HasFactory;

    protected $fillable = ['city_id', 'name', 'is_active', 'sort'];

    protected function casts(): array
    {
        return ['is_active' => 'bool'];
    }

    public function city(): BelongsTo
    {
        return $this->belongsTo(City::class);
    }
}
