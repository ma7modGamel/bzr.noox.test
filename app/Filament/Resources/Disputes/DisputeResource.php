<?php

declare(strict_types=1);

namespace App\Filament\Resources\Disputes;

use App\Filament\Resources\Disputes\Pages\ListDisputes;
use App\Filament\Resources\Disputes\Pages\ViewDispute;
use App\Filament\Resources\Orders\OrderResource;
use App\Filament\Support\NavigationGroups;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Modules\Support\Models\SupportReason;
use BackedEnum;
use Filament\Actions\ViewAction;
use Filament\Infolists\Components\TextEntry;
use Filament\Resources\Resource;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Schema;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Filters\SelectFilter;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Builder;

/** النزاعات — 16 §النزاعات و§قرار النزاع، BR-120، DEC-060. */
final class DisputeResource extends Resource
{
    protected static ?string $model = Dispute::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedScale;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Care;

    protected static ?int $navigationSort = 10;

    protected static ?string $modelLabel = 'نزاع';

    protected static ?string $pluralModelLabel = 'النزاعات';

    public static function canCreate(): bool
    {
        return false; // يفتحها العميل أو الفني من التطبيق (C27)
    }

    public static function getNavigationBadge(): ?string
    {
        $open = Dispute::query()->where('status', DisputeStatus::Open->value)->count();

        return $open > 0 ? (string) $open : null;
    }

    public static function getNavigationBadgeColor(): ?string
    {
        return 'danger';
    }

    public static function infolist(Schema $schema): Schema
    {
        return $schema->components([
            Section::make('النزاع')
                ->columns(3)
                ->schema([
                    TextEntry::make('order.number')
                        ->label('الطلب')
                        ->formatStateUsing(fn (int $state): string => '#'.$state)
                        ->url(fn (Dispute $record): string => OrderResource::getUrl('view', ['record' => $record->order_id])),
                    TextEntry::make('order.status')->label('حالة الطلب')->badge(),
                    TextEntry::make('opened_by_type')->label('فتحه')->badge(),
                    TextEntry::make('reason_code')->label('السبب')
                        ->formatStateUsing(fn (string $state): string => SupportReason::query()->where('code', $state)->value('label') ?? $state),
                    TextEntry::make('is_post_close')->label('بعد الإغلاق (BR-120)')
                        ->formatStateUsing(fn (bool $state): string => $state ? 'نعم — الطلب يبقى مغلقًا' : 'لا'),
                    TextEntry::make('created_at')->label('فُتح')->dateTime('Y-m-d H:i', config('app.display_timezone')),
                    TextEntry::make('description')->label('الوصف')->columnSpanFull(),
                ]),
            Section::make('القرار')
                ->columns(3)
                ->visible(fn (Dispute $record): bool => $record->status === DisputeStatus::Resolved)
                ->schema([
                    TextEntry::make('resolution')->label('النتيجة')->badge(),
                    TextEntry::make('resolvedBy.name')->label('بواسطة'),
                    TextEntry::make('resolved_at')->label('في')->dateTime('Y-m-d H:i', config('app.display_timezone')),
                    TextEntry::make('resolution_note')->label('الملاحظة')->columnSpanFull(),
                ]),
        ]);
    }

    public static function table(Table $table): Table
    {
        return $table
            ->modifyQueryUsing(fn (Builder $query) => $query->with('order'))
            ->columns([
                TextColumn::make('order.number')->label('الطلب')->formatStateUsing(fn (int $state): string => '#'.$state)->searchable(),
                TextColumn::make('status')->label('الحالة')->badge(),
                TextColumn::make('opened_by_type')->label('فتحه')->badge(),
                IconColumn::make('is_post_close')->label('بعد الإغلاق')->boolean(),
                TextColumn::make('created_at')->label('فُتح')->since()->sortable(),
            ])
            ->filters([
                SelectFilter::make('status')->label('الحالة')
                    ->options(collect(DisputeStatus::cases())->mapWithKeys(fn (DisputeStatus $s) => [$s->value => $s->getLabel()])->all())
                    ->default(DisputeStatus::Open->value),
            ])
            ->recordActions([ViewAction::make()])
            ->defaultSort('created_at');
    }

    public static function getPages(): array
    {
        return [
            'index' => ListDisputes::route('/'),
            'view' => ViewDispute::route('/{record}'),
        ];
    }
}
