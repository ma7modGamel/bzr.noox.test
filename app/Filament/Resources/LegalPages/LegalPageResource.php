<?php

declare(strict_types=1);

namespace App\Filament\Resources\LegalPages;

use App\Filament\Resources\LegalPages\Pages\EditLegalPage;
use App\Filament\Resources\LegalPages\Pages\ListLegalPages;
use App\Filament\Resources\LegalPages\RelationManagers\VersionsRelationManager;
use App\Filament\Support\NavigationGroups;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use BackedEnum;
use Filament\Actions\EditAction;
use Filament\Forms\Components\TextInput;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/** وحدة «الصفحات» — 16، DEC-051. للمدير العام: نسخ، مسودات، نشر، وسجل. */
final class LegalPageResource extends Resource
{
    protected static ?string $model = LegalPage::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedDocumentText;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Configuration;

    protected static ?int $navigationSort = 30;

    protected static ?string $modelLabel = 'صفحة';

    protected static ?string $pluralModelLabel = 'الصفحات';

    protected static ?string $slug = 'legal-pages';

    public static function canAccess(): bool
    {
        return auth('admin')->user()?->isSuper() ?? false;
    }

    public static function canCreate(): bool
    {
        return false; // الصفحات الأربع ثابتة (DEC-051)؛ المحتوى يتغير بالنسخ.
    }

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('title')->label('اسم الصفحة في اللوحة')->required()->maxLength(150),
            TextInput::make('slug')->label('المسار العام')->prefix('/')->disabled()->dehydrated(false),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->defaultSort('sort')
            ->columns([
                TextColumn::make('title')->label('الصفحة'),
                TextColumn::make('slug')->label('المسار')->prefix('/'),
                TextColumn::make('current')
                    ->label('النسخة السارية')
                    ->state(fn (LegalPage $record): string => ($version = $record->currentVersion()) === null ? 'لم تُنشر بعد' : 'النسخة '.$version->version),
                TextColumn::make('draft')
                    ->label('مسودة')
                    ->state(fn (LegalPage $record): ?string => $record->versions()->where('status', LegalPageStatus::Draft->value)->where('requires_legal_review', true)->exists()
                        ? 'مسودة تحتاج مراجعة قانونية'
                        : null)
                    ->badge()
                    ->color('warning'),
            ])
            ->recordActions([EditAction::make()->label('النسخ')]);
    }

    public static function getRelations(): array
    {
        return [VersionsRelationManager::class];
    }

    public static function getPages(): array
    {
        return [
            'index' => ListLegalPages::route('/'),
            'edit' => EditLegalPage::route('/{record}/edit'),
        ];
    }
}
