<?php

declare(strict_types=1);

namespace App\Modules\Orders\Actions;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\TrackingPoint;
use App\Modules\Orders\Services\OrderEtaService;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;

/**
 * BR-110 — الفني يرسل موقعه كل CFG-080 في `ON_THE_WAY` **فقط**، ويراه العميل صاحب الطلب وحده.
 * ليس انتقالًا: لا يمر بآلة الحالات.
 */
final readonly class RecordLocationAction
{
    public function __construct(
        private SettingsRepository $settings,
        private OrderEtaService $eta,
    ) {}

    public function execute(Order $order, ProviderProfile $provider, float $lat, float $lng): TrackingPoint
    {
        if ($order->provider_profile_id !== $provider->getKey()) {
            throw BusinessRuleViolationException::rule('BR-110', 'هذا الطلب غير مسنَد إليك.');
        }

        if ($order->status !== OrderStatus::OnTheWay) {
            throw BusinessRuleViolationException::rule(
                'BR-110',
                'إرسال الموقع متاح أثناء "في الطريق" فقط.',
            );
        }

        $minimumInterval = $this->settings->int(Cfg::LocationUpdateSeconds);
        $wasRecordedTooRecently = $order->trackingPoints()
            ->where('recorded_at', '>', now()->subSeconds($minimumInterval))
            ->exists();

        if ($wasRecordedTooRecently) {
            throw BusinessRuleViolationException::rule('BR-110', 'تحديث الموقع أسرع من المعدل المسموح.');
        }

        $point = TrackingPoint::query()->create([
            'order_id' => $order->getKey(),
            'lat' => $lat,
            'lng' => $lng,
            'recorded_at' => now(),
        ]);

        $this->eta->refreshIfDue($order, $lat, $lng);

        return $point;
    }
}
