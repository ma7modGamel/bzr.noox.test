<?php

declare(strict_types=1);

namespace App\Modules\Catalog\Models;

use App\Modules\Geography\Models\City;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** BR-011، BR-012، DEC-024 — الكتالوج مستويان: فئة ← نوع مشكلة. */
final class Category extends Model
{
    use HasFactory;

    protected $fillable = ['name', 'icon_path', 'sort', 'is_active'];

    protected function casts(): array
    {
        return ['is_active' => 'bool'];
    }

    public function problemTypes(): HasMany
    {
        return $this->hasMany(ProblemType::class)->orderBy('sort');
    }

    public function cities(): BelongsToMany
    {
        return $this->belongsToMany(City::class, 'city_categories')->withPivot('is_active');
    }
}
