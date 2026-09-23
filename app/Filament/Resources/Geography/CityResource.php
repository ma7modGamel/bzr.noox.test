<?php

declare(strict_types=1);

namespace App\Filament\Resources\Geography;

use App\Filament\Resources\Geography\Pages\CreateCity;
use App\Filament\Resources\Geography\Pages\EditCity;
use App\Filament\Resources\Geography\Pages\ListCities;
use App\Filament\Support\NavigationGroups;
use App\Modules\Geography\Models\City;
use BackedEnum;
use Filament\Actions\DeleteAction;
use Filament\Actions\EditAction;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/**
 * المدن والمناطق — 20، BR-010. للمدير العام وحده (23).
 * التوسع لمدن جديدة إعداد فقط، بلا أسماء مدن في الكود.
 */
final class CityResource extends Resource
{
    protected static ?string $model = City::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedMapPin;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Configuration;

    protected static ?int $navigationSort = 70;

    protected static ?string $modelLabel = 'مدينة';

    protected static ?string $pluralModelLabel = 'المدن والمناطق';

    public static function canAccess(): bool
    {
        return auth('admin')->user()?->isSuper() ?? false;
    }

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('name')->label('اسم المدينة')->required()->maxLength(100),
            Select::make('timezone')
                ->label('المنطقة الزمنية')
                ->options(['Africa/Cairo' => 'Africa/Cairo'])
                ->default('Africa/Cairo')
                ->required(),
            TextInput::make('center_lat')->label('خط العرض للمركز')->numeric(),
            TextInput::make('center_lng')->label('خط الطول للمركز')->numeric(),
            Toggle::make('is_active')->label('مفعّلة')->default(true),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                TextColumn::make('name')->label('المدينة')->searchable(),
                TextColumn::make('areas_count')->label('المناطق')->counts('areas'),
                IconColumn::make('is_active')->label('مفعّلة')->boolean(),
            ])
            ->recordActions([EditAction::make(), DeleteAction::make()]);
    }

    public static function getRelations(): array
    {
        return [RelationManagers\AreasRelationManager::class];
    }

    public static function getPages(): array
    {
        return [
            'index' => ListCities::route('/'),
            'create' => CreateCity::route('/create'),
            'edit' => EditCity::route('/{record}/edit'),
        ];
    }
}
