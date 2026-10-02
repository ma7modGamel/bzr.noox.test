<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Models;

use App\Modules\Identity\Models\User;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/**
 * رمز FCM لجهاز (29). Push يصل لكل رموز المستخدم؛ `app_mode` آخر وضع سجّل منه فقط (DEC-058).
 */
final class DeviceToken extends Model
{
    public const PLATFORMS = ['ANDROID', 'IOS'];

    protected $fillable = ['user_id', 'token', 'platform', 'app_mode', 'last_used_at'];

    protected $hidden = ['token'];

    protected function casts(): array
    {
        return ['last_used_at' => 'immutable_datetime'];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    /** آخر 6 أحرف فقط للسجلات (32). */
    public function redacted(): string
    {
        return '…'.substr($this->token, -6);
    }
}
