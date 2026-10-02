<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Enums;

/** سبب إدراج عرض التنفيذ في قائمة مراجعة الإدارة (BR-046). */
enum PriceReviewReason: string
{
    case OutsideRange = 'OUTSIDE_RANGE';
    case OtherProblem = 'OTHER_PROBLEM';
}
