<?php

declare(strict_types=1);

namespace App\Modules\Catalog\Data;

final readonly class PriceGuideRow
{
    public function __construct(
        public int $rowNumber,
        public int $problemTypeId,
        public string $categoryName,
        public string $problemTypeName,
        public ?string $minimum,
        public ?string $maximum,
        public ?string $notes,
    ) {}
}
