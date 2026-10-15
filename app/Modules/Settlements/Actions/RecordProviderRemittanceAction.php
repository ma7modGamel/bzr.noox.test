<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Providers\Enums\PayoutMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settlements\Models\ProviderRemittance;
use App\Modules\Settlements\Services\DuesBlocking;
use App\Modules\Settlements\Services\ProviderEarningsService;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use DateTimeInterface;
use Illuminate\Support\Facades\DB;

/**
 * سداد الفني للمنصة (BR-063) أو توريد الموظف لنقدية التحصيل (BR-065، CFG-094).
 * ما يزيد على الدين الحالي يحتاج تأكيدًا صريحًا؛ ثم يُعاد تقييم المنع فورًا (DEC-066).
 */
final readonly class RecordProviderRemittanceAction
{
    public function __construct(
        private ProviderEarningsService $earnings,
        private DuesBlocking $dues,
    ) {}

    public function execute(
        ProviderProfile $profile,
        Admin $admin,
        string $amount,
        PayoutMethod $method,
        string $reference,
        DateTimeInterface $receivedAt,
        bool $allowAboveDebt = false,
        ?string $note = null,
    ): ProviderRemittance {
        RecordGuard::superAdmin($admin);
        $amount = RecordGuard::positiveAmount($amount);
        $reference = RecordGuard::reference($reference);
        RecordGuard::notInFuture($receivedAt);

        return DB::transaction(function () use ($profile, $admin, $amount, $method, $reference, $receivedAt, $allowAboveDebt, $note): ProviderRemittance {
            ProviderProfile::query()->whereKey($profile->getKey())->lockForUpdate()->first();
            $debt = $this->debt($profile);

            if (! $allowAboveDebt && bccomp($amount, $debt, 2) > 0) {
                throw BusinessRuleViolationException::rule(
                    'BR-063', Localization::text('dynamic.remittance_due', ['amount' => $debt]),
                );
            }

            $remittance = $profile->remittances()->create([
                'amount' => $amount,
                'method' => $method->value,
                'reference' => $reference,
                'received_at' => $receivedAt,
                'admin_id' => $admin->getKey(),
                'note' => RecordGuard::note($note),
            ]);

            $this->dues->refresh($profile);

            return $remittance;
        });
    }

    /** الدين الحالي (موجب)، أو صفر إن لم يكن الفني مدينًا. */
    public function debt(ProviderProfile $profile): string
    {
        return number_format(max(0.0, -$this->earnings->summary($profile)['balance']), 2, '.', '');
    }
}
