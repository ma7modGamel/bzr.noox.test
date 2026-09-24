<?php

declare(strict_types=1);

namespace App\Filament\Resources\SupportReasons\Pages;

use App\Filament\Resources\SupportReasons\SupportReasonResource;
use Filament\Resources\Pages\EditRecord;

final class EditSupportReason extends EditRecord
{
    protected static string $resource = SupportReasonResource::class;

    public function getTitle(): string
    {
        return 'تعديل سبب الدعم';
    }
}
