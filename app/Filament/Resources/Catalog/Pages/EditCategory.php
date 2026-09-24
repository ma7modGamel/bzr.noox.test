<?php

declare(strict_types=1);

namespace App\Filament\Resources\Catalog\Pages;

use App\Filament\Resources\Catalog\CategoryResource;
use App\Modules\Catalog\Services\CoverageCheck;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\EditRecord;

final class EditCategory extends EditRecord
{
    protected static string $resource = CategoryResource::class;

    /** DEC-053 — تحذير بلا منع عند تفعيل فئة لا يغطيها فني نشط. */
    protected function afterSave(): void
    {
        $warning = app(CoverageCheck::class)->categoryWarning($this->getRecord());

        if ($warning !== null) {
            Notification::make()->title('تنبيه تغطية')->body($warning)->warning()->persistent()->send();
        }
    }
}
