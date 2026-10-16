<?php

declare(strict_types=1);

namespace App\Filament\Resources\Providers\Pages;

use App\Filament\Resources\Providers\ProviderProfileResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Providers\Actions\EnableEmployeeOnboarding;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\DomainException;
use Filament\Actions\Action;
use Filament\Forms\Components\Select;
use Filament\Notifications\Notification;
use Filament\Resources\Pages\ListRecords;
use Filament\Schemas\Components\Tabs\Tab;
use Illuminate\Database\Eloquent\Builder;

final class ListProviderProfiles extends ListRecords
{
    protected static string $resource = ProviderProfileResource::class;

    public function getTitle(): string
    {
        return 'مقدمو الخدمة';
    }

    protected function getHeaderActions(): array
    {
        return [
            Action::make('enableEmployeeOnboarding')
                ->label('إتاحة استكمال بيانات موظف')
                ->modalDescription('اختر حساب الموظف المسجّل. يستكمل بيانات الفني داخل التطبيق، ثم تراجعه الإدارة قبل استقبال العمل.')
                ->schema([
                    Select::make('user_id')->label('حساب الموظف')->required()->searchable()
                        ->getSearchResultsUsing(fn (string $search): array => User::query()
                            ->whereDoesntHave('providerProfile')
                            ->where(fn (Builder $query) => $query->where('name', 'like', '%'.$search.'%')->orWhere('email', 'like', '%'.$search.'%')->orWhere('phone', 'like', '%'.$search.'%'))
                            ->limit(30)->get()->mapWithKeys(fn (User $user): array => [$user->getKey() => $user->name.' — '.$user->email])->all())
                        ->getOptionLabelUsing(fn ($value): ?string => User::query()->find($value)?->name),
                ])
                ->action(function (array $data): void {
                    /** @var Admin $admin */
                    $admin = auth('admin')->user();
                    try {
                        app(EnableEmployeeOnboarding::class)->execute(User::query()->findOrFail($data['user_id']), $admin);
                    } catch (DomainException $exception) {
                        Notification::make()->danger()->title($exception->getMessage())->send();

                        return;
                    }
                    Notification::make()->success()->title('أصبح استكمال بيانات الفني متاحًا لهذا الموظف داخل التطبيق.')->send();
                }),
        ];
    }

    /** @return array<string, Tab> */
    public function getTabs(): array
    {
        return [
            'review' => Tab::make('بانتظار المراجعة')
                ->modifyQueryUsing(fn (Builder $query) => $query->where('status', ProviderStatus::PendingReview->value)->whereNotNull('submitted_at'))
                ->badge(fn (): int => ProviderProfile::query()->where('status', ProviderStatus::PendingReview->value)->whereNotNull('submitted_at')->count())
                ->badgeColor('warning'),
            'completion' => Tab::make('بانتظار استكمال البيانات')
                ->modifyQueryUsing(fn (Builder $query) => $query->employees()->where('status', ProviderStatus::PendingReview->value)->whereNull('submitted_at')),
            'active' => Tab::make('النشطون')
                ->modifyQueryUsing(fn (Builder $query) => $query->where('status', ProviderStatus::Active->value)),
            'all' => Tab::make('الكل'),
        ];
    }
}
