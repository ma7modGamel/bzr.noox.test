<?php

declare(strict_types=1);

namespace App\Modules\Orders\Contracts;

use App\Modules\Orders\Data\EtaEstimate;

interface RouteEtaProvider
{
    public function estimate(float $originLat, float $originLng, float $destinationLat, float $destinationLng): EtaEstimate;
}
