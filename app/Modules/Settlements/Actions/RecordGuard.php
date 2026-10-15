<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Actions;

use App\Modules\Identity\Models\Admin;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use DateTimeInterface;

/** قيود مشتركة لتسجيل الحركات المالية: المدير العام، مبلغ موجب، رقم مرجعي، تاريخ غير مستقبلي (23، DEC-066). */
final class RecordGuard
{
    public static function superAdmin(Admin $admin): void
    {
        if (! $admin->isSuper() || ! $admin->is_active) {
            throw BusinessRuleViolationException::rule('DEC-066', Localization::text('copy.only_the_general_manager_may_record_financial_transactions'));
        }
    }

    public static function positiveAmount(string $amount): string
    {
        $normalized = str_replace([',', '٬', ' '], '', trim($amount));

        if (! is_numeric($normalized) || bccomp($normalized, '0.00', 2) <= 0) {
            throw BusinessRuleViolationException::rule('DEC-066', Localization::text('copy.the_amount_must_be_greater_than_zero'));
        }

        return number_format((float) $normalized, 2, '.', '');
    }

    public static function reference(string $reference): string
    {
        $reference = trim($reference);

        if ($reference === '' || mb_strlen($reference) > 64) {
            throw BusinessRuleViolationException::rule('DEC-066', Localization::text('copy.a_reference_number_is_required_up_to_64_characters'));
        }

        return $reference;
    }

    public static function notInFuture(DateTimeInterface $at): void
    {
        if ($at > now()) {
            throw BusinessRuleViolationException::rule('DEC-066', Localization::text('copy.a_transaction_cannot_be_recorded_with_a_future_date'));
        }
    }

    public static function note(?string $note): ?string
    {
        $note = trim((string) $note);

        return $note === '' ? null : mb_substr($note, 0, 500);
    }
}
