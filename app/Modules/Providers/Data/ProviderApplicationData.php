<?php

declare(strict_types=1);

namespace App\Modules\Providers\Data;

use App\Modules\Providers\Enums\PayoutMethod;

final readonly class ProviderApplicationData
{
    /**
     * @param  list<int>  $categoryIds
     * @param  list<int>  $specialtyIds
     * @param  list<int>  $areaIds
     */
    public function __construct(
        public int $experienceYears,
        public ?string $bio,
        public array $categoryIds,
        public array $specialtyIds,
        public array $areaIds,
        public PayoutMethod $payoutMethod,
        public string $payoutDetails,
        public ?int $profilePhotoMediaId,
        public ?int $idFrontMediaId,
        public ?int $idBackMediaId,
    ) {}
}
