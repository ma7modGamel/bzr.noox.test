<?php

declare(strict_types=1);

namespace App\Filament\Resources\SupportReasons\Pages;

use App\Filament\Resources\SupportReasons\SupportReasonResource;
use Filament\Actions\CreateAction;
use Filament\Resources\Pages\ListRecords;

final class ListSupportReasons extends ListRecords
{
    protected static string $resource = SupportReasonResource::class;

    public function getTitle(): string
    {
        return 'أسباب المشاكل والبلاغات';
    }

    protected function getHeaderActions(): array
    {
        return [
            CreateAction::make()->label('سبب جديد'),
        ];
    }
}
