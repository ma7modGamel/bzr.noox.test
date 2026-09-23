<?php

declare(strict_types=1);

namespace App\Filament\Widgets;

use App\Filament\Resources\Orders\OrderResource;
use App\Filament\Resources\Providers\ProviderProfileResource;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use Filament\Widgets\StatsOverviewWidget;
use Filament\Widgets\StatsOverviewWidget\Stat;

/**
 * عدّادات اللوحة الرئيسية — 16: كل عدّاد قابل للنقر ويخدم قرارًا تشغيليًا.
 * العدّاد الأول يتبدل حسب وضع التشغيل: "بلا تعيين" (موظفين) أو "بلا عروض" (سوق).
 */
final class OperationsOverview extends StatsOverviewWidget
{
    protected ?string $heading = 'اليوم';

    protected function getStats(): array
    {
        $features = app(FeatureGate::class);
        $settings = app(SettingsRepository::class);

        return [
            $features->offersEnabled()
                ? $this->withoutOffersStat()
                : $this->awaitingAssignmentStat($settings),

            $this->disputesStat(),
            $this->pendingProvidersStat(),
            $this->latePaymentsStat($settings),
        ];
    }

    /** وضع الموظفين: الطلبات التي تنتظر قرار إنسان قبل انتهاء CFG-093. */
    private function awaitingAssignmentStat(SettingsRepository $settings): Stat
    {
        $count = Order::query()->awaitingAssignment()->count();
        $alertAfter = $settings->int(Cfg::AssignmentAlertNowMinutes);

        $overdue = Order::query()
            ->awaitingAssignment()
            ->where('created_at', '<=', now()->subMinutes($alertAfter))
            ->count();

        return Stat::make('طلبات بلا تعيين', (string) $count)
            ->description($overdue > 0 ? "منها {$overdue} تجاوزت مهلة التنبيه (CFG-092)" : 'ضمن المهلة')
            ->descriptionColor($overdue > 0 ? 'danger' : 'success')
            ->color($count > 0 ? 'warning' : 'gray')
            ->url(OrderResource::getUrl('index', ['activeTab' => 'awaiting_assignment']));
    }

    /** وضع السوق: الطلبات التي انتهت نافذتها بلا عروض (EC-02). */
    private function withoutOffersStat(): Stat
    {
        $count = Order::query()
            ->where('status', OrderStatus::Open->value)
            ->whereDoesntHave('offers')
            ->count();

        return Stat::make('طلبات بلا عروض', (string) $count)
            ->description('دعوة فنيين أو تعديل الطلب (08)')
            ->color($count > 0 ? 'warning' : 'gray')
            ->url(OrderResource::getUrl('index'));
    }

    private function disputesStat(): Stat
    {
        $count = Order::query()->where('status', OrderStatus::Disputed->value)->count();

        return Stat::make('نزاعات مفتوحة', (string) $count)
            ->description('قرار الإدارة مطلوب (T-23 / T-24)')
            ->color($count > 0 ? 'danger' : 'gray')
            ->url(OrderResource::getUrl('index', ['activeTab' => 'disputed']));
    }

    private function pendingProvidersStat(): Stat
    {
        $count = ProviderProfile::query()
            ->where('status', ProviderStatus::PendingReview->value)
            ->count();

        return Stat::make('مقدمو خدمة بانتظار المراجعة', (string) $count)
            ->description('مستندات + اتصال لتوثيق الهاتف (DEC-033)')
            ->color($count > 0 ? 'warning' : 'gray')
            ->url(ProviderProfileResource::getUrl('index'));
    }

    private function latePaymentsStat(SettingsRepository $settings): Stat
    {
        $hours = $settings->int(Cfg::PaymentDelayAlertHours);

        $count = Order::query()
            ->where('status', OrderStatus::AwaitingPayment->value)
            ->where('completed_at', '<=', now()->subHours($hours))
            ->count();

        return Stat::make("تأخر الدفع > {$hours} ساعة", (string) $count)
            ->description('يظهر للإدارة ولا يُغلق تلقائيًا (BR-054)')
            ->color($count > 0 ? 'danger' : 'gray')
            ->url(OrderResource::getUrl('index'));
    }
}
