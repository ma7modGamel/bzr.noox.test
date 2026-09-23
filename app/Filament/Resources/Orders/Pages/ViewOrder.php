<?php

declare(strict_types=1);

namespace App\Filament\Resources\Orders\Pages;

use App\Filament\Resources\Orders\OrderResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Support\Exceptions\DomainException;
use Filament\Actions\Action;
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
            $this->assignAction(),
            $this->reassignAction(),
        ];
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
