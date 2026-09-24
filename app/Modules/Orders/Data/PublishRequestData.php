<?php

declare(strict_types=1);

namespace App\Modules\Orders\Data;

use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use Carbon\CarbonImmutable;

/**
 * مدخلات نشر الطلب — الخطوات الثلاث في 07.
 * `pricingMode` و`budgetAmount` يُتجاهلان في وضع الموظفين (BR-008).
 */
final readonly class PublishRequestData
{
    /** @param list<int> $mediaIds */
    public function __construct(
        public int $customerAddressId,
        public int $categoryId,
        public int $problemTypeId,
        public TimingType $timingType,
        public MaterialsResponsibility $materialsResponsibility,
        public bool $termsAccepted,
        public ?string $description = null,
        public array $mediaIds = [],
        public ?CarbonImmutable $slotStart = null,
        public ?PricingMode $pricingMode = null,
        public ?string $budgetAmount = null,
    ) {}
}
