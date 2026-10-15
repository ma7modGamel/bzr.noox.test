<?php

declare(strict_types=1);

namespace App\Filament\Support;

use App\Modules\Identity\Models\Admin;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\PayoutMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settlements\Actions\RecordProviderPayoutAction;
use App\Modules\Settlements\Actions\RecordProviderRemittanceAction;
use App\Modules\Settlements\Services\ProviderEarningsService;
use App\Support\Exceptions\DomainException;
use Closure;
use Filament\Actions\Action;
use Filament\Forms\Components\Checkbox;
use Filament\Forms\Components\DateTimePicker;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\Textarea;
use Filament\Forms\Components\TextInput;
use Filament\Notifications\Notification;
use Filament\Support\Icons\Heroicon;
use Illuminate\Support\Carbon;

/**
 * تسجيل التحويل للفني والسداد/التوريد منه — DEC-066. للمدير العام وحده (23)،
 * ومشتركة بين صفحة الفني وقائمة المستحقات.
 */
final class SettlementActions
{
    /** @param Closure(?ProviderProfile): ProviderProfile $resolve */
    public static function payout(Closure $resolve, string $name = 'recordPayout'): Action
    {
        return Action::make($name)
            ->label(fn (?ProviderProfile $record = null): string => self::isEmployee($resolve($record)) ? 'تسجيل رد خامات' : 'تسجيل تحويل')
            ->icon(Heroicon::OutlinedArrowUpTray)
            ->color('success')
            ->visible(fn (): bool => self::super())
            ->modalDescription(fn (?ProviderProfile $record = null): string => 'المتاح للتحويل الآن: '
                .self::money(app(ProviderEarningsService::class)->summary($resolve($record))['available'])
                .' جنيه (BR-064). سجّل بعد تنفيذ التحويل فعلًا.')
            ->schema(self::movementFields('paid_at', 'تاريخ التحويل'))
            ->action(function (array $data, ?ProviderProfile $record = null) use ($resolve): void {
                self::run(fn (Admin $admin) => app(RecordProviderPayoutAction::class)->execute(
                    $resolve($record), $admin, (string) $data['amount'], PayoutMethod::from((string) $data['method']),
                    (string) $data['reference'], Carbon::parse($data['paid_at']), $data['note'] ?? null,
                ), 'تم تسجيل التحويل وإبلاغ الفني (NTF-23).');
            });
    }

    /** @param Closure(?ProviderProfile): ProviderProfile $resolve */
    public static function remittance(Closure $resolve, string $name = 'recordRemittance'): Action
    {
        return Action::make($name)
            ->label(fn (?ProviderProfile $record = null): string => self::isEmployee($resolve($record)) ? 'تسجيل توريد' : 'تسجيل سداد')
            ->icon(Heroicon::OutlinedArrowDownTray)
            ->color('warning')
            ->visible(fn (): bool => self::super())
            ->modalDescription(fn (?ProviderProfile $record = null): string => 'المستحق على الفني الآن: '
                .app(RecordProviderRemittanceAction::class)->debt($resolve($record)).' جنيه.')
            ->schema([
                ...self::movementFields('received_at', 'تاريخ الاستلام'),
                Checkbox::make('above_debt')
                    ->label('المبلغ أكبر من المستحق الحالي (توريد نقدية طلب لم يُغلق بعد) — أؤكد التسجيل'),
            ])
            ->action(function (array $data, ?ProviderProfile $record = null) use ($resolve): void {
                self::run(fn (Admin $admin) => app(RecordProviderRemittanceAction::class)->execute(
                    $resolve($record), $admin, (string) $data['amount'], PayoutMethod::from((string) $data['method']),
                    (string) $data['reference'], Carbon::parse($data['received_at']), (bool) ($data['above_debt'] ?? false), $data['note'] ?? null,
                ), 'تم تسجيل السداد وتحديث حالة المنع (BR-063).');
            });
    }

    /** موجب = المنصة مدينة للفني، سالب = الفني مدين للمنصة (BR-062)، بمسميات الوضع (DEC-066). */
    public static function balanceLabel(ProviderProfile $profile, float $balance): string
    {
        if (abs($balance) < 0.005) {
            return 'لا شيء';
        }

        $amount = self::money(abs($balance)).' جنيه';

        if (self::isEmployee($profile)) {
            return $balance < 0 ? 'مطلوب توريده: '.$amount : 'رد خامات: '.$amount;
        }

        return $balance < 0 ? 'مدين للمنصة: '.$amount : 'مستحق له: '.$amount;
    }

    public static function money(float $amount): string
    {
        return number_format($amount, 2, '.', '');
    }

    public static function super(): bool
    {
        $admin = auth('admin')->user();

        return $admin instanceof Admin && $admin->isSuper();
    }

    /** @return list<mixed> */
    private static function movementFields(string $dateField, string $dateLabel): array
    {
        return [
            TextInput::make('amount')->label('المبلغ (جنيه)')->numeric()->minValue(0.01)->step('0.01')->required(),
            Select::make('method')
                ->label('القناة')
                ->options(collect(PayoutMethod::cases())->mapWithKeys(fn (PayoutMethod $m) => [$m->value => $m->getLabel()])->all())
                ->default(PayoutMethod::Instapay->value)
                ->required(),
            TextInput::make('reference')->label('الرقم المرجعي')->required()->maxLength(64),
            DateTimePicker::make($dateField)->label($dateLabel)->default(now())->maxDate(now())->seconds(false)->required(),
            Textarea::make('note')->label('ملاحظة')->maxLength(500),
        ];
    }

    private static function isEmployee(ProviderProfile $profile): bool
    {
        return $profile->employment_type === EmploymentType::Employee;
    }

    /** @param Closure(Admin): mixed $callback */
    private static function run(Closure $callback, string $success): void
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();

        try {
            $callback($admin);
        } catch (DomainException $e) {
            Notification::make()->danger()->title($e->errorCode())->body($e->getMessage())->send();

            return;
        }

        Notification::make()->success()->title($success)->send();
    }
}
