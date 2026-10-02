<?php

declare(strict_types=1);

namespace App\Modules\Customers\Data;

final readonly class InspectionMetrics
{
    public function __construct(
        public int $explicitRejections,
        public int $expiredQuotes,
        public int $freeInspectionsClosedWithoutExecution,
    ) {}
}
