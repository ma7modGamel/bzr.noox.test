<?php

declare(strict_types=1);

namespace App\Filament\Resources\PendingTransfers\Pages;

use App\Filament\Resources\PendingTransfers\PendingTransferResource;
use Filament\Resources\Pages\ListRecords;

final class ListPendingTransfers extends ListRecords
{
    protected static string $resource = PendingTransferResource::class;

    public function getTitle(): string
    {
        return 'تحويلات إنستاباي بانتظار التأكيد';
    }
}
