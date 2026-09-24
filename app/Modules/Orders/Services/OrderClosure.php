<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\TrackingPoint;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;

/**
 * آثار الدخول إلى CLOSED (10 §آثار جانبية): حساب العمولة (BR-060)،
 * تحديد موعد قابلية التسوية (BR-061)، وحذف ما تبقى من نقاط المسار (BR-112).
 *
 * تستدعيها كل الإجراءات التي تُغلق طلبًا، فيبقى الإغلاق موحّدًا مهما كان مساره.
 */
final readonly class OrderClosure
{
    public function __construct(private SettingsRepository $settings) {}

    /** يُطبَّق داخل معاملة آلة الحالات على الصف المقفول. */
    public function apply(Order $order, bool $withCommission = true): void
    {
        $closedAt = now();

        $order->closed_at = $closedAt;
        $order->settlement_eligible_at = $closedAt->copy()
            ->addHours($this->settings->int(Cfg::DisputeWindowHours));

        // BR-055 / BR-056 — الإغلاق بلا دفع أو بمبلغ صفر لا عمولة عليه
        $order->commission_amount = $withCommission
            ? OrderAmounts::for($order)->commission($order)
            : '0.00';

        TrackingPoint::query()->where('order_id', $order->getKey())->delete();
    }
}
