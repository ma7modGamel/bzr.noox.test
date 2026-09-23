<?php

declare(strict_types=1);

namespace App\Filament\Resources\Geography\RelationManagers;

use Filament\Actions\CreateAction;
use Filament\Actions\DeleteAction;
use Filament\Actions\EditAction;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Resources\RelationManagers\RelationManager;
use Filament\Schemas\Schema;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/** BR-010 — إيقاف منطقة يمنع الطلبات الجديدة ولا يؤثر على القائمة (EC-29). */
final class AreasRelationManager extends RelationManager
{
    protected static string $relationship = 'areas';

    protected static ?string $title = 'المناطق';

    public function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('name')->label('اسم المنطقة')->required()->maxLength(100),
            TextInput::make('sort')->label('الترتيب')->numeric()->integer()->default(0),
            Toggle::make('is_active')->label('مفعّلة')->default(true),
        ]);
    }

    public function table(Table $table): Table
    {
        return $table
            ->recordTitleAttribute('name')
            ->defaultSort('sort')
            ->columns([
                TextColumn::make('name')->label('المنطقة')->searchable(),
                IconColumn::make('is_active')->label('مفعّلة')->boolean(),
            ])
            ->headerActions([CreateAction::make()->label('إضافة منطقة')])
            ->recordActions([EditAction::make(), DeleteAction::make()]);
    }
}
