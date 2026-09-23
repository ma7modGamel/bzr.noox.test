<?php

declare(strict_types=1);

namespace App\Filament\Resources\Geography\Pages;

use App\Filament\Resources\Geography\CityResource;
use Filament\Resources\Pages\CreateRecord;

final class CreateCity extends CreateRecord
{
    protected static string $resource = CityResource::class;
}
