<?php

declare(strict_types=1);

namespace App\Modules\Content\Models;

use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Identity\Models\Admin;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

final class LegalPageVersion extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return [
            'status' => LegalPageStatus::class,
            'requires_legal_review' => 'boolean',
            'effective_at' => 'immutable_datetime',
            'published_at' => 'immutable_datetime',
        ];
    }

    public function page(): BelongsTo
    {
        return $this->belongsTo(LegalPage::class, 'legal_page_id');
    }

    public function publisher(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'published_by');
    }

    /** HTML المحرر بعد إزالة ما ليس نصًا منسقًا (سكربتات، أحداث، إطارات). */
    public function safeBody(): string
    {
        $allowed = '<p><br><h2><h3><h4><ul><ol><li><strong><b><em><i><u><a><blockquote><hr>';
        $html = strip_tags((string) $this->body, $allowed);

        return (string) preg_replace('/\s(on\w+|style)\s*=\s*("[^"]*"|\'[^\']*\'|[^\s>]+)/i', '', $html);
    }
}
