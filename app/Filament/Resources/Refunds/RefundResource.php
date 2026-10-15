<?php

declare(strict_types=1);

namespace App\Filament\Resources\Refunds;

use App\Filament\Resources\Refunds\Pages\ListRefunds;
use App\Filament\Support\NavigationGroups;
use App\Filament\Support\SettlementActions;
use App\Modules\Payments\Models\Refund;
use BackedEnum;
use Filament\Resources\Resource;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/** الاستردادات المسجلة — 16 §المال، DEC-019، DEC-066. للقراءة؛ التسجيل من صفحة النزاع. */
final class RefundResource extends Resource
{
    protected static ?string $model = Refund::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedReceiptRefund;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Money;

    protected static ?int $navigationSort = 30;

    protected static ?string $modelLabel = 'استرداد';

    protected static ?string $pluralModelLabel = 'الاستردادات';

    public static function canAccess(): bool
    {
        return SettlementActions::super();
    }

    public static function canCreate(): bool
    {
        return false;
    }

    public static function table(Table $table): Table
    {
        return $table
            ->modifyQueryUsing(fn ($query) => $query->with(['order', 'admin']))
            ->defaultSort('created_at', 'desc')
            ->columns([
                TextColumn::make('order.number')->label('الطلب')->prefix('#')->searchable(),
                TextColumn::make('amount')->label('المبلغ')->suffix(' جنيه'),
                TextColumn::make('labor_amount')->label('مصنعية')->suffix(' جنيه'),
                TextColumn::make('materials_amount')->label('خامات')->suffix(' جنيه'),
                TextColumn::make('gateway_reference')->label('الرقم المرجعي')->copyable(),
                TextColumn::make('reason')->label('السبب')->limit(60)->wrap(),
                TextColumn::make('admin.name')->label('سجّله'),
                TextColumn::make('created_at')->label('التاريخ')->dateTime('Y-m-d H:i', config('app.display_timezone')),
            ]);
    }

    public static function getPages(): array
    {
        return [
            'index' => ListRefunds::route('/'),
        ];
    }
}
