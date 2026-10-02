<?php

declare(strict_types=1);

namespace App\Filament\Resources\Admins;

use App\Filament\Resources\Admins\Pages\CreateAdmin;
use App\Filament\Resources\Admins\Pages\EditAdmin;
use App\Filament\Resources\Admins\Pages\ListAdmins;
use App\Filament\Support\NavigationGroups;
use App\Modules\Identity\Enums\AdminRole;
use App\Modules\Identity\Models\Admin;
use BackedEnum;
use Filament\Actions\Action;
use Filament\Actions\EditAction;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\TextInput;
use Filament\Forms\Components\Toggle;
use Filament\Notifications\Notification;
use Filament\Resources\Resource;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;

/** مستخدمو الإدارة — 16، 23: للمدير العام وحده (DEC-060). */
final class AdminResource extends Resource
{
    protected static ?string $model = Admin::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedShieldCheck;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Configuration;

    protected static ?int $navigationSort = 90;

    protected static ?string $modelLabel = 'مستخدم إدارة';

    protected static ?string $pluralModelLabel = 'مستخدمو الإدارة';

    public static function canAccess(): bool
    {
        return auth('admin')->user()?->isSuper() ?? false;
    }

    public static function form(Schema $schema): Schema
    {
        return $schema->components([
            TextInput::make('name')->label('الاسم')->required()->maxLength(100),
            TextInput::make('email')->label('البريد')->email()->required()->unique(ignoreRecord: true),
            Select::make('role')->label('الدور')->required()
                ->options(collect(AdminRole::cases())->mapWithKeys(fn (AdminRole $r) => [$r->value => $r->getLabel()])->all()),
            TextInput::make('password')->label('كلمة المرور')->password()->revealable()
                ->minLength(12)
                ->required(fn (string $operation): bool => $operation === 'create')
                ->dehydrated(fn (?string $state): bool => filled($state))
                ->helperText('12 حرفًا على الأقل. يضبط المستخدم المصادقة الثنائية عند أول دخول.'),
            Toggle::make('is_active')->label('نشط')->default(true),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                TextColumn::make('name')->label('الاسم')->searchable(),
                TextColumn::make('email')->label('البريد')->searchable(),
                TextColumn::make('role')->label('الدور')->badge(),
                IconColumn::make('is_active')->label('نشط')->boolean(),
                IconColumn::make('two_factor_confirmed_at')->label('مصادقة ثنائية')->boolean()
                    ->state(fn (Admin $record): bool => $record->two_factor_confirmed_at !== null),
            ])
            ->recordActions([
                EditAction::make(),
                Action::make('resetTwoFactor')
                    ->label('إعادة ضبط المصادقة الثنائية')
                    ->icon(Heroicon::OutlinedKey)
                    ->color('gray')
                    ->requiresConfirmation()
                    ->modalDescription('عند فقد الهاتف: يُطلب منه إعداد المصادقة من جديد في دخوله التالي.')
                    ->visible(fn (Admin $record): bool => $record->two_factor_confirmed_at !== null)
                    ->action(function (Admin $record): void {
                        $record->saveAppAuthenticationSecret(null);
                        $record->saveAppAuthenticationRecoveryCodes(null);
                        Notification::make()->success()->title('تمت إعادة الضبط.')->send();
                    }),
            ]);
    }

    public static function getPages(): array
    {
        return [
            'index' => ListAdmins::route('/'),
            'create' => CreateAdmin::route('/create'),
            'edit' => EditAdmin::route('/{record}/edit'),
        ];
    }
}
