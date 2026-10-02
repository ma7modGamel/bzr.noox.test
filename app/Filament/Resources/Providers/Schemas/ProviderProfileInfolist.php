<?php

declare(strict_types=1);

namespace App\Filament\Resources\Providers\Schemas;

use App\Modules\Providers\Models\ProviderDocument;
use App\Modules\Providers\Models\ProviderProfile;
use Filament\Infolists\Components\ImageEntry;
use Filament\Infolists\Components\TextEntry;
use Filament\Schemas\Components\Grid;
use Filament\Schemas\Components\Section;
use Filament\Schemas\Schema;

/** ملف الفني للمراجعة — 15 §قائمة مراجعة الإدارة، 16 §الفنيون. */
final class ProviderProfileInfolist
{
    public static function configure(Schema $schema): Schema
    {
        return $schema->components([
            Section::make('الحساب والمراجعة')
                ->columns(3)
                ->schema([
                    TextEntry::make('user.name')->label('الاسم'),
                    TextEntry::make('user.phone')->label('الهاتف'),
                    TextEntry::make('user.email')->label('البريد'),
                    TextEntry::make('status')->label('الحالة')->badge(),
                    TextEntry::make('employment_type')->label('الصفة')->badge()->placeholder('—'),
                    TextEntry::make('phone_verified_at')
                        ->label('الهاتف موثّق')
                        ->placeholder('لم يُوثّق بعد')
                        ->dateTime('Y-m-d H:i', config('app.display_timezone')),
                    TextEntry::make('submitted_at')->label('تاريخ التقديم')->placeholder('—')->dateTime('Y-m-d H:i', config('app.display_timezone')),
                    TextEntry::make('reviewed_at')->label('آخر قرار')->placeholder('—')->dateTime('Y-m-d H:i', config('app.display_timezone')),
                    TextEntry::make('rejection_reason')->label('سبب الرفض')->placeholder('—'),
                    TextEntry::make('suspension_reason')->label('سبب الإيقاف')->placeholder('—')->columnSpanFull(),
                ]),

            Grid::make(2)->schema([
                Section::make('المهنة')
                    ->schema([
                        TextEntry::make('categories.name')->label('الفئات')->badge(),
                        TextEntry::make('specialties.name')->label('التخصصات')->badge()->placeholder('—'),
                        TextEntry::make('areas.name')->label('المناطق')->badge(),
                        TextEntry::make('experience_years')->label('سنوات الخبرة')->placeholder('—'),
                        TextEntry::make('bio')->label('النبذة')->placeholder('—'),
                    ]),
                Section::make('الأداء')
                    ->schema([
                        TextEntry::make('rating_avg')->label('التقييم')->placeholder('—'),
                        TextEntry::make('completed_orders_count')->label('خدمات مكتملة'),
                        TextEntry::make('payout_method')->label('طريقة الاستلام')->placeholder('—'),
                    ]),
            ]),

            // 23: مستندات الهوية لمدير التشغيل والمدير العام فقط، من مسار اللوحة بلا رابط عام (DEC-060).
            Section::make('مستندات الهوية')
                ->description('طابق الاسم والصورة الشخصية مع البطاقة (15 §قائمة المراجعة).')
                ->columns(3)
                ->schema(fn (ProviderProfile $record): array => $record->documents
                    ->map(fn (ProviderDocument $document): ImageEntry => ImageEntry::make('document_'.$document->getKey())
                        ->label($document->type)
                        ->state(route('admin.provider-documents.show', $document))
                        ->imageHeight(220))
                    ->all()),
        ]);
    }
}
