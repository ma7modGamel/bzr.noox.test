<?php

declare(strict_types=1);

namespace Tests;

use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ArrivalVerification;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Foundation\Testing\TestCase as BaseTestCase;

abstract class TestCase extends BaseTestCase
{
    /** Existing fixtures describe Arabic responses; Symfony otherwise supplies en-US automatically. */
    protected $serverVariables = ['HTTP_ACCEPT_LANGUAGE' => 'ar'];

    /** يضبط مفاتيح المرحلة لاختبار بعينه (39). */
    protected function setOperatingFlags(bool $offersEnabled, bool $inspectionFeeEnabled = false): void
    {
        $settings = app(SettingsRepository::class);

        $settings->setMany([
            Cfg::OffersEnabled->value => $offersEnabled,
            Cfg::InspectionFeeEnabled->value => $inspectionFeeEnabled,
        ]);
    }

    /** A real customer-issued code for journeys that include employee arrival verification. */
    protected function arrivalCode(Order $order): ?string
    {
        if ($order->operating_mode !== OperatingMode::Employee) {
            return null;
        }

        return app(ArrivalVerification::class)->issue($order, $order->customer)['code'];
    }
}
