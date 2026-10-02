<?php

declare(strict_types=1);

namespace App\Filament\Resources\Providers\Pages;

use App\Filament\Resources\Providers\ProviderProfileResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Providers\Actions\ReviewProviderAction;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
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
 * مراجعة الفني وقراراته — 15 §قائمة مراجعة الإدارة، BR-131، DEC-060.
 * القبول بعد توثيق الهاتف فقط، وكل رفض أو إيقاف بسبب إلزامي.
 */
final class ViewProviderProfile extends ViewRecord
{
    protected static string $resource = ProviderProfileResource::class;

    public function getTitle(): string
    {
        return $this->profile()->user?->name ?? 'مقدم خدمة';
    }

    protected function getHeaderActions(): array
    {
        return [
            Action::make('verifyPhone')
                ->label('تسجيل «الهاتف موثّق»')
                ->icon(Heroicon::OutlinedPhone)
                ->color('gray')
                ->requiresConfirmation()
                ->modalDescription('بعد الاتصال بالفني وتأكيد هويته وفئاته (15 §قائمة المراجعة، البند 3).')
                ->visible(fn (): bool => $this->profile()->phone_verified_at === null)
                ->action(fn () => $this->run(fn (Admin $admin) => $this->review()->verifyPhone($this->profile(), $admin), 'تم تسجيل توثيق الهاتف.')),

            Action::make('approve')
                ->label('قبول')
                ->icon(Heroicon::OutlinedCheckCircle)
                ->color('success')
                ->visible(fn (): bool => $this->profile()->status === ProviderStatus::PendingReview)
                ->disabled(fn (): bool => $this->profile()->phone_verified_at === null)
                ->tooltip(fn (): ?string => $this->profile()->phone_verified_at === null ? 'سجّل توثيق الهاتف أولًا.' : null)
                ->schema([
                    Select::make('employment_type')
                        ->label('الصفة')
                        ->options(collect(EmploymentType::cases())->mapWithKeys(fn (EmploymentType $type) => [$type->value => $type->getLabel()])->all())
                        ->default(fn (): string => app(FeatureGate::class)->offersEnabled() ? EmploymentType::Independent->value : EmploymentType::Employee->value)
                        ->helperText('بيان تعاقدي لا يراه مقدم الخدمة (39).')
                        ->required(),
                ])
                ->action(fn (array $data) => $this->run(
                    fn (Admin $admin) => $this->review()->approve($this->profile(), $admin, EmploymentType::from((string) $data['employment_type'])),
                    'تم القبول وإبلاغ الفني (NTF-22).',
                )),

            Action::make('reject')
                ->label('رفض')
                ->icon(Heroicon::OutlinedXCircle)
                ->color('danger')
                ->visible(fn (): bool => $this->profile()->status === ProviderStatus::PendingReview)
                ->schema([Textarea::make('reason')->label('سبب الرفض (يصل للفني)')->required()->minLength(5)->maxLength(500)])
                ->action(fn (array $data) => $this->run(
                    fn (Admin $admin) => $this->review()->reject($this->profile(), $admin, (string) $data['reason']),
                    'تم الرفض وإبلاغ الفني بالسبب (NTF-22).',
                )),

            Action::make('suspend')
                ->label('إيقاف')
                ->icon(Heroicon::OutlinedPauseCircle)
                ->color('danger')
                ->visible(fn (): bool => $this->profile()->status === ProviderStatus::Active)
                ->modalDescription('يكمل طلباته النشطة، ولا يستقبل طلبات جديدة، وتُغلق عروضه المقدمة (BR-131).')
                ->schema([Textarea::make('reason')->label('سبب الإيقاف')->required()->minLength(5)->maxLength(500)])
                ->action(fn (array $data) => $this->run(
                    fn (Admin $admin) => $this->review()->suspend($this->profile(), $admin, (string) $data['reason']),
                    'تم إيقاف الفني.',
                )),

            Action::make('reactivate')
                ->label('إعادة التفعيل')
                ->icon(Heroicon::OutlinedPlayCircle)
                ->color('success')
                ->requiresConfirmation()
                ->visible(fn (): bool => $this->profile()->status === ProviderStatus::Suspended)
                ->action(fn () => $this->run(fn (Admin $admin) => $this->review()->reactivate($this->profile(), $admin), 'تمت إعادة التفعيل.')),
        ];
    }

    private function profile(): ProviderProfile
    {
        /** @var ProviderProfile $record */
        $record = $this->getRecord();

        return $record;
    }

    private function review(): ReviewProviderAction
    {
        return app(ReviewProviderAction::class);
    }

    /** كل قرار يُسجَّل باسم المسؤول، وأخطاء المجال تظهر كما هي. */
    private function run(callable $callback, string $success): void
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();

        try {
            $callback($admin);
        } catch (DomainException $e) {
            Notification::make()->danger()->title($e->errorCode())->body($e->getMessage())->send();

            return;
        }

        $this->record->refresh();
        $this->refreshFormData([]);
        Notification::make()->success()->title($success)->send();
    }
}
