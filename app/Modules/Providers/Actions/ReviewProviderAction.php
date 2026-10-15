<?php

declare(strict_types=1);

namespace App\Modules\Providers\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Notifications\Data\LocalizedMessage;
use App\Modules\Notifications\Services\AccountNotifications;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

/**
 * قرارات الإدارة على ملف الفني — 15 §قائمة مراجعة الإدارة، BR-131، DEC-060.
 * كل قرار يسجل المسؤول والوقت، والإشعار يُرسل بعد حفظ المعاملة.
 */
final class ReviewProviderAction
{
    public function __construct(private readonly AccountNotifications $notifications) {}

    /** قائمة المراجعة البند 3: تسجيل «الهاتف موثّق» بعد الاتصال (DEC-033). */
    public function verifyPhone(ProviderProfile $profile, Admin $admin): ProviderProfile
    {
        $profile->forceFill(['phone_verified_at' => now()])->save();

        return $profile;
    }

    public function approve(ProviderProfile $profile, Admin $admin, EmploymentType $employmentType): ProviderProfile
    {
        $this->assertStatus($profile, ProviderStatus::PendingReview, Localization::text('copy.this_application_is_not_awaiting_review'));
        if ($profile->phone_verified_at === null) {
            throw BusinessRuleViolationException::rule('BR-022', Localization::text('copy.record_phone_verification_after_calling_before_approving_the_application'));
        }

        $profile->forceFill([
            'status' => ProviderStatus::Active,
            'employment_type' => $employmentType,
            'reviewed_by' => $admin->getKey(),
            'reviewed_at' => now(),
            'rejection_reason' => null,
        ])->save();
        DB::afterCommit(fn () => $this->notifications->applicationReviewed($profile->user, approved: true));

        return $profile;
    }

    public function reject(ProviderProfile $profile, Admin $admin, string $reason): ProviderProfile
    {
        $this->assertStatus($profile, ProviderStatus::PendingReview, Localization::text('copy.this_application_is_not_awaiting_review'));

        $profile->forceFill([
            'status' => ProviderStatus::Rejected,
            'reviewed_by' => $admin->getKey(),
            'reviewed_at' => now(),
            'rejection_reason' => trim($reason),
            'available_now' => false,
        ])->save();
        DB::afterCommit(fn () => $this->notifications->applicationReviewed($profile->user, approved: false, reason: trim($reason)));

        return $profile;
    }

    /** BR-131: يكمل طلباته النشطة، ولا يستقبل جديدًا، وعروضه المقدمة تُغلق. */
    public function suspend(ProviderProfile $profile, Admin $admin, string $reason): ProviderProfile
    {
        $this->assertStatus($profile, ProviderStatus::Active, Localization::text('copy.only_an_active_technician_can_be_suspended'));

        DB::transaction(function () use ($profile, $admin, $reason): void {
            $profile->forceFill([
                'status' => ProviderStatus::Suspended,
                'suspension_reason' => trim($reason),
                'available_now' => false,
                'reviewed_by' => $admin->getKey(),
                'reviewed_at' => now(),
            ])->save();
            $profile->offers()
                ->where('status', OfferStatus::Submitted->value)
                ->update(['status' => OfferStatus::Closed->value, 'decided_at' => now()]);
        });
        DB::afterCommit(fn () => $this->notifications->accountStatusChanged(
            $profile->user, new LocalizedMessage('notifications.provider_suspended_title'), new LocalizedMessage('notifications.provider_suspended_body', ['reason' => trim($reason)]),
        ));

        return $profile;
    }

    public function reactivate(ProviderProfile $profile, Admin $admin): ProviderProfile
    {
        $this->assertStatus($profile, ProviderStatus::Suspended, Localization::text('copy.only_a_suspended_technician_can_be_reactivated'));

        $profile->forceFill([
            'status' => ProviderStatus::Active,
            'suspension_reason' => null,
            'reviewed_by' => $admin->getKey(),
            'reviewed_at' => now(),
        ])->save();
        DB::afterCommit(fn () => $this->notifications->accountStatusChanged(
            $profile->user, new LocalizedMessage('notifications.provider_active_title'), new LocalizedMessage('notifications.provider_active_body'),
        ));

        return $profile;
    }

    private function assertStatus(ProviderProfile $profile, ProviderStatus $expected, string $message): void
    {
        if ($profile->status !== $expected) {
            throw BusinessRuleViolationException::rule('BR-131', $message);
        }
    }
}
