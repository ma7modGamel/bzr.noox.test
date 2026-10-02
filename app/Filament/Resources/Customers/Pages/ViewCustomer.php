<?php

declare(strict_types=1);

namespace App\Filament\Resources\Customers\Pages;

use App\Filament\Resources\Customers\CustomerResource;
use App\Modules\Identity\Actions\SetUserBlockedAction;
use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use Filament\Actions\Action;
use Filament\Forms\Components\Textarea;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\ViewRecord;
use Filament\Support\Icons\Heroicon;

/** الحظر ورفعه بسبب — BR-003، EC-18، DEC-060. */
final class ViewCustomer extends ViewRecord
{
    protected static string $resource = CustomerResource::class;

    public function getTitle(): string
    {
        return $this->user()->name;
    }

    protected function getHeaderActions(): array
    {
        return [
            Action::make('block')
                ->label('حظر الحساب')
                ->icon(Heroicon::OutlinedNoSymbol)
                ->color('danger')
                ->visible(fn (): bool => $this->user()->status === UserStatus::Active)
                ->modalDescription('يُخرج من كل الأجهزة فورًا، وتُلغى طلباته المفتوحة. الطلبات المؤكدة تبقى لقرارك (EC-18).')
                ->schema([Textarea::make('reason')->label('السبب (يصل له بالبريد)')->required()->minLength(5)->maxLength(500)])
                ->action(function (array $data): void {
                    $cancelled = app(SetUserBlockedAction::class)->block($this->user(), $this->admin(), (string) $data['reason']);
                    $this->done('تم الحظر'.($cancelled > 0 ? ' وإلغاء '.$cancelled.' طلب مفتوح.' : '.'));
                }),
            Action::make('unblock')
                ->label('رفع الحظر')
                ->icon(Heroicon::OutlinedCheckCircle)
                ->color('success')
                ->requiresConfirmation()
                ->visible(fn (): bool => $this->user()->status === UserStatus::Blocked)
                ->action(function (): void {
                    app(SetUserBlockedAction::class)->unblock($this->user(), $this->admin());
                    $this->done('تم رفع الحظر.');
                }),
        ];
    }

    private function user(): User
    {
        /** @var User $record */
        $record = $this->getRecord();

        return $record;
    }

    private function admin(): Admin
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();

        return $admin;
    }

    private function done(string $message): void
    {
        $this->record->refresh();
        $this->refreshFormData([]);
        Notification::make()->success()->title($message)->send();
    }
}
