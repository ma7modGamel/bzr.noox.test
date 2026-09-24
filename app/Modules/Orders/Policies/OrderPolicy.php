<?php

declare(strict_types=1);

namespace App\Modules\Orders\Policies;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use Illuminate\Contracts\Auth\Authenticatable;

/**
 * ملكية الطلب ورؤيته — 23-PERMISSIONS-MATRIX.
 *
 * قاعدة الملكية (23): المورد غير المملوك يعيد `404` لا `403`، لعدم كشف وجوده.
 * الطبقة التي تستدعي هذه السياسة هي من تترجم `false` إلى 404.
 */
final class OrderPolicy
{
    /**
     * الإدارة خارج قاعدة الملكية: صلاحياتها بالدور لا بامتلاك المورد (23، DEC-021).
     * فصل الحسابين (03) يجعل هذا الفحص صريحًا لا ضمنيًا.
     */
    public function before(Authenticatable $user, string $ability): ?bool
    {
        return $user instanceof Admin ? true : null;
    }

    /** العميل صاحب الطلب، أو الفني المسنَد إليه. */
    public function view(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order) || $this->isAssignedProvider($user, $order);
    }

    /** BR-021 / DEC-031 — التعديل في `OPEN` وقبل وصول أي عرض. */
    public function update(User $user, Order $order): bool
    {
        if (! $this->isCustomer($user, $order) || $order->status !== OrderStatus::Open) {
            return false;
        }

        return $order->offers()->count() === 0;
    }

    /** BR-070 — العميل يلغي مجانًا حتى "في الطريق"؛ بعد الوصول "عندي مشكلة" فقط. */
    public function cancel(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order)
            && in_array($order->status, [OrderStatus::Open, OrderStatus::Confirmed, OrderStatus::OnTheWay], true);
    }

    /** BR-034 — قبول العرض للعميل صاحب الطلب وحده. */
    public function acceptOffer(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order) && $order->status === OrderStatus::Open;
    }

    /** إجراءات التنفيذ للفني المسنَد وحده. */
    public function perform(User $user, Order $order): bool
    {
        return $this->isAssignedProvider($user, $order);
    }

    /** BR-050 — تبديل طريقة الدفع متاح حتى يتم الدفع. */
    public function pay(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order)
            && in_array($order->status, [
                OrderStatus::Confirmed, OrderStatus::OnTheWay, OrderStatus::Arrived,
                OrderStatus::AwaitingQuoteApproval, OrderStatus::InProgress, OrderStatus::AwaitingPayment,
            ], true);
    }

    /** موافقة/رفض مقترح السعر للعميل وحده (23). */
    public function decideProposal(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order);
    }

    /** BR-120 — فتح مشكلة متاح للطرفين من `ARRIVED`. */
    public function openDispute(User $user, Order $order): bool
    {
        if (! $this->view($user, $order)) {
            return false;
        }

        return $order->status->allowsDispute() || $order->status === OrderStatus::Closed;
    }

    /** BR-090 — العميل يقيّم بعد الإغلاق. */
    public function review(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order) && $order->status === OrderStatus::Closed;
    }

    /** BR-110 — التتبع يراه العميل صاحب الطلب فقط (DEC-030). */
    public function track(User $user, Order $order): bool
    {
        return $this->isCustomer($user, $order);
    }

    private function isCustomer(User $user, Order $order): bool
    {
        return $order->customer_id === $user->getKey();
    }

    private function isAssignedProvider(User $user, Order $order): bool
    {
        $profileId = $user->providerProfile?->getKey();

        return $profileId !== null && $order->provider_profile_id === $profileId;
    }
}
