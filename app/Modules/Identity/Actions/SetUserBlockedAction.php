<?php

declare(strict_types=1);

namespace App\Modules\Identity\Actions;

use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Services\AccountNotifications;
use App\Modules\Orders\Actions\CancelOrderAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Support\Exceptions\DomainException;
use Illuminate\Support\Facades\DB;

/**
 * حظر المستخدم ورفعه — BR-003، EC-18، DEC-060.
 * الحظر يلغي رموز الدخول فورًا ويلغي الطلبات المفتوحة إداريًا؛ المؤكدة تقررها الإدارة.
 */
final class SetUserBlockedAction
{
    public function __construct(
        private readonly CancelOrderAction $cancel,
        private readonly AccountNotifications $notifications,
    ) {}

    /** @return int عدد الطلبات المفتوحة التي أُلغيت. */
    public function block(User $user, Admin $admin, string $reason): int
    {
        DB::transaction(function () use ($user, $reason): void {
            $user->forceFill(['status' => UserStatus::Blocked, 'blocked_reason' => trim($reason)])->save();
            $user->tokens()->delete();
            DB::table('device_tokens')->where('user_id', $user->getKey())->delete();
        });

        $cancelled = 0;
        Order::query()
            ->where('customer_id', $user->getKey())
            ->where('status', OrderStatus::Open->value)
            ->get()
            ->each(function (Order $order) use ($admin, &$cancelled): void {
                try {
                    $this->cancel->execute($order, ActorType::Admin, $admin->getKey(), CancelReason::AccountBlocked, 'حظر الحساب (EC-18)');
                    $cancelled++;
                } catch (DomainException) {
                    // تغيّرت حالة الطلب قبل الإلغاء؛ يبقى لقرار الإدارة.
                }
            });

        $this->notifications->accountStatusChanged(
            $user, 'تم إيقاف حسابك', 'السبب: '.trim($reason).' للاستفسار راسل الدعم على '.config('mail.support_address').'.',
        );

        return $cancelled;
    }

    public function unblock(User $user, Admin $admin): void
    {
        $user->forceFill(['status' => UserStatus::Active, 'blocked_reason' => null])->save();
        $this->notifications->accountStatusChanged($user, 'أُعيد تفعيل حسابك', 'يمكنك تسجيل الدخول واستخدام التطبيق من جديد.');
    }
}
