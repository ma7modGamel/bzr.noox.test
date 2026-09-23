<?php

declare(strict_types=1);

namespace App\Modules\Geography\Models;

use App\Modules\Catalog\Models\Category;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsToMany;
use Illuminate\Database\Eloquent\Relations\HasMany;

/** 20 — لا أسماء مدن في الكود؛ التوسع إعداد فقط. */
final class City extends Model
{
    use HasFactory;

    protected $fillable = ['name', 'is_active', 'timezone', 'center_lat', 'center_lng', 'radius_km'];

    protected function casts(): array
    {
        return ['is_active' => 'bool', 'center_lat' => 'decimal:7', 'center_lng' => 'decimal:7'];
    }

    public function areas(): HasMany
    {
        return $this->hasMany(Area::class);
    }

    public function categories(): BelongsToMany
    {
        return $this->belongsToMany(Category::class, 'city_categories')->withPivot('is_active');
    }
}
