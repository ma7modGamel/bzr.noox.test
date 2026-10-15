<?php

declare(strict_types=1);

namespace App\Filament\Resources\ReviewTags;

use App\Filament\Resources\ReviewTags\Pages\CreateReviewTag;
use App\Filament\Resources\ReviewTags\Pages\EditReviewTag;
use App\Filament\Resources\ReviewTags\Pages\ListReviewTags;
use App\Filament\Support\NavigationGroups;
use App\Modules\Reviews\Models\ReviewTag;
use Filament\Actions\EditAction;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Model;

final class ReviewTagResource extends Resource
{
    protected static ?string $model = ReviewTag::class;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Care;

    protected static ?string $modelLabel = 'وسم تقييم الخدمة';

    protected static ?string $pluralModelLabel = 'وسوم تقييم الخدمة';

    public static function canViewAny(): bool
    {
        return auth('admin')->user()?->isSuper() ?? false;
    }

    public static function canCreate(): bool
    {
        return self::canViewAny();
    }

    public static function canEdit(Model $record): bool
    {
        return self::canViewAny();
    }

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('code')->label('الكود الثابت')->required()->maxLength(48)->rule('regex:/^[A-Z0-9_]+$/')
                ->unique(ignoreRecord: true)->disabled(fn (?ReviewTag $record): bool => $record !== null)
                ->helperText('لا يتغير بعد الإنشاء؛ تعطيل الوسم يمنع اختياره دون تغيير التقييمات السابقة.'),
            TextInput::make('label')->label('النص بالعربية')->required()->maxLength(120),
            TextInput::make('label_en')->label('النص بالإنجليزية')->maxLength(120)->extraInputAttributes(['dir' => 'ltr']),
            TextInput::make('sort')->label('الترتيب')->integer()->minValue(0)->maxValue(65535)->default(0),
            Toggle::make('is_active')->label('متاح للتقييمات الجديدة')->default(true),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table->defaultSort('sort')->columns([
            TextColumn::make('code')->label('الكود'), TextColumn::make('label')->label('الوسم')->searchable(),
            TextColumn::make('sort')->label('الترتيب'), IconColumn::make('is_active')->label('متاح')->boolean(),
        ])->recordActions([EditAction::make()]);
    }

    public static function getPages(): array
    {
        return ['index' => ListReviewTags::route('/'), 'create' => CreateReviewTag::route('/create'), 'edit' => EditReviewTag::route('/{record}/edit')];
    }
}
