<?php

declare(strict_types=1);

namespace App\Filament\Resources\LegalPages\Pages;

use App\Filament\Resources\LegalPages\LegalPageResource;
use Filament\Resources\Pages\ListRecords;

final class ListLegalPages extends ListRecords
{
    protected static string $resource = LegalPageResource::class;

    public function getTitle(): string
    {
        return 'الصفحات';
    }
}
