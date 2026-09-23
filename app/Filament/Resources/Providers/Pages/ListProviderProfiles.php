<?php

declare(strict_types=1);

namespace App\Filament\Resources\Providers\Pages;

use App\Filament\Resources\Providers\ProviderProfileResource;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Filament\Resources\Pages\ListRecords;
use Filament\Schemas\Components\Tabs\Tab;
use Illuminate\Database\Eloquent\Builder;

final class ListProviderProfiles extends ListRecords
{
    protected static string $resource = ProviderProfileResource::class;

    public function getTitle(): string
    {
        return 'مقدمو الخدمة';
    }

    /** @return array<string, Tab> */
    public function getTabs(): array
    {
        return [
            'review' => Tab::make('بانتظار المراجعة')
                ->modifyQueryUsing(fn (Builder $query) => $query->where('status', ProviderStatus::PendingReview->value))
                ->badge(fn (): int => ProviderProfile::query()->where('status', ProviderStatus::PendingReview->value)->count())
                ->badgeColor('warning'),
            'active' => Tab::make('النشطون')
                ->modifyQueryUsing(fn (Builder $query) => $query->where('status', ProviderStatus::Active->value)),
            'all' => Tab::make('الكل'),
        ];
    }
}
