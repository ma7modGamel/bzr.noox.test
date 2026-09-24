<?php

declare(strict_types=1);

namespace App\Modules\Orders\Data;

final readonly class EtaEstimate
{
    public function __construct(
        public int $minutes,
        public bool $approximate = false,
    ) {}
}
