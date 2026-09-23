<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use App\Modules\Orders\Enums\OrderStatus;

/**
 * انتقال غير موجود في جدول 10-ORDER-LIFECYCLE؛ أي انتقال غير مذكور مرفوض.
 */
final class InvalidTransitionException extends DomainException
{
    public static function for(OrderStatus $from, string $action): self
    {
        return new self(
            "الإجراء [{$action}] غير مسموح والطلب في حالة [{$from->value}].",
            ['from' => $from->value, 'action' => $action],
        );
    }

    public function errorCode(): string
    {
        return 'INVALID_TRANSITION';
    }

    public function httpStatus(): int
    {
        return 409;
    }
}
