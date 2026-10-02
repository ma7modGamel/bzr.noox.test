<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Push;

/** نتيجة محاولة Push واحدة — 17 §الإرسال وإعادة المحاولة. */
enum PushResult
{
    case Sent;
    /** UNREGISTERED أو 404 أو INVALID_ARGUMENT للرمز: يُحذف الرمز بلا إعادة. */
    case InvalidToken;
    /** انقطاع أو 429 أو 5xx: إعادة المحاولة (EC-22). */
    case Retry;
    /** 401/403 أو إعداد ناقص: خطأ تشغيل لا تفيده الإعادة. */
    case Fatal;
}
