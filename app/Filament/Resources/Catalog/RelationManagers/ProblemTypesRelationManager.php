<?php

declare(strict_types=1);

namespace App\Filament\Resources\Catalog\RelationManagers;

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

/** BR-012 — لكل فئة نوع "مشكلة أخرى" يجعل الوصف إلزاميًا (BR-013). */
final class ProblemTypesRelationManager extends RelationManager
{
    protected static string $relationship = 'problemTypes';

    protected static ?string $title = 'أنواع المشاكل';

    public function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('name')->label('نوع المشكلة')->required()->maxLength(150),
            TextInput::make('sort')->label('الترتيب')->numeric()->integer()->default(0),
            Toggle::make('is_other')
                ->label('"مشكلة أخرى"')
                ->helperText('يجعل وصف الطلب إلزاميًا (BR-013). نوع واحد لكل فئة.'),
            Toggle::make('is_active')->label('مفعّل')->default(true),
        ]);
    }

    public function table(Table $table): Table
    {
        return $table
            ->recordTitleAttribute('name')
            ->defaultSort('sort')
            ->columns([
                TextColumn::make('name')->label('النوع')->searchable(),
                IconColumn::make('is_other')->label('"مشكلة أخرى"')->boolean(),
                IconColumn::make('is_active')->label('مفعّل')->boolean(),
            ])
            ->headerActions([CreateAction::make()->label('إضافة نوع')])
            ->recordActions([EditAction::make(), DeleteAction::make()]);
    }
}
