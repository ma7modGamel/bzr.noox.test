<?php

declare(strict_types=1);

namespace App\Filament\Resources\Catalog;

use App\Filament\Resources\Catalog\Pages\CreateCategory;
use App\Filament\Resources\Catalog\Pages\EditCategory;
use App\Filament\Resources\Catalog\Pages\ListCategories;
use App\Filament\Support\NavigationGroups;
use App\Modules\Catalog\Models\Category;
use BackedEnum;
use Filament\Actions\DeleteAction;
use Filament\Actions\EditAction;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/** الكتالوج — BR-012، DEC-024. أنواع المشاكل تُدار من علاقة الفئة. */
final class CategoryResource extends Resource
{
    protected static ?string $model = Category::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedSquares2x2;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Configuration;

    protected static ?int $navigationSort = 60;

    protected static ?string $modelLabel = 'فئة';

    protected static ?string $pluralModelLabel = 'الفئات وأنواع المشاكل';

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('name')->label('اسم الفئة')->required()->maxLength(100),
            TextInput::make('sort')->label('الترتيب')->numeric()->integer()->default(0),
            Toggle::make('is_active')->label('مفعّلة')->default(true),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->defaultSort('sort')
            ->columns([
                TextColumn::make('name')->label('الفئة')->searchable(),
                TextColumn::make('problem_types_count')
                    ->label('أنواع المشاكل')
                    ->counts('problemTypes'),
                IconColumn::make('is_active')->label('مفعّلة')->boolean(),
                TextColumn::make('sort')->label('الترتيب')->sortable(),
            ])
            ->recordActions([
                EditAction::make(),
                DeleteAction::make(),
            ])
            ;
    }

    public static function getRelations(): array
    {
        return [
            RelationManagers\ProblemTypesRelationManager::class,
        ];
    }

    public static function getPages(): array
    {
        return [
            'index' => ListCategories::route('/'),
            'create' => CreateCategory::route('/create'),
            'edit' => EditCategory::route('/{record}/edit'),
        ];
    }
}
