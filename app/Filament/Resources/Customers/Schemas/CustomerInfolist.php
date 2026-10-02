<?php

declare(strict_types=1);

namespace App\Filament\Resources\Customers\Schemas;

use App\Modules\Customers\Services\CustomerInspectionMetrics;
use App\Modules\Identity\Models\User;
use Filament\Infolists\Components\TextEntry;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Schema;

/** ملف العميل — 16 §العملاء. العدادات معلوماتية ولا تنفذ حظرًا آليًا (BR-047). */
final class CustomerInfolist
{
    public static function configure(Schema $schema): Schema
    {
        return $schema->components([
            Section::make('الحساب')
                ->columns(3)
                ->schema([
                    TextEntry::make('name')->label('الاسم'),
                    TextEntry::make('email')->label('البريد'),
                    TextEntry::make('phone')->label('الهاتف'),
                    TextEntry::make('status')->label('الحالة')->badge(),
                    TextEntry::make('blocked_reason')->label('سبب الحظر')->placeholder('—'),
                    TextEntry::make('customer_rating_avg')->label('تقييم الفنيين له')->placeholder('—'),
                ]),
            Section::make('عدادات المعاينة (BR-047)')
                ->description('معلوماتية فقط؛ لا تُطبق أي منع أو عقوبة آليًا.')
                ->columns(3)
                ->schema([
                    TextEntry::make('explicit_rejections')->label('رفض صريح لعرض التنفيذ')
                        ->state(fn (User $record): int => app(CustomerInspectionMetrics::class)->for($record)->explicitRejections),
                    TextEntry::make('expired_quotes')->label('انتهاء مهلة عرض التنفيذ')
                        ->state(fn (User $record): int => app(CustomerInspectionMetrics::class)->for($record)->expiredQuotes),
                    TextEntry::make('free_inspections')->label('معاينات مجانية بلا تنفيذ')
                        ->state(fn (User $record): int => app(CustomerInspectionMetrics::class)->for($record)->freeInspectionsClosedWithoutExecution),
                ]),
        ]);
    }
}
