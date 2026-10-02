<?php

declare(strict_types=1);

namespace App\Filament\Resources\Disputes\Pages;

use App\Filament\Resources\Disputes\DisputeResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Support\Actions\ResolveDisputeAction;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Support\Exceptions\DomainException;
use Filament\Actions\Action;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\Textarea;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\ViewRecord;
use Filament\Support\Icons\Heroicon;

/**
 * قرار النزاع — 16 §قرار النزاع، 23: مدير التشغيل يحسم بلا استرداد، والمدير العام وحده يختار الاسترداد.
 * تسجيل مبلغ الاسترداد نفسه في دفعة المال (7).
 */
final class ViewDispute extends ViewRecord
{
    protected static string $resource = DisputeResource::class;

    public function getTitle(): string
    {
        return 'نزاع على الطلب #'.$this->dispute()->order?->number;
    }

    protected function getHeaderActions(): array
    {
        return [
            Action::make('resolve')
                ->label('القرار')
                ->icon(Heroicon::OutlinedScale)
                ->visible(fn (): bool => $this->dispute()->status === DisputeStatus::Open)
                ->modalDescription('راجع السجل الزمني والمحادثة ونقطة الوصول في صفحة الطلب قبل القرار.')
                ->schema([
                    Select::make('resolution')
                        ->label('النتيجة')
                        ->options(fn (): array => collect(self::allowedResolutions($this->dispute(), $this->admin()))
                            ->mapWithKeys(fn (DisputeResolution $r) => [$r->value => $r->getLabel()])
                            ->all())
                        ->required(),
                    Textarea::make('note')->label('ملاحظة القرار')->required()->minLength(5)->maxLength(1000),
                ])
                ->action(function (array $data): void {
                    $resolution = DisputeResolution::from((string) $data['resolution']);
                    if (! in_array($resolution, self::allowedResolutions($this->dispute(), $this->admin()), true)) {
                        Notification::make()->danger()->title('نتيجة غير مسموحة لدورك.')->send();

                        return;
                    }

                    try {
                        app(ResolveDisputeAction::class)->execute(
                            $this->dispute()->order, $this->dispute(), $this->admin(), $resolution, (string) $data['note'],
                        );
                    } catch (DomainException $e) {
                        Notification::make()->danger()->title($e->errorCode())->body($e->getMessage())->send();

                        return;
                    }

                    $this->record->refresh();
                    $this->refreshFormData([]);
                    Notification::make()->success()->title('تم تسجيل القرار وإبلاغ الطرفين (NTF-17).')->send();
                }),
        ];
    }

    /** @return list<DisputeResolution> */
    public static function allowedResolutions(Dispute $dispute, Admin $admin): array
    {
        $refunds = $admin->isSuper();

        if ($dispute->is_post_close) {
            return array_values(array_filter([
                DisputeResolution::CloseAsIs,
                $refunds ? DisputeResolution::CloseWithRefund : null,
            ]));
        }

        return array_values(array_filter([
            DisputeResolution::CloseAsIs,
            DisputeResolution::CloseWithoutPayment,
            $refunds ? DisputeResolution::CloseWithRefund : null,
            $refunds ? DisputeResolution::CancelFullRefund : null,
        ]));
    }

    private function dispute(): Dispute
    {
        /** @var Dispute $record */
        $record = $this->getRecord();

        return $record;
    }

    private function admin(): Admin
    {
        /** @var Admin $admin */
        $admin = auth('admin')->user();

        return $admin;
    }
}
