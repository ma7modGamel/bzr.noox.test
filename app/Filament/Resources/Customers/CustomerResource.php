<?php

declare(strict_types=1);

namespace App\Filament\Resources\Customers;

use App\Filament\Resources\Customers\Pages\ListCustomers;
use App\Filament\Resources\Customers\Pages\ViewCustomer;
use App\Filament\Resources\Customers\Schemas\CustomerInfolist;
use App\Filament\Support\NavigationGroups;
use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\User;
use BackedEnum;
use Filament\Actions\ViewAction;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Filters\SelectFilter;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Builder;

/** العملاء — 16 §العملاء: الملف والطلبات وعدادات BR-047، والحظر/رفعه بسبب (BR-003، DEC-060). */
final class CustomerResource extends Resource
{
    protected static ?string $model = User::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedUsers;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::People;

    protected static ?int $navigationSort = 20;

    protected static ?string $modelLabel = 'عميل';

    protected static ?string $pluralModelLabel = 'العملاء';

    public static function canCreate(): bool
    {
        return false; // التسجيل من التطبيق (03)
    }

    public static function infolist(Schema $schema): Schema
    {
        return CustomerInfolist::configure($schema);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->modifyQueryUsing(fn (Builder $query) => $query->withCount('orders'))
            ->columns([
                TextColumn::make('name')->label('الاسم')->searchable()->sortable(),
                TextColumn::make('email')->label('البريد')->searchable()->toggleable(),
                TextColumn::make('phone')->label('الهاتف')->searchable(),
                TextColumn::make('status')->label('الحالة')->badge(),
                TextColumn::make('orders_count')->label('الطلبات')->sortable(),
                TextColumn::make('created_at')->label('التسجيل')->dateTime('Y-m-d', config('app.display_timezone'))->sortable(),
            ])
            ->filters([
                SelectFilter::make('status')->label('الحالة')->options(
                    collect(UserStatus::cases())->mapWithKeys(fn (UserStatus $s) => [$s->value => $s->getLabel()])->all(),
                ),
            ])
            ->recordActions([ViewAction::make()])
            ->defaultSort('id', 'desc');
    }

    public static function getPages(): array
    {
        return [
            'index' => ListCustomers::route('/'),
            'view' => ViewCustomer::route('/{record}'),
        ];
    }
}
