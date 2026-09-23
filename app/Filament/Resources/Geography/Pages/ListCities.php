<?php

declare(strict_types=1);

namespace App\Filament\Resources\Geography\Pages;

use App\Filament\Resources\Geography\CityResource;
use Filament\Resources\Pages\ListRecords;

final class ListCities extends ListRecords
{
    protected static string $resource = CityResource::class;

    public function getTitle(): string
    {
        return 'المدن والمناطق';
    }

    protected function getHeaderActions(): array
    {
        return [\Filament\Actions\CreateAction::make()->label('مدينة جديدة')];
    }
}
