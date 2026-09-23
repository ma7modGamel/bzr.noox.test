<?php

declare(strict_types=1);

namespace App\Filament\Resources\Orders\Tables;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\OperatingMode;
use Filament\Actions\ViewAction;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Filters\SelectFilter;
use Filament\Tables\Filters\TernaryFilter;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Builder;

/**
 * قائمة الطلبات — 16: بحث بالرقم/العميل/الفني، وتصفية بالحالة والمنطقة والفئة والتاريخ.
 */
final class OrdersTable
{
    public static function configure(Table $table): Table
    {
        return $table
            ->defaultSort('id', 'desc')
            ->modifyQueryUsing(fn (Builder $query) => $query->with([
                'customer', 'providerProfile.user', 'area', 'category', 'problemType',
            ]))
            ->columns([
                TextColumn::make('number')
                    ->label('الرقم')
                    ->formatStateUsing(fn (int $state): string => '#'.$state)
                    ->searchable()
                    ->sortable(),

                TextColumn::make('status')
                    ->label('الحالة')
                    ->badge()
                    ->formatStateUsing(fn (OrderStatus $state, Order $record): string => $state->labelFor($record->operating_mode)),

                TextColumn::make('operating_mode')
                    ->label('الوضع')
                    ->badge()
                    ->toggleable(isToggledHiddenByDefault: true),

                TextColumn::make('customer.name')
                    ->label('العميل')
                    ->searchable()
                    ->toggleable(),

                TextColumn::make('providerProfile.user.name')
                    ->label('مقدم الخدمة')
                    ->placeholder('بلا تعيين')
                    ->searchable()
                    ->toggleable(),

                TextColumn::make('category.name')
                    ->label('الفئة')
                    ->toggleable(),

                TextColumn::make('area.name')
                    ->label('المنطقة')
                    ->toggleable(),

                TextColumn::make('timing_type')
                    ->label('الموعد')
                    ->badge()
                    ->color(fn (TimingType $state): string => $state === TimingType::Now ? 'warning' : 'gray')
                    ->description(fn (Order $record): ?string => $record->slot_start?->timezone(config('app.display_timezone'))->format('Y-m-d H:i')),

                TextColumn::make('final_amount')
                    ->label('الإجمالي')
                    ->money('EGP')
                    ->placeholder('—')
                    ->extraAttributes(['class' => 'bzr-money'])
                    ->toggleable(),

                TextColumn::make('created_at')
                    ->label('أُنشئ')
                    ->since()
                    ->sortable(),
            ])
            ->filters([
                SelectFilter::make('status')
                    ->label('الحالة')
                    ->multiple()
                    ->options(fn (): array => collect(OrderStatus::cases())
                        ->mapWithKeys(fn (OrderStatus $s) => [$s->value => $s->getLabel()])
                        ->all()),

                SelectFilter::make('operating_mode')
                    ->label('وضع التشغيل')
                    ->options(fn (): array => collect(OperatingMode::cases())
                        ->mapWithKeys(fn (OperatingMode $m) => [$m->value => $m->getLabel()])
                        ->all()),

                SelectFilter::make('category_id')
                    ->label('الفئة')
                    ->relationship('category', 'name'),

                SelectFilter::make('area_id')
                    ->label('المنطقة')
                    ->relationship('area', 'name'),

                SelectFilter::make('pricing_mode')
                    ->label('طريقة التسعير')
                    ->options(fn (): array => collect(PricingMode::cases())
                        ->mapWithKeys(fn (PricingMode $p) => [$p->value => $p->getLabel()])
                        ->all()),

                TernaryFilter::make('awaiting_assignment')
                    ->label('بانتظار التعيين')
                    ->placeholder('الكل')
                    ->trueLabel('بلا تعيين فقط')
                    ->falseLabel('المعيَّنة فقط')
                    ->queries(
                        true: fn (Builder $query) => $query->awaitingAssignment(),
                        false: fn (Builder $query) => $query->whereNotNull('provider_profile_id'),
                        blank: fn (Builder $query) => $query,
                    ),
            ])
            ->recordActions([
                ViewAction::make()->label('فتح'),
            ]);
    }
}
