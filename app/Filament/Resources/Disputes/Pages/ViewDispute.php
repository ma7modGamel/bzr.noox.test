<?php

declare(strict_types=1);

namespace App\Filament\Resources\Disputes\Pages;

use App\Filament\Resources\Disputes\DisputeResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Payments\Actions\RecordRefundAction;
use App\Modules\Support\Actions\ResolveDisputeAction;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Support\Exceptions\DomainException;
use Filament\Actions\Action;
use Filament\Forms\Components\Select;
use Filament\Forms\Components\Textarea;
use Filament\Forms\Components\TextInput;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\ViewRecord;
use Filament\Support\Icons\Heroicon;

/**
 * قرار النزاع — 16 §قرار النزاع، 23: مدير التشغيل يحسم بلا استرداد، والمدير العام وحده يختار الاسترداد.
 * وبعد قرار باسترداد يسجل المدير العام مبلغ الاسترداد نفسه (DEC-066).
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

            // DEC-019، DEC-066 — بعد تنفيذ الاسترداد فعلًا في البوابة أو البنك؛ للمدير العام وحده.
            Action::make('recordRefund')
                ->label('تسجيل الاسترداد')
                ->icon(Heroicon::OutlinedReceiptRefund)
                ->color('warning')
                ->visible(fn (): bool => $this->refundable() !== null)
                ->modalDescription(fn (): string => 'المتبقي للاسترداد: '.($this->refundable()['total'] ?? '0.00')
                    .' جنيه. سجّل بعد تنفيذ الاسترداد في فوري أو من حساب المنصة؛ العمولة تُعاد حسابها (BR-060).')
                ->fillForm(fn (): array => [
                    'labor_amount' => $this->refundable()['labor'] ?? '0.00',
                    'materials_amount' => $this->refundable()['materials'] ?? '0.00',
                ])
                ->schema([
                    TextInput::make('labor_amount')->label('جزء المصنعية (جنيه)')->numeric()->minValue(0)->step('0.01')->required(),
                    TextInput::make('materials_amount')->label('جزء الخامات (جنيه)')->numeric()->minValue(0)->step('0.01')->required(),
                    TextInput::make('reference')->label('الرقم المرجعي للاسترداد')->required()->maxLength(64),
                    Textarea::make('reason')->label('السبب')->required()->minLength(5)->maxLength(500),
                ])
                ->action(function (array $data): void {
                    try {
                        app(RecordRefundAction::class)->execute(
                            $this->dispute(), $this->admin(), (string) $data['labor_amount'], (string) $data['materials_amount'],
                            (string) $data['reference'], (string) $data['reason'],
                        );
                    } catch (DomainException $e) {
                        Notification::make()->danger()->title($e->errorCode())->body($e->getMessage())->send();

                        return;
                    }

                    $this->record->refresh();
                    Notification::make()->success()->title('تم تسجيل الاسترداد (EVT-082).')->send();
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

    /** @return array{labor: string, materials: string, total: string}|null ما يمكن استرداده الآن، أو null إن لم يكن الإجراء متاحًا. */
    private function refundable(): ?array
    {
        $dispute = $this->dispute();

        if (! $this->admin()->isSuper()
            || $dispute->status !== DisputeStatus::Resolved
            || ! in_array($dispute->resolution, RecordRefundAction::REFUND_RESOLUTIONS, true)) {
            return null;
        }

        try {
            $remaining = app(RecordRefundAction::class)->remaining($dispute->order()->firstOrFail());
        } catch (DomainException) {
            return null;
        }

        return bccomp($remaining['total'], '0.00', 2) > 0 ? $remaining : null;
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
