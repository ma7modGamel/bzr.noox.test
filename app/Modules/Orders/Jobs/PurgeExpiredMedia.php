<?php

declare(strict_types=1);

namespace App\Modules\Orders\Jobs;

use App\Modules\Orders\Models\OrderMedia;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;
use Illuminate\Support\Facades\Storage;

/** BR-014 — حذف الرفع المؤقت غير المرتبط بعد 24 ساعة. */
final class PurgeExpiredMedia implements ShouldQueue
{
    use Queueable;

    public function handle(): int
    {
        $deleted = 0;

        OrderMedia::query()
            ->whereNull('order_id')
            ->whereNotNull('expires_at')
            ->where('expires_at', '<=', now())
            ->orderBy('id')
            ->chunkById(100, function ($mediaItems) use (&$deleted): void {
                foreach ($mediaItems as $media) {
                    Storage::disk('local')->delete($media->path);
                    $media->delete();
                    $deleted++;
                }
            });

        return $deleted;
    }
}
