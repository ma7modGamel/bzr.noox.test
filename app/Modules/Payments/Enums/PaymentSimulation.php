<?php

declare(strict_types=1);

namespace App\Modules\Payments\Enums;

/** نتائج المحاكي في local/testing/staging فقط؛ لا تصل إلى واجهة الإنتاج. */
enum PaymentSimulation: string
{
    case Pending = 'PENDING';
    case Success = 'SUCCESS';
    case Failure = 'FAILURE';
    case Expiry = 'EXPIRY';
}
