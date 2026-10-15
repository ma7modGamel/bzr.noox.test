<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use App\Support\Localization;

/** BR-003 — المستخدم المحظور لا يدخل ولا ينشر؛ طلباته النشطة تُعالج وفق EC-18. */
final class AccountBlockedException extends DomainException
{
    public static function make(): self
    {
        return new self(Localization::text('copy.this_account_is_blocked'), ['rule' => 'BR-003']);
    }

    public function errorCode(): string
    {
        return 'ACCOUNT_BLOCKED';
    }

    public function httpStatus(): int
    {
        return 403;
    }
}
