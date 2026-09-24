<?php

declare(strict_types=1);

namespace App\Filament\Resources\SupportReasons\Pages;

use App\Filament\Resources\SupportReasons\SupportReasonResource;
use Filament\Resources\Pages\CreateRecord;

final class CreateSupportReason extends CreateRecord
{
    protected static string $resource = SupportReasonResource::class;

    public function getTitle(): string
    {
        return 'إضافة سبب دعم';
    }
}
