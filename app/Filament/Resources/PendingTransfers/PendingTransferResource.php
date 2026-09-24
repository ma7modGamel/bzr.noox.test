<?php

declare(strict_types=1);

namespace App\Filament\Resources\PendingTransfers;

use App\Filament\Resources\PendingTransfers\Pages\ListPendingTransfers;
use App\Filament\Support\NavigationGroups;
use App\Modules\Identity\Models\Admin;
use App\Modules\Payments\Actions\ConfirmInstapayTransferAction;
use App\Modules\Payments\Actions\RejectInstapayTransferAction;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Enums\TransferRejectionReason;
use App\Modules\Payments\Models\Payment;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use BackedEnum;
use Filament\Actions\Action;
use Filament\Forms\Components\Checkbox;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\Textarea;
use Filament\Notifications\Notification;
use Filament\Resources\Resource;
use Filament\Schemas\Components\Text;
use Filament\Support\Icons\Heroicon;
use Filament\Tables\Columns\IconColumn;
use Filament\Tables\Columns\TextColumn;
use Filament\Tables\Table;
use Illuminate\Database\Eloquent\Builder;
use Illuminate\Support\Facades\Storage;

/**
 * تحويلات إنستاباي بانتظار التأكيد — 16، DEC-050، BR-057. للمدير العام فقط.
 * التأكيد لا يعتمد على صورة الإيصال وحدها؛ النافذة تفرض تأكيد رؤية المبلغ في حساب المنصة.
 */
final class PendingTransferResource extends Resource
{
    protected static ?string $model = Payment::class;

    protected static string|BackedEnum|null $navigationIcon = Heroicon::OutlinedBanknotes;

    protected static string|null|\UnitEnum $navigationGroup = NavigationGroups::Money;

    protected static ?int $navigationSort = 10;

    protected static ?string $modelLabel = 'تحويل بانتظار التأكيد';

    protected static ?string $pluralModelLabel = 'تحويلات بانتظار التأكيد';

    protected static ?string $slug = 'pending-transfers';

    public static function canAccess(): bool
    {
        $admin = auth('admin')->user();

        return $admin instanceof Admin && $admin->isSuper();
    }

    public static function getEloquentQuery(): Builder
    {
        return parent::getEloquentQuery()
            ->where('status', PaymentStatus::PendingVerification->value)
            ->with(['order', 'receipt']);
    }

    public static function getNavigationBadge(): ?string
    {
        $count = Payment::query()->where('status', PaymentStatus::PendingVerification->value)->count();

        return $count > 0 ? (string) $count : null;
    }

    public static function getNavigationBadgeColor(): ?string
    {
        return 'warning';
    }

    public static function table(Table $table): Table
    {
        $alertHours = app(SettingsRepository::class)->int(Cfg::PaymentDelayAlertHours);

        return $table
            ->defaultSort('submitted_at')
            ->columns([
                TextColumn::make('order.number')->label('الطلب')->prefix('#')->searchable(),
                TextColumn::make('amount')->label('المبلغ المطلوب')->suffix(' جنيه'),
                TextColumn::make('transfer_reference')->label('الرقم المرجعي للتحويل')->copyable()->searchable(),
                IconColumn::make('receipt_media_id')->label('إيصال')->boolean(),
                TextColumn::make('submitted_at')->label('وقت الإرسال')->since()->sortable(),
                TextColumn::make('overdue')
                    ->label('الحالة')
                    ->state(fn (Payment $record): string => $record->submitted_at?->addHours($alertHours)->isPast()
                        ? "متأخر (> {$alertHours} ساعة)"
                        : 'ضمن المهلة')
                    ->badge()
                    ->color(fn (Payment $record): string => $record->submitted_at?->addHours($alertHours)->isPast() ? 'danger' : 'gray'),
            ])
            ->recordActions([
                Action::make('receipt')
                    ->label('الإيصال')
                    ->icon(Heroicon::OutlinedPhoto)
                    ->visible(fn (Payment $record): bool => $record->receipt !== null)
                    ->url(fn (Payment $record): ?string => $record->receipt === null ? null : Storage::disk('local')->temporaryUrl($record->receipt->path, now()->addMinutes(10)))
                    ->openUrlInNewTab(),
                Action::make('confirm')
                    ->label('تأكيد الاستلام')
                    ->color('success')
                    ->icon(Heroicon::OutlinedCheckCircle)
                    ->modalHeading('تأكيد استلام التحويل')
                    ->schema([
                        Text::make('لا تؤكد اعتمادًا على صورة الإيصال وحدها. افتح حساب إنستاباي الخاص بالمنصة وتأكد أن المبلغ المطلوب وصل كاملًا وبالرقم المرجعي نفسه، ثم أكّد. التأكيد يغلق الطلب (T-20) ولا يمكن التراجع عنه.'),
                        Checkbox::make('seen_in_account')
                            ->label('رأيت المبلغ كاملًا في حساب المنصة')
                            ->accepted()
                            ->required(),
                    ])
                    ->action(function (Payment $record): void {
                        app(ConfirmInstapayTransferAction::class)->execute($record, auth('admin')->user());
                        Notification::make()->title('تم تأكيد الاستلام وإغلاق الطلب')->success()->send();
                    }),
                Action::make('reject')
                    ->label('رفض')
                    ->color('danger')
                    ->icon(Heroicon::OutlinedXCircle)
                    ->modalHeading('رفض التحويل')
                    ->schema([
                        Select::make('reason')->label('السبب')->options(TransferRejectionReason::class)->required(),
                        Textarea::make('note')->label('ملاحظة (اختيارية)')->maxLength(500),
                    ])
                    ->action(function (Payment $record, array $data): void {
                        app(RejectInstapayTransferAction::class)->execute(
                            $record,
                            auth('admin')->user(),
                            $data['reason'] instanceof TransferRejectionReason ? $data['reason'] : TransferRejectionReason::from($data['reason']),
                            $data['note'] ?? null,
                        );
                        Notification::make()->title('تم الرفض وإبلاغ العميل (NTF-30)')->warning()->send();
                    }),
            ])
            ->emptyStateHeading('لا توجد تحويلات بانتظار التأكيد');
    }

    public static function getPages(): array
    {
        return [
            'index' => ListPendingTransfers::route('/'),
        ];
    }
}
