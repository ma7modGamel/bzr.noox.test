<?php

declare(strict_types=1);

namespace App\Filament\Resources\Catalog\Pages;

use App\Filament\Resources\Catalog\CategoryResource;
use Filament\Resources\Pages\CreateRecord;

final class CreateCategory extends CreateRecord
{
    protected static string $resource = CategoryResource::class;
}
