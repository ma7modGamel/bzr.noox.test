<?php

declare(strict_types=1);

namespace App\Filament\Resources\Orders\Pages;

use App\Filament\Resources\Orders\OrderResource;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Services\FeatureGate;
use Filament\Resources\Pages\ListRecords;
use Filament\Schemas\Components\Tabs\Tab;
use Illuminate\Database\Eloquent\Builder;

final class ListOrders extends ListRecords
{
    protected static string $resource = OrderResource::class;

    public function getTitle(): string
    {
        return 'الطلبات';
    }

    /** @return array<string, Tab> */
    public function getTabs(): array
    {
        $employeeMode = ! app(FeatureGate::class)->offersEnabled();

        $tabs = [
            'active' => Tab::make('النشطة')
                ->modifyQueryUsing(fn (Builder $query) => $query->active())
                ->badge(fn (): int => Order::query()->active()->count()),
        ];

        // تبويب لوحة التشغيل الأول في وضع الموظفين: الطلبات بلا تعيين (39، 16)
        if ($employeeMode) {
            $tabs['awaiting_assignment'] = Tab::make('بانتظار التعيين')
                ->modifyQueryUsing(fn (Builder $query) => $query->awaitingAssignment())
                ->badge(fn (): int => Order::query()->awaitingAssignment()->count())
                ->badgeColor('warning');
        }

        return $tabs + [
            'disputed' => Tab::make('نزاعات')
                ->modifyQueryUsing(fn (Builder $query) => $query->where('status', OrderStatus::Disputed->value))
                ->badge(fn (): int => Order::query()->where('status', OrderStatus::Disputed->value)->count())
                ->badgeColor('danger'),
            'closed' => Tab::make('المغلقة')
                ->modifyQueryUsing(fn (Builder $query) => $query->where('status', OrderStatus::Closed->value)),
            'all' => Tab::make('الكل'),
        ];
    }
}
