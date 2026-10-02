<?php

declare(strict_types=1);

namespace App\Filament\Resources\Providers;

use App\Filament\Resources\Providers\Pages\ListProviderProfiles;
use App\Filament\Resources\Providers\Pages\ViewProviderProfile;
use App\Filament\Resources\Providers\Schemas\ProviderProfileInfolist;
use App\Filament\Resources\Providers\Tables\ProviderProfilesTable;
use App\Filament\Support\NavigationGroups;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use BackedEnum;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Table;

/** مراجعة الفنيين وإيقافهم — 15، 16 §الفنيون. */
final class ProviderProfileResource extends Resource
{
    protected static ?string $model = ProviderProfile::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedWrenchScrewdriver;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::People;

    protected static ?int $navigationSort = 10;

    protected static ?string $modelLabel = 'مقدم خدمة';

    protected static ?string $pluralModelLabel = 'مقدمو الخدمة';

    public static function table(Table $table): Table
    {
        return ProviderProfilesTable::configure($table);
    }

    public static function infolist(Schema $schema): Schema
    {
        return ProviderProfileInfolist::configure($schema);
    }

    public static function getPages(): array
    {
        return [
            'index' => ListProviderProfiles::route('/'),
            'view' => ViewProviderProfile::route('/{record}'),
        ];
    }

    public static function canCreate(): bool
    {
        return false; // التسجيل من التطبيق؛ الإدارة تراجع فقط (15)
    }

    public static function getNavigationBadge(): ?string
    {
        $pending = ProviderProfile::query()
            ->where('status', ProviderStatus::PendingReview->value)
            ->count();

        return $pending > 0 ? (string) $pending : null;
    }

    public static function getNavigationBadgeColor(): ?string
    {
        return 'warning';
    }
}
