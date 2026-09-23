<?php

declare(strict_types=1);

namespace App\Filament\Resources\Catalog\Pages;

use App\Filament\Resources\Catalog\CategoryResource;
use Filament\Actions\CreateAction;
use Filament\Resources\Pages\ListRecords;

final class ListCategories extends ListRecords
{
    protected static string $resource = CategoryResource::class;

    public function getTitle(): string
    {
        return 'الكتالوج';
    }

    protected function getHeaderActions(): array
    {
        return [CreateAction::make()->label('فئة جديدة')];
    }
}
