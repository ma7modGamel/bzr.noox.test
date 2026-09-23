<?php

declare(strict_types=1);

namespace App\Support\Exceptions;

use App\Modules\Settings\Enums\Cfg;

/**
 * الإجراء يخص ميزة معطّلة في وضع التشغيل الحالي (39-OPERATING-MODES).
 * يُرفض قبل أي قفل أو كتابة، ومهما كانت صلاحية المنفّذ (23 §قاعدة الوضع).
 */
final class FeatureDisabledException extends DomainException
{
    public static function for(Cfg $flag, string $action): self
    {
        return new self(
            "الإجراء [{$action}] غير متاح في وضع التشغيل الحالي.",
            ['feature' => $flag->value, 'setting' => $flag->id(), 'action' => $action],
        );
    }

    public function errorCode(): string
    {
        return 'FEATURE_DISABLED';
    }

    public function httpStatus(): int
    {
        return 409;
    }
}
