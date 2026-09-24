<?php

declare(strict_types=1);

use App\Modules\Orders\Jobs\AutoCloseConfirmedOrders;
use App\Modules\Orders\Jobs\ExpirePendingProposals;
use App\Modules\Orders\Jobs\ExpireStaleOrders;
use App\Modules\Orders\Jobs\PurgeExpiredMedia;
use Illuminate\Support\Facades\Schedule;

/*
| المهام المجدولة — 30 §المهام المجدولة.
| كل مهمة idempotent: تعيد التحقق من الحالة داخل المعاملة قبل التنفيذ،
| و`withoutOverlapping` يمنع تشغيلين متوازيين على نفس الصفوف.
*/

// T-03 — انتهاء نافذة العروض (CFG-013) أو مهلة التعيين (CFG-093)
Schedule::job(new ExpireStaleOrders)
    ->everyMinute()
    ->withoutOverlapping()
    ->name('orders:expire-stale');

// CFG-040 — انتهاء مهلة مقترح السعر = رفض (T-15 أو T-28)
Schedule::job(new ExpirePendingProposals)
    ->everyMinute()
    ->withoutOverlapping()
    ->name('proposals:expire-pending');

// BR-080 — الإغلاق التلقائي بعد CFG-051 من الإنهاء (T-21)
Schedule::job(new AutoCloseConfirmedOrders)
    ->everyFiveMinutes()
    ->withoutOverlapping()
    ->name('orders:auto-close');

// BR-014 — الرفع المؤقت غير المنشور يُحذف بعد 24 ساعة
Schedule::job(new PurgeExpiredMedia)
    ->hourly()
    ->withoutOverlapping()
    ->name('media:purge-expired');
