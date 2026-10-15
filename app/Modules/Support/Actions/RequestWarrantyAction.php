<?php

declare(strict_types=1);

namespace App\Modules\Support\Actions;

use App\Filament\Resources\Orders\OrderResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Support\Models\WarrantyClaim;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Filament\Actions\Action;
use Filament\Notifications\Notification;
use Illuminate\Support\Facades\DB;

final class RequestWarrantyAction
{
    public static function eligible(Order $order): bool
    {
        return ! $order->isWarrantyVisit() && $order->status === OrderStatus::Closed
            && $order->work_started_at !== null && $order->completed_at !== null
            && $order->provider_profile_id !== null && now()->gte($order->completed_at)
            && now()->lte($order->completed_at->addDays(14));
    }

    public function execute(Order $order, User $customer, string $description): WarrantyClaim
    {
        return DB::transaction(function () use ($order, $customer, $description): WarrantyClaim {
            $fresh = Order::query()->lockForUpdate()->findOrFail($order->getKey());
            if ($fresh->customer_id !== $customer->getKey() || ! self::eligible($fresh)) {
                throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_unavailable'));
            }
            if ($fresh->warrantyClaims()->whereIn('status', ['REQUESTED', 'ASSIGNED'])->exists()) {
                throw BusinessRuleViolationException::rule('WARRANTY-14', Localization::text('copy.warranty_pending'));
            }
            $claim = $fresh->warrantyClaims()->create(['description' => $description, 'status' => 'REQUESTED']);
            DB::afterCommit(function () use ($fresh): void {
                Notification::make()->warning()->title('طلب ضمان — #'.$fresh->number)
                    ->body('راجع عودة العطل وتواصل مع العميل لتنسيق زيارة فني مختلف.')
                    ->actions([Action::make('view')->label('مراجعة الطلب')->url(OrderResource::getUrl('view', ['record' => $fresh->id], panel: 'admin'))])
                    ->sendToDatabase(Admin::query()->where('is_active', true)->get());
            });

            return $claim;
        });
    }
}
