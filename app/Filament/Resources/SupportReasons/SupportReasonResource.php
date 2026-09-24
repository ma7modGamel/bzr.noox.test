<?php

declare(strict_types=1);

namespace App\Filament\Resources\SupportReasons;

use App\Filament\Resources\SupportReasons\Pages\CreateSupportReason;
use App\Filament\Resources\SupportReasons\Pages\EditSupportReason;
use App\Filament\Resources\SupportReasons\Pages\ListSupportReasons;
use App\Filament\Support\NavigationGroups;
use App\Modules\Support\Enums\SupportReasonType;
use App\Modules\Support\Models\SupportReason;
use BackedEnum;
use Filament\Actions\EditAction;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Resources\Resource;
use Filament\Schemas\Components\Utilities\Get;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Filters\SelectFilter;
use Filament\Tables\Table;
use Illuminate\Validation\Rules\Unique;

/** أسباب C27 وC28؛ الإخفاء بالتعطيل يحفظ دلالة السجلات التاريخية. */
final class SupportReasonResource extends Resource
{
    protected static ?string $model = SupportReason::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedChatBubbleBottomCenterText;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Care;

    protected static ?int $navigationSort = 50;

    protected static ?string $modelLabel = 'سبب دعم';

    protected static ?string $pluralModelLabel = 'أسباب المشاكل والبلاغات';

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            Select::make('type')
                ->label('القائمة')
                ->options(SupportReasonType::class)
                ->required(),
            TextInput::make('code')
                ->label('الكود')
                ->helperText('ثابت بعد استخدامه في بلاغ؛ أحرف إنجليزية كبيرة وأرقام وشرطة سفلية فقط.')
                ->required()
                ->rule('regex:/^[A-Z0-9_]+$/')
                ->maxLength(48)
                ->unique(
                    ignoreRecord: true,
                    modifyRuleUsing: fn (Unique $rule, Get $get): Unique => $rule->where('type', $get('type')),
                ),
            TextInput::make('label')
                ->label('النص الظاهر للعميل')
                ->required()
                ->maxLength(120),
            TextInput::make('sort')
                ->label('الترتيب')
                ->numeric()
                ->integer()
                ->minValue(0)
                ->default(0),
            Toggle::make('is_active')
                ->label('ظاهر في التطبيقات')
                ->default(true),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->defaultSort('sort')
            ->columns([
                TextColumn::make('type')->label('القائمة')->badge()->sortable(),
                TextColumn::make('label')->label('السبب')->searchable(),
                TextColumn::make('code')->label('الكود')->searchable()->copyable(),
                TextColumn::make('sort')->label('الترتيب')->sortable(),
                IconColumn::make('is_active')->label('ظاهر')->boolean(),
            ])
            ->filters([
                SelectFilter::make('type')->label('القائمة')->options(SupportReasonType::class),
            ])
            ->recordActions([
                EditAction::make(),
            ]);
    }

    public static function getRelations(): array
    {
        return [];
    }

    public static function getPages(): array
    {
        return [
            'index' => ListSupportReasons::route('/'),
            'create' => CreateSupportReason::route('/create'),
            'edit' => EditSupportReason::route('/{record}/edit'),
        ];
    }
}
