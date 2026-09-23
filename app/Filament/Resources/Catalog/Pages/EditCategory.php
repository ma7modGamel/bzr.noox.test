<?php

declare(strict_types=1);

namespace App\Filament\Resources\Catalog\Pages;

use App\Filament\Resources\Catalog\CategoryResource;
use Filament\Resources\Pages\EditRecord;

final class EditCategory extends EditRecord
{
    protected static string $resource = CategoryResource::class;
}
