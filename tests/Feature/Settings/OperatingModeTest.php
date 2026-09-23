<?php

declare(strict_types=1);

namespace Tests\Feature\Settings;

use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** 39-OPERATING-MODES — AC-PH1-09، BR-006، BR-009. */
final class OperatingModeTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function المنصة_تبدأ_في_وضع_الموظفين_بمعاينة_مجانية(): void
    {
        $features = app(FeatureGate::class);

        $this->assertSame(OperatingMode::Employee, $features->mode());
        $this->assertFalse($features->offersEnabled());
        $this->assertTrue($features->inspectionIsFree());
    }

    #[Test]
    public function تفعيل_عروض_السعر_ينقل_المنصة_لوضع_السوق(): void
    {
        $this->setOperatingFlags(offersEnabled: true);

        $this->assertSame(OperatingMode::Marketplace, app(FeatureGate::class)->mode());
    }

    /** AC-PH1-09 — BR-009. */
    #[Test]
    public function رسوم_المعاينة_لا_تُفعَّل_قبل_تفعيل_العروض(): void
    {
        $this->expectException(BusinessRuleViolationException::class);

        app(FeatureGate::class)->assertFlagCombination(
            offersEnabled: false,
            inspectionFeeEnabled: true,
        );
    }

    #[Test]
    public function القرارات_المفتوحة_تبقى_فارغة_حتى_تُحسم(): void
    {
        $settings = app(SettingsRepository::class);

        // OD-01 و OD-02 — لا يلزمان قبل تفعيل CFG-090 (35)
        $this->assertNull($settings->decimal(Cfg::DefaultCommissionRate));
        $this->assertNull($settings->decimal(Cfg::ProviderDebtLimit));
    }
}
