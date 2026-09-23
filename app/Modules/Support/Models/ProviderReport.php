<?php

declare(strict_types=1);

namespace App\Modules\Support\Models;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Enums\ReportStatus;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

/** BR-121 — ملف مستقل للإدارة لا يغير أي حالة. */
final class ProviderReport extends Model
{
    protected $guarded = ['id'];

    protected function casts(): array
    {
        return ['status' => ReportStatus::class, 'reviewed_at' => 'immutable_datetime'];
    }

    public function reporter(): BelongsTo
    {
        return $this->belongsTo(User::class, 'reporter_user_id');
    }

    public function providerProfile(): BelongsTo
    {
        return $this->belongsTo(ProviderProfile::class);
    }

    public function reviewedBy(): BelongsTo
    {
        return $this->belongsTo(Admin::class, 'reviewed_by');
    }
}
