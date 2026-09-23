<?php

declare(strict_types=1);

namespace App\Modules\Communication\Models;

use App\Modules\Identity\Models\User;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-100 — الحجب يستبدل المطابقات بـ ••• ويحفظ الأصل مشفرًا للإدارة عند النزاع فقط. */
final class Message extends Model
{
    protected $guarded = ['id'];

    protected $hidden = ['original_body_encrypted'];

    protected function casts(): array
    {
        return [
            'was_masked' => 'bool',
            'original_body_encrypted' => 'encrypted',
            'read_at' => 'immutable_datetime',
        ];
    }

    public function conversation(): BelongsTo
    {
        return $this->belongsTo(Conversation::class);
    }

    public function sender(): BelongsTo
    {
        return $this->belongsTo(User::class, 'sender_user_id');
    }
}
