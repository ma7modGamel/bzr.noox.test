<?php

declare(strict_types=1);

namespace App\Modules\Settlements\Jobs;

use App\Modules\Settlements\Services\DuesBlocking;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;

/** BR-063 — كل 10 دقائق (30 §المهام المجدولة). */
final class RefreshDuesBlocks implements ShouldQueue
{
    use Queueable;

    public function handle(DuesBlocking $dues): int
    {
        return $dues->refreshAll();
    }
}
