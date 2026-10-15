<?php

declare(strict_types=1);

namespace App\Filament\Resources\Settlements;

use App\Filament\Resources\Providers\ProviderProfileResource;
use App\Filament\Resources\Settlements\Pages\ListSettlements;
use App\Filament\Support\NavigationGroups;
use App\Filament\Support\SettlementActions;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settlements\Services\ProviderEarningsService;
use BackedEnum;
use Filament\Actions\Action;
use Filament\Resources\Resource;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Filters\SelectFilter;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Builder;

/**
 * المستحقات — 16 §المال، DEC-066: رصيد كل فني (BR-062) ومتاحه ومعلّقه، وتسجيل التحويل والسداد.
 * للمدير العام وحده (23). الأرقام محسوبة عند العرض من نفس خدمة كشف P18.
 */
final class SettlementResource extends Resource
{
    protected static ?string $model = ProviderProfile::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedScale;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Money;

    protected static ?int $navigationSort = 20;

    protected static ?string $modelLabel = 'مستحقات فني';

    protected static ?string $pluralModelLabel = 'المستحقات';

    protected static ?string $slug = 'settlements';

    public static function canAccess(): bool
    {
        return SettlementActions::super();
    }

    public static function getEloquentQuery(): Builder
    {
        return parent::getEloquentQuery()
            ->with('user')
            ->whereIn('status', [ProviderStatus::Active->value, ProviderStatus::Suspended->value]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->columns([
                TextColumn::make('user.name')->label('الفني')->searchable(),
                TextColumn::make('employment_type')->label('الصفة')->badge(),
                TextColumn::make('balance')
                    ->label('الرصيد')
                    ->state(fn (ProviderProfile $record): string => SettlementActions::balanceLabel(
                        $record, app(ProviderEarningsService::class)->summary($record)['balance'],
                    )),
                TextColumn::make('available')
                    ->label('منه بعد مهلة النزاع (المتاح)')
                    ->state(fn (ProviderProfile $record): string => SettlementActions::balanceLabel(
                        $record, app(ProviderEarningsService::class)->summary($record)['available'],
                    )),
                IconColumn::make('dues_blocked_at')
                    ->label('ممنوع (BR-063)')
                    ->boolean()
                    ->state(fn (ProviderProfile $record): bool => $record->isBlockedByDues()),
            ])
            ->filters([
                SelectFilter::make('situation')
                    ->label('الحالة')
                    ->options([
                        'payout' => 'يحتاج تحويلًا (متاح موجب)',
                        'owes' => 'عليه سداد/توريد',
                        'blocked' => 'ممنوع من العروض',
                    ])
                    ->query(fn (Builder $query, array $data): Builder => self::situation($query, $data['value'] ?? null)),
            ])
            ->recordActions([
                SettlementActions::payout(fn (?ProviderProfile $record): ProviderProfile => $record),
                SettlementActions::remittance(fn (?ProviderProfile $record): ProviderProfile => $record),
                Action::make('statement')
                    ->label('الكشف')
                    ->icon(Heroicon::OutlinedDocumentText)
                    ->url(fn (ProviderProfile $record): string => ProviderProfileResource::getUrl('view', ['record' => $record])),
            ]);
    }

    public static function getPages(): array
    {
        return [
            'index' => ListSettlements::route('/'),
        ];
    }

    /** الأرصدة محسوبة لا مخزّنة (DEC-017)، فيُرشَّح بمعرّفات من يطابق. */
    private static function situation(Builder $query, ?string $situation): Builder
    {
        if ($situation === null || $situation === '') {
            return $query;
        }

        if ($situation === 'blocked') {
            return $query->whereNotNull('dues_blocked_at');
        }

        $earnings = app(ProviderEarningsService::class);
        $ids = (clone $query)->get()
            ->filter(function (ProviderProfile $profile) use ($earnings, $situation): bool {
                $summary = $earnings->summary($profile);

                return $situation === 'payout' ? $summary['available'] > 0.004 : $summary['balance'] < -0.004;
            })
            ->modelKeys();

        return $query->whereKey($ids);
    }
}
