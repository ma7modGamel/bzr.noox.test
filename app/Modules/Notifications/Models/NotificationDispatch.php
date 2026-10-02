<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Models;

use Illuminate\Database\Eloquent\Model;

/**
 * مفتاح فريد يمنع تكرار NTF-07 وNTF-18، ويحدد نوافذ تجميع NTF-04 (17 §التوقيت).
 */
final class NotificationDispatch extends Model
{
    public const UPDATED_AT = null;

    protected $fillable = ['user_id', 'code', 'order_id', 'key', 'notification_id'];

    protected function casts(): array
    {
        return ['created_at' => 'immutable_datetime'];
    }
}
