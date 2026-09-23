<?php

declare(strict_types=1);

namespace App\Modules\Settings\Models;

use App\Modules\Identity\Models\Admin;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * جدول `settings` — 29-DATABASE-DESIGN. القراءة دائمًا عبر SettingsRepository (مخزَّن مؤقتًا).
 *
 * @property string $key
 * @property mixed $value
 */
final class Setting extends Model
{
    protected $primaryKey = 'key';

    public $incrementing = false;

    protected $keyType = 'string';

    public const UPDATED_AT = 'updated_at';

    public const CREATED_AT = null;

    protected $fillable = ['key', 'value', 'updated_by'];

    protected function casts(): array
    {
        return [
            'value' => 'array',
            'updated_at' => 'immutable_datetime',
        ];
    }

    public function updatedBy(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'updated_by');
    }
}
