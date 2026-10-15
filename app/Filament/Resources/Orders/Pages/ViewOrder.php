<?php

declare(strict_types=1);

namespace App\Filament\Resources\Orders\Pages;

use App\Filament\Resources\Orders\OrderResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Actions\AdminCloseWithoutPaymentAction;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\CancelOrderAction;
use App\Modules\Orders\Actions\MarkArrivedAction;
use App\Modules\Orders\Actions\ResolveCustomerNoShowAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Support\Actions\AssignWarrantyVisitAction;
use App\Support\Exceptions\DomainException;
use Carbon\CarbonImmutable;
use Filament\Actions\Action;
use Filament\Forms\Components\DateTimePicker;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\Textarea;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\ViewRecord;
use Filament\Support\Icons\Heroicon;

/**
 * صفحة الطلب — كل تدخل يطلب سببًا ويُسجل باسم المسؤول (16، AC-ADM-03).
 * التعيين وإعادة التعيين متاحان في وضع الموظفين فقط؛ الخادم يحرسهما لا الواجهة (39).
 */
final class ViewOrder extends ViewRecord
{
    protected static string $resource = OrderResource::class;

    public function getTitle(): string
    {
        /** @var Order $record */
        $record = $this->getRecord();

        return 'طلب #'.$record->number;
    }

    public function getSubheading(): ?string
    {
        /** @var Order $record */
        $record = $this->getRecord();

        return $record->statusLabel().' · '.$record->operating_mode->getLabel();
    }

    protected function getHeaderActions(): array
    {
        return [
            $this->warrantyVisitAction(),
            $this->assignAction(),
            $this->reassignAction(),
            $this->resolveNoShowAction(),
            $this->confirmArrivalExceptionAction(),
            $this->closeWithoutPaymentAction(),
            $this->cancelAction(),
        ];
    }

    private function confirmArrivalExceptionAction(): Action
    {
        return Action::make('confirmArrivalException')->label('تأكيد الوصول باستثناء موثّق')->color('warning')
            ->visible(fn (): bool => $this->record instanceof Order && $this->record->status === OrderStatus::OnTheWay && $this->record->arrival_exception_report !== null)
            ->modalDescription(fn (): string => 'تحقّق من الوصول واتصل بالعميل. بلاغ الفني: '.($this->record->arrival_exception_report['note'] ?? ''))
            ->schema([Textarea::make('contact_note')->label('نتيجة الاتصال والتحقق وسبب الاستثناء')->required()->minLength(5)->maxLength(500)])
            ->action(function (array $data): void {
                $report = $this->record->arrival_exception_report;
                $this->runIntervention(fn (Admin $admin) => app(MarkArrivedAction::class)->execute(
                    $this->record, $this->record->providerProfile, (float) $report['lat'], (float) $report['lng'], true,
                    expectedVersion: $this->record->version, exceptionAdmin: $admin, exceptionNote: $data['contact_note']), 'تم تأكيد الوصول وتسجيل الاستثناء.');
            });
    }

    /** غياب العميل بلاغ؛ الإدارة وحدها تتابع الاتصال وتحسم نتيجته. */
    private function resolveNoShowAction(): Action
    {
        return Action::make('resolveNoShow')
            ->label('التواصل مع العميل وحسم بلاغ الغياب')
            ->color('warning')
            ->visible(fn (): bool => $this->record instanceof Order && $this->record->hasPendingNoShowReport())
            ->modalDescription('اتصل بالعميل من لوحة الإدارة، ثم سجّل نتيجة المحاولة. بلاغ الفني لا يلغي الطلب.')
            ->schema([
                Select::make('outcome')->label('نتيجة المتابعة')
                    ->options(['CONTINUE' => 'العميل موجود: متابعة الزيارة', 'CANCEL' => 'تعذّر حضور العميل: إلغاء الطلب بلا مبلغ'])
                    ->required()->in(['CONTINUE', 'CANCEL']),
                Textarea::make('contact_note')->label('نتيجة محاولة الاتصال بالعميل')
                    ->required()->minLength(5)->maxLength(500),
            ])
            ->action(function (array $data): void {
                $this->runIntervention(
                    fn (Admin $admin) => app(ResolveCustomerNoShowAction::class)->execute(
                        $this->record, $admin, $data['outcome'] === 'CANCEL', (string) $data['contact_note'],
                    ),
                    'تم تسجيل متابعة الإدارة وحسم بلاغ الغياب.',
                );
            });
    }

    private function warrantyVisitAction(): Action
    {
        return Action::make('assignWarrantyVisit')->label('تنسيق زيارة ضمان بفني آخر')->color('warning')
            ->visible(fn (): bool => $this->record instanceof Order && $this->record->warrantyClaims()->where('status', 'REQUESTED')->exists())
            ->modalDescription(fn (): string => (string) $this->record->warrantyClaims()->where('status', 'REQUESTED')->latest('id')->first()?->description)
            ->schema([
                Select::make('provider_profile_id')->label('الفني البديل')->searchable()->required()
                    ->options(function (): array {
                        $candidate = clone $this->record;
                        $candidate->timing_type = TimingType::Scheduled;
                        $candidate->slot_start = null;
                        $candidate->slot_end = null;

                        return app(ProviderEligibility::class)->query($candidate)->where('id', '!=', $this->record->provider_profile_id)
                            ->get()->mapWithKeys(fn (ProviderProfile $provider): array => [$provider->id => $provider->user->name])->all();
                    }),
                DateTimePicker::make('scheduled_at')->label('موعد الزيارة المتفق عليه مع العميل')
                    ->timezone(config('app.display_timezone'))->required()->minDate(now()),
                Textarea::make('note')->label('نتيجة متابعة العميل وتنسيق الزيارة')->required()->minLength(5)->maxLength(500),
            ])->action(function (array $data): void {
                $claim = $this->record->warrantyClaims()->where('status', 'REQUESTED')->latest('id')->firstOrFail();
                app(AssignWarrantyVisitAction::class)->execute(
                    $claim, ProviderProfile::query()->findOrFail($data['provider_profile_id']), auth('admin')->user(),
                    CarbonImmutable::parse($data['scheduled_at']), (string) $data['note'],
                );
                Notification::make()->title('تم تنسيق زيارة الضمان بفني مختلف')->success()->send();
                $this->refreshFormData([]);
            });
    }

    private function closeWithoutPaymentAction(): Action
    {
        return Action::make('closeWithoutPayment')
            ->label('إغلاق بدون دفع')
            ->icon(Heroicon::OutlinedBanknotes)
            ->color('gray')
            ->requiresConfirmation()
            ->modalDescription('يُغلق الطلب بلا مبلغ وبعمولة صفر. يُسجَّل باسمك مع السبب.')
            ->visible(fn (): bool => $this->record instanceof Order
                && $this->record->status === OrderStatus::AwaitingPayment)
            ->schema([
                Textarea::make('reason')->label('السبب')->required()->minLength(5)->maxLength(500),
            ])
            ->action(function (array $data): void {
                $this->runIntervention(
                    fn (Admin $admin) => app(AdminCloseWithoutPaymentAction::class)
                        ->execute($this->record, $admin, (string) $data['reason']),
                    'تم إغلاق الطلب بدون دفع.',
                );
            });
    }

    /** T-26 — إلغاء إداري بسبب إلزامي (16). */
    private function cancelAction(): Action
    {
        return Action::make('adminCancel')
            ->label('إلغاء الطلب')
            ->icon(Heroicon::OutlinedXCircle)
            ->color('danger')
            ->requiresConfirmation()
            ->visible(fn (): bool => $this->record instanceof Order
                && in_array($this->record->status, [
                    OrderStatus::Open, OrderStatus::Confirmed, OrderStatus::OnTheWay,
                    OrderStatus::Arrived, OrderStatus::AwaitingQuoteApproval, OrderStatus::InProgress,
                ], true))
            ->schema([
                Select::make('reason_code')
                    ->label('سبب الإلغاء')
                    ->options(fn (): array => collect(CancelReason::forActor(ActorType::Admin))
                        ->mapWithKeys(fn (CancelReason $r) => [$r->value => $r->getLabel()])
                        ->all())
                    ->required(),
                Textarea::make('note')->label('ملاحظة')->maxLength(500),
            ])
            ->action(function (array $data): void {
                $this->runIntervention(
                    fn (Admin $admin) => app(CancelOrderAction::class)->execute(
                        $this->record,
                        ActorType::Admin,
                        $admin->getKey(),
                        CancelReason::from((string) $data['reason_code']),
                        $data['note'] ?? null,
                    ),
                    'تم إلغاء الطلب.',
                );
            });
    }

    /** كل تدخل إداري يمر من هنا: يُسجَّل باسم المسؤول ويعرض أخطاء المجال كما هي (AC-ADM-03). */
    private function runIntervention(callable $callback, string $successMessage): void
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();

        try {
            $callback($admin);
        } catch (DomainException $e) {
            Notification::make()->danger()->title($e->errorCode())->body($e->getMessage())->send();

            return;
        }

        $this->refreshFormData([]);

        Notification::make()->success()->title($successMessage)->send();
    }

    /** T-27 — تعيين مقدم خدمة لطلب بانتظار التعيين. */
    private function assignAction(): Action
    {
        return Action::make('assignProvider')
            ->label('تعيين مقدم خدمة')
            ->icon(Heroicon::OutlinedUserPlus)
            ->color('primary')
            ->visible(fn (): bool => $this->record instanceof Order
                && $this->record->status === OrderStatus::Open
                && ! app(FeatureGate::class)->offersEnabled())
            ->schema([
                Select::make('provider_profile_id')
                    ->label('مقدم الخدمة المؤهل')
                    ->helperText('الفئة + المنطقة + التوفر + بلا تعارض مواعيد (BR-007)')
                    ->options(fn (): array => $this->eligibleOptions())
                    ->searchable()
                    ->required(),
            ])
            ->action(function (array $data): void {
                $this->runAssignment(fn (AssignProviderAction $action, ProviderProfile $provider, Admin $admin) => $action->execute($this->record, $provider, $admin),
                    (int) $data['provider_profile_id'],
                    'تم تعيين مقدم الخدمة وتأكيد الطلب.');
            });
    }

    /** T-29 — إعادة التعيين قبل الوصول (اعتذار + تعيين بديل في معاملة واحدة). */
    private function reassignAction(): Action
    {
        return Action::make('reassignProvider')
            ->label('إعادة التعيين')
            ->icon(Heroicon::OutlinedArrowPath)
            ->color('warning')
            ->requiresConfirmation()
            ->visible(fn (): bool => $this->record instanceof Order
                && in_array($this->record->status, [OrderStatus::Confirmed, OrderStatus::OnTheWay], true)
                && ! app(FeatureGate::class)->offersEnabled())
            ->schema([
                Select::make('provider_profile_id')
                    ->label('مقدم الخدمة البديل')
                    ->options(fn (): array => $this->eligibleOptions())
                    ->searchable()
                    ->required(),
                Textarea::make('reason')
                    ->label('سبب إعادة التعيين')
                    ->required()
                    ->minLength(5)
                    ->maxLength(500),
            ])
            ->action(function (array $data): void {
                $reason = (string) $data['reason'];

                $this->runAssignment(fn (AssignProviderAction $action, ProviderProfile $provider, Admin $admin) => $action->reassign($this->record, $provider, $admin, $reason),
                    (int) $data['provider_profile_id'],
                    'تم تعيين مقدم خدمة بديل.');
            });
    }

    /** @return array<int, string> */
    private function eligibleOptions(): array
    {
        /** @var Order $record */
        $record = $this->getRecord();

        return app(ProviderEligibility::class)
            ->query($record)
            ->get()
            ->mapWithKeys(fn (ProviderProfile $p): array => [
                $p->getKey() => $p->user->name.' — حِمل حالي: '.$p->currentLoad(),
            ])
            ->all();
    }

    private function runAssignment(callable $callback, int $providerId, string $successMessage): void
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();
        $provider = ProviderProfile::query()->with('user')->findOrFail($providerId);

        try {
            $callback(app(AssignProviderAction::class), $provider, $admin);
        } catch (DomainException $e) {
            Notification::make()
                ->danger()
                ->title($e->errorCode())
                ->body($e->getMessage())
                ->send();

            return;
        }

        $this->refreshFormData([]);

        Notification::make()->success()->title($successMessage)->send();
    }
}
