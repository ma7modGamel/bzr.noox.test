<?php

declare(strict_types=1);

namespace App\Modules\Notifications\Jobs;

use App\Modules\Notifications\Services\OrderNotifications;
use Carbon\CarbonImmutable;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;

/** NTF-04 — نهاية نافذة التجميع (5 دقائق): إشعار واحد بالعروض التي وصلت داخلها، إن وُجدت. */
final class FlushOfferNotifications implements ShouldQueue
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;

    public function __construct(
        public readonly int $orderId,
        public readonly CarbonImmutable $since,
    ) {}

    public function handle(OrderNotifications $notifications): void
    {
        $notifications->flushOfferBundle($this->orderId, $this->since);
    }
}
