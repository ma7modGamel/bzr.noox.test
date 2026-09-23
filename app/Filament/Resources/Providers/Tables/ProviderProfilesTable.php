<?php

declare(strict_types=1);

namespace App\Filament\Resources\Providers\Tables;

use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Filters\SelectFilter;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Builder;

final class ProviderProfilesTable
{
    public static function configure(Table $table): Table
    {
        return $table
            ->modifyQueryUsing(fn (Builder $query) => $query->with(['user', 'categories', 'areas']))
            ->columns([
                TextColumn::make('user.name')->label('الاسم')->searchable()->sortable(),
                TextColumn::make('user.phone')->label('الهاتف')->searchable()->toggleable(),

                TextColumn::make('status')->label('الحالة')->badge(),

                TextColumn::make('employment_type')
                    ->label('الصفة')
                    ->badge()
                    ->tooltip('بيان تعاقدي لا حالة — 39'),

                IconColumn::make('phone_verified_at')
                    ->label('هاتف موثق')
                    ->boolean()
                    ->tooltip('يوثقه مدير التشغيل باتصال (DEC-033)'),

                IconColumn::make('available_now')->label('متاح الآن')->boolean(),

                TextColumn::make('categories.name')->label('الفئات')->badge()->limitList(3),
                TextColumn::make('areas.name')->label('المناطق')->badge()->limitList(3)->toggleable(),

                TextColumn::make('rating_avg')->label('التقييم')->placeholder('—')->sortable(),
                TextColumn::make('completed_orders_count')->label('خدمات مكتملة')->sortable(),

                IconColumn::make('dues_blocked_at')
                    ->label('ممنوع من العروض')
                    ->boolean()
                    ->tooltip('BR-063 — لا يُطبق في وضع الموظفين (BR-065)')
                    ->visible(fn (): bool => app(FeatureGate::class)->offersEnabled()),
            ])
            ->filters([
                SelectFilter::make('status')
                    ->label('الحالة')
                    ->options(fn (): array => collect(ProviderStatus::cases())
                        ->mapWithKeys(fn (ProviderStatus $s) => [$s->value => $s->getLabel()])
                        ->all()),
                SelectFilter::make('employment_type')
                    ->label('الصفة')
                    ->options(fn (): array => collect(EmploymentType::cases())
                        ->mapWithKeys(fn (EmploymentType $e) => [$e->value => $e->getLabel()])
                        ->all()),
            ])
            ->defaultSort('id', 'desc');
    }
}
