<?php

declare(strict_types=1);

namespace App\Filament\Resources\Orders\Schemas;

use App\Modules\Customers\Services\CustomerInspectionMetrics;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Pricing\Enums\PriceReviewReason;
use Filament\Infolists\Components\TextEntry;
use Filament\Schemas\Components\Grid;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Schema;

/** صفحة تفاصيل الطلب — 16 §الطلبات: البيانات + المبالغ + التعيين. السجل الزمني في RelationManager. */
final class OrderInfolist
{
    public static function configure(Schema $schema): Schema
    {
        return $schema->components([
            Section::make('الطلب')
                ->columns(3)
                ->schema([
                    TextEntry::make('number')
                        ->label('الرقم')
                        ->formatStateUsing(fn (int $state): string => '#'.$state),
                    TextEntry::make('status')
                        ->label('الحالة')
                        ->badge()
                        ->formatStateUsing(fn (OrderStatus $state, Order $record): string => $state->labelFor($record->operating_mode)),
                    TextEntry::make('operating_mode')
                        ->label('وضع التشغيل')
                        ->badge()
                        ->hintIcon('heroicon-o-information-circle')
                        ->hint('مثبت عند النشر — BR-009'),
                    TextEntry::make('category.name')->label('الفئة'),
                    TextEntry::make('problemType.name')->label('نوع المشكلة'),
                    TextEntry::make('pricing_mode')->label('طريقة التسعير')->badge(),
                    TextEntry::make('description')
                        ->label('الوصف')
                        ->placeholder('—')
                        ->columnSpanFull(),
                ]),

            Grid::make(2)->schema([
                Section::make('الزيارة')
                    ->schema([
                        TextEntry::make('timing_type')->label('نوع الموعد')->badge(),
                        TextEntry::make('slot_start')
                            ->label('الفترة')
                            ->placeholder('—')
                            ->formatStateUsing(fn (?object $state, Order $record): string => $state === null
                                ? '—'
                                : $state->timezone(config('app.display_timezone'))->format('Y-m-d H:i')
                                    .' – '.$record->slot_end?->timezone(config('app.display_timezone'))->format('H:i')),
                        TextEntry::make('area.name')->label('المنطقة'),
                        TextEntry::make('address_text')->label('العنوان')->columnSpanFull(),
                        TextEntry::make('materials_responsibility')->label('الخامات'),
                    ]),

                Section::make('الأطراف')
                    ->schema([
                        TextEntry::make('customer.name')->label('العميل'),
                        TextEntry::make('customer.phone')->label('هاتف العميل'),
                        TextEntry::make('providerProfile.user.name')
                            ->label('مقدم الخدمة')
                            ->placeholder('بلا تعيين'),
                        TextEntry::make('assignedByAdmin.name')
                            ->label('عيّنه')
                            ->placeholder('—')
                            ->visible(fn (Order $record): bool => $record->isEmployeeMode()),
                        TextEntry::make('assigned_at')
                            ->label('وقت التعيين')
                            ->dateTime()
                            ->placeholder('—')
                            ->visible(fn (Order $record): bool => $record->isEmployeeMode()),
                    ]),
            ]),

            Section::make('رقابة وضع الموظفين')
                ->description('مؤشرات معلوماتية فقط؛ لا تمنع العميل ولا تغيّر السعر تلقائيًا (BR-046/047).')
                ->visible(fn (Order $record): bool => $record->isEmployeeMode())
                ->columns(4)
                ->schema([
                    TextEntry::make('customer_explicit_quote_rejections')
                        ->label('رفض عروض التنفيذ')
                        ->state(fn (Order $record): int => app(CustomerInspectionMetrics::class)->for($record->customer)->explicitRejections),
                    TextEntry::make('customer_expired_quotes')
                        ->label('عروض انتهت مهلتها')
                        ->state(fn (Order $record): int => app(CustomerInspectionMetrics::class)->for($record->customer)->expiredQuotes),
                    TextEntry::make('customer_free_inspections')
                        ->label('معاينات مجانية بلا تنفيذ')
                        ->state(fn (Order $record): int => app(CustomerInspectionMetrics::class)->for($record->customer)->freeInspectionsClosedWithoutExecution),
                    TextEntry::make('flagged_price_proposals')
                        ->label('أسعار معلّمة للمراجعة في الطلب')
                        ->badge()
                        ->state(fn (Order $record): int => $record->proposals()->where('outside_price_guide', true)->count())
                        ->helperText(function (Order $record): ?string {
                            $reasons = $record->proposals()
                                ->where('outside_price_guide', true)
                                ->pluck('price_review_reason')
                                ->unique()
                                ->map(fn (string $reason): string => match (PriceReviewReason::from($reason)) {
                                    PriceReviewReason::OutsideRange => 'سعر خارج النطاق',
                                    PriceReviewReason::OtherProblem => 'مشكلة أخرى',
                                })
                                ->implode('، ');

                            return $reasons === '' ? null : $reasons;
                        }),
                ]),

            Section::make('المبالغ')
                ->description('السعر المقبول لا يُعدَّل أبدًا؛ أي تغيير يتم بمقترح سعر (BR-040).')
                ->columns(4)
                ->schema([
                    TextEntry::make('labor_total')->label('المصنعية')->money('EGP')->placeholder('—'),
                    TextEntry::make('materials_total')->label('الخامات')->money('EGP')->placeholder('—'),
                    TextEntry::make('final_amount')->label('الإجمالي')->money('EGP')->placeholder('—'),
                    TextEntry::make('payment_status')->label('حالة الدفع')->badge(),
                    TextEntry::make('commission_rate')
                        ->label('نسبة العمولة المثبتة')
                        ->placeholder('—')
                        ->formatStateUsing(fn (?string $state): string => $state === null
                            ? '—'
                            : rtrim(rtrim(number_format((float) $state * 100, 2), '0'), '.').'%')
                        ->helperText(fn (Order $record): ?string => $record->isEmployeeMode()
                            ? 'وضع الموظفين: كامل المصنعية للمنصة (BR-065)'
                            : null),
                    TextEntry::make('commission_amount')->label('العمولة')->money('EGP')->placeholder('—'),
                    TextEntry::make('payment_method')->label('طريقة الدفع')->badge()->placeholder('—'),
                    TextEntry::make('settlement_eligible_at')->label('قابل للتسوية')->dateTime()->placeholder('—'),
                ]),
        ]);
    }
}
