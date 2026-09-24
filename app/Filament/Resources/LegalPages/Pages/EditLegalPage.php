<?php

declare(strict_types=1);

namespace App\Filament\Resources\LegalPages\Pages;

use App\Filament\Resources\LegalPages\LegalPageResource;
use Filament\Resources\Pages\EditRecord;

final class EditLegalPage extends EditRecord
{
    protected static string $resource = LegalPageResource::class;
}
