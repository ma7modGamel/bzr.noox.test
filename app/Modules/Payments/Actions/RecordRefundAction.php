<?php

declare(strict_types=1);

namespace App\Modules\Payments\Actions;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Models\Refund;
use App\Modules\Settlements\Actions\RecordGuard;
use App\Modules\Settlements\Services\DuesBlocking;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Support\Facades\DB;

/**
 * DEC-019، DEC-066 — المدير العام يسجل استردادًا نفّذه في البوابة/البنك، بعد حسم النزاع بنتيجة استرداد،
 * للدفع الإلكتروني الناجح فقط. يحدّث مبالغ الطلب وحالة دفعه ويعيد حساب العمولة (BR-060)، ويسجل EVT-082.
 */
final readonly class RecordRefundAction
{
    /** @var list<DisputeResolution> */
    public const REFUND_RESOLUTIONS = [DisputeResolution::CloseWithRefund, DisputeResolution::CancelFullRefund];

    public function __construct(private DuesBlocking $dues) {}

    public function execute(
        Dispute $dispute,
        Admin $admin,
        string $laborAmount,
        string $materialsAmount,
        string $reference,
        string $reason,
    ): Refund {
        RecordGuard::superAdmin($admin);
        $reference = RecordGuard::reference($reference);
        $reason = trim($reason);

        if (mb_strlen($reason) < 5) {
            throw BusinessRuleViolationException::rule('DEC-019', Localization::text('copy.a_refund_reason_is_required'));
        }

        if ($dispute->status !== DisputeStatus::Resolved || ! in_array($dispute->resolution, self::REFUND_RESOLUTIONS, true)) {
            throw BusinessRuleViolationException::rule('DEC-019', Localization::text('copy.a_refund_can_only_be_recorded_after_a_dispute'));
        }

        $labor = $this->amount($laborAmount);
        $materials = $this->amount($materialsAmount);
        $total = bcadd($labor, $materials, 2);

        if (bccomp($total, '0.00', 2) <= 0) {
            throw BusinessRuleViolationException::rule('DEC-019', Localization::text('copy.the_refund_amount_must_be_greater_than_zero'));
        }

        $refund = DB::transaction(function () use ($dispute, $admin, $labor, $materials, $total, $reference, $reason): Refund {
            /** @var Order $order */
            $order = Order::query()->whereKey($dispute->order_id)->lockForUpdate()->firstOrFail();
            $payment = $this->payment($order);
            $remaining = $this->remaining($order, $payment);

            if (bccomp($labor, $remaining['labor'], 2) > 0) {
                throw BusinessRuleViolationException::rule('BR-060', Localization::text('dynamic.refund_labor', ['amount' => $remaining['labor']]));
            }

            if (bccomp($materials, $remaining['materials'], 2) > 0) {
                throw BusinessRuleViolationException::rule('BR-060', Localization::text('dynamic.refund_materials', ['amount' => $remaining['materials']]));
            }

            if (bccomp($total, $remaining['total'], 2) > 0) {
                throw BusinessRuleViolationException::rule('DEC-019', Localization::text('dynamic.refund_total', ['amount' => $remaining['total']]));
            }

            if ($dispute->resolution === DisputeResolution::CancelFullRefund && bccomp($total, $remaining['total'], 2) !== 0) {
                throw BusinessRuleViolationException::rule('DEC-019', Localization::text('dynamic.refund_full', ['amount' => $remaining['total']]));
            }

            $refund = $payment->refunds()->create([
                'order_id' => $order->getKey(),
                'amount' => $total,
                'labor_amount' => $labor,
                'materials_amount' => $materials,
                'gateway_reference' => $reference,
                'reason' => mb_substr($reason, 0, 500),
                'admin_id' => $admin->getKey(),
            ]);

            $order->labor_refunded = bcadd($this->money($order->labor_refunded), $labor, 2);
            $order->refunded_total = bcadd($this->money($order->refunded_total), $total, 2);
            $order->payment_status = bccomp($order->refunded_total, $this->money($payment->amount), 2) >= 0
                ? OrderPaymentStatus::Refunded
                : OrderPaymentStatus::PartiallyRefunded;

            // BR-060 — العمولة على المصنعية بعد المسترد، للطلب المغلق بعمولة.
            if ($order->status === OrderStatus::Closed && bccomp($this->money($order->commission_amount), '0.00', 2) > 0) {
                $base = $this->floor(bcsub($this->money($order->labor_total), $order->labor_refunded, 2));
                $order->commission_amount = bcmul((string) ($order->commission_rate ?? '0'), $base, 2);
            }

            $order->save();

            OrderEvent::query()->create([
                'order_id' => $order->getKey(),
                'event_code' => OrderEventCode::RefundRecorded,
                'actor_type' => ActorType::Admin,
                'actor_id' => $admin->getKey(),
                'from_status' => $order->status,
                'to_status' => $order->status,
                'ref_type' => 'refund',
                'ref_id' => $refund->getKey(),
                'meta' => ['amount' => $total, 'labor' => $labor, 'materials' => $materials, 'reference' => $reference],
                'created_at' => now(),
            ]);

            if ($order->providerProfile !== null) {
                $this->dues->refresh($order->providerProfile);
            }

            return $refund;
        });

        return $refund;
    }

    /**
     * ما يمكن استرداده بعد ما سبق: المصنعية، الخامات، والإجمالي من المبلغ المدفوع.
     *
     * @return array{labor: string, materials: string, total: string}
     */
    public function remaining(Order $order, ?Payment $payment = null): array
    {
        $payment ??= $this->payment($order);
        $laborRefunded = $this->money($order->labor_refunded);
        $materialsRefunded = bcsub($this->money($order->refunded_total), $laborRefunded, 2);

        return [
            'labor' => $this->floor(bcsub($this->money($order->labor_total), $laborRefunded, 2)),
            'materials' => $this->floor(bcsub($this->money($order->materials_total), $materialsRefunded, 2)),
            'total' => $this->floor(bcsub($this->money($payment->amount), $this->money($order->refunded_total), 2)),
        ];
    }

    /** المحاولة الإلكترونية الناجحة التي دُفع بها الطلب؛ النقدي لا يُسترد داخل النظام (19). */
    public function payment(Order $order): Payment
    {
        return $order->payments()
            ->where('method', PaymentMethod::Electronic->value)
            ->where('status', PaymentStatus::Succeeded->value)
            ->latest('id')
            ->first()
            ?? throw BusinessRuleViolationException::rule('DEC-019', Localization::text('copy.there_is_no_successful_electronic_payment_for_this_request'));
    }

    private function amount(string $value): string
    {
        $normalized = str_replace([',', '٬', ' '], '', trim($value));

        if ($normalized === '') {
            return '0.00';
        }

        if (! is_numeric($normalized) || bccomp($normalized, '0.00', 2) < 0) {
            throw BusinessRuleViolationException::rule('DEC-019', Localization::text('copy.amounts_must_be_non_negative_numbers'));
        }

        return number_format((float) $normalized, 2, '.', '');
    }

    private function money(mixed $value): string
    {
        return number_format((float) ($value ?? 0), 2, '.', '');
    }

    private function floor(string $value): string
    {
        return bccomp($value, '0.00', 2) < 0 ? '0.00' : $value;
    }
}
