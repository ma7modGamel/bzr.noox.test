<?php

declare(strict_types=1);

namespace App\Filament\Resources\Geography\Pages;

use App\Filament\Resources\Geography\CityResource;
use Filament\Resources\Pages\EditRecord;

final class EditCity extends EditRecord
{
    protected static string $resource = CityResource::class;
}
