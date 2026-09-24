<?php

declare(strict_types=1);

namespace App\Modules\Support\Models;

use App\Modules\Support\Enums\SupportReasonType;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Database\Eloquent\Model;

/** سبب دعم قابل للإدارة؛ التطبيقان يستهلكان الكود والتسمية من GET /config. */
final class SupportReason extends Model
{
    protected $fillable = ['type', 'code', 'label', 'is_active', 'sort'];

    protected function casts(): array
    {
        return [
            'type' => SupportReasonType::class,
            'is_active' => 'bool',
            'sort' => 'integer',
        ];
    }

    /** @param Builder<self> $query */
    public function scopeForType(Builder $query, SupportReasonType $type): void
    {
        $query->where('type', $type->value);
    }

    /** @param Builder<self> $query */
    public function scopeActive(Builder $query): void
    {
        $query->where('is_active', true);
    }

    /** @return array{code: string, label: string} */
    public function toOption(): array
    {
        return ['code' => $this->code, 'label' => $this->label];
    }
}
