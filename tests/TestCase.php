<?php

declare(strict_types=1);

namespace Tests;

use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Foundation\Testing\TestCase as BaseTestCase;

abstract class TestCase extends BaseTestCase
{
    /** يضبط مفاتيح المرحلة لاختبار بعينه (39). */
    protected function setOperatingFlags(bool $offersEnabled, bool $inspectionFeeEnabled = false): void
    {
        $settings = app(SettingsRepository::class);

        $settings->setMany([
            Cfg::OffersEnabled->value => $offersEnabled,
            Cfg::InspectionFeeEnabled->value => $inspectionFeeEnabled,
        ]);
    }
}
