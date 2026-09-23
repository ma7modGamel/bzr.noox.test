<?php

declare(strict_types=1);

namespace App\Modules\Customers\Models;

use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Identity\Models\User;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\SoftDeletes;

/** 20 — الطلب يحفظ نسخة ثابتة من هذه البيانات لا مرجعًا حيًا (28). */
final class CustomerAddress extends Model
{
    use HasFactory;
    use SoftDeletes;

    protected $fillable = [
        'user_id', 'label', 'city_id', 'area_id', 'address_text',
        'building', 'floor', 'apartment', 'landmark', 'lat', 'lng', 'is_default',
    ];

    protected function casts(): array
    {
        return ['is_default' => 'bool', 'lat' => 'decimal:7', 'lng' => 'decimal:7'];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function city(): BelongsTo
    {
        return $this->belongsTo(City::class);
    }

    public function area(): BelongsTo
    {
        return $this->belongsTo(Area::class);
    }
}
