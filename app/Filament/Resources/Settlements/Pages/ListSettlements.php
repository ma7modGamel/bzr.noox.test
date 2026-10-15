<?php

declare(strict_types=1);

namespace App\Filament\Resources\Settlements\Pages;

use App\Filament\Resources\Settlements\SettlementResource;
use Filament\Resources\Pages\ListRecords;

final class ListSettlements extends ListRecords
{
    protected static string $resource = SettlementResource::class;

    public function getTitle(): string
    {
        return 'المستحقات';
    }

    public function getSubheading(): ?string
    {
        return 'موجب = المنصة مدينة للفني، سالب = الفني مدين للمنصة (BR-062). في وضع الموظفين: التوريد اليومي ورد الخامات (BR-065).';
    }
}
