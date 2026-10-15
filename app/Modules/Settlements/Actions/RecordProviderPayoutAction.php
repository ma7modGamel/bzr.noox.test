<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Notifications\Services\SettlementNotifications;
use App\Modules\Providers\Enums\PayoutMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settlements\Models\ProviderPayout;
use App\Modules\Settlements\Services\ProviderEarningsService;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use DateTimeInterface;
use Illuminate\Support\Facades\DB;

/**
 * BR-064 — تحويل يدوي للفني برقم مرجعي، لا يتجاوز الرصيد المتاح.
 * في وضع الموظفين هو رد قيمة الخامات (BR-065). NTF-23 بعد الحفظ (DEC-066).
 */
final readonly class RecordProviderPayoutAction
{
    public function __construct(
        private ProviderEarningsService $earnings,
        private SettlementNotifications $notifications,
    ) {}

    public function execute(
        ProviderProfile $profile,
        Admin $admin,
        string $amount,
        PayoutMethod $method,
        string $reference,
        DateTimeInterface $paidAt,
        ?string $note = null,
    ): ProviderPayout {
        RecordGuard::superAdmin($admin);
        $amount = RecordGuard::positiveAmount($amount);
        $reference = RecordGuard::reference($reference);
        RecordGuard::notInFuture($paidAt);

        return DB::transaction(function () use ($profile, $admin, $amount, $method, $reference, $paidAt, $note): ProviderPayout {
            ProviderProfile::query()->whereKey($profile->getKey())->lockForUpdate()->first();
            $available = number_format($this->earnings->summary($profile)['available'], 2, '.', '');

            if (bccomp($amount, $available, 2) > 0) {
                throw BusinessRuleViolationException::rule('BR-064', Localization::text('dynamic.payout_available', ['amount' => $available]));
            }

            $payout = $profile->payouts()->create([
                'amount' => $amount,
                'method' => $method->value,
                'reference' => $reference,
                'paid_at' => $paidAt,
                'admin_id' => $admin->getKey(),
                'note' => RecordGuard::note($note),
            ]);

            DB::afterCommit(fn () => $this->notifications->payoutSent($profile->user, $amount, $reference));

            return $payout;
        });
    }
}
