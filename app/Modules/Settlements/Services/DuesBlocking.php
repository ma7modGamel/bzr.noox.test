<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Services;

use App\Modules\Notifications\Services\SettlementNotifications;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Support\Facades\DB;

/**
 * BR-063 — منع العروض الجديدة لمن تجاوز دينه CFG-061، ورفعه تلقائيًا عند العودة داخل الحد.
 * وضع السوق فقط (BR-065)، ولا منع ما لم يُحدَّد الحد (OD-02). NTF-24 عند بداية المنع فقط (DEC-066).
 */
final readonly class DuesBlocking
{
    public function __construct(
        private ProviderEarningsService $earnings,
        private FeatureGate $features,
        private SettingsRepository $settings,
        private SettlementNotifications $notifications,
    ) {}

    public function refresh(ProviderProfile $profile): void
    {
        $limit = $this->limit();
        $debt = max(0.0, -$this->earnings->summary($profile)['balance']);
        $blocked = $limit !== null && $debt > (float) $limit;

        if ($blocked === $profile->isBlockedByDues()) {
            return;
        }

        $profile->forceFill(['dues_blocked_at' => $blocked ? now() : null])->save();

        if ($blocked) {
            DB::afterCommit(fn () => $this->notifications->duesLimitExceeded(
                $profile->user, number_format($debt, 2, '.', ''), (string) $limit,
            ));
        }
    }

    /** @return int عدد الفنيين الذين تغيّرت حالة منعهم. */
    public function refreshAll(): int
    {
        $changed = 0;

        ProviderProfile::query()->with('user')->orderBy('id')->chunkById(100, function ($profiles) use (&$changed): void {
            foreach ($profiles as $profile) {
                $before = $profile->isBlockedByDues();
                $this->refresh($profile);
                $changed += (int) ($before !== $profile->isBlockedByDues());
            }
        });

        return $changed;
    }

    /** الحد بالجنيه، أو null حين لا ينطبق المنع (وضع الموظفين أو CFG-061 غير محدد). */
    public function limit(): ?string
    {
        if ($this->features->mode() === OperatingMode::Employee) {
            return null;
        }

        return $this->settings->decimal(Cfg::ProviderDebtLimit);
    }
}
