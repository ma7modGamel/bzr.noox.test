<?php

declare(strict_types=1);

namespace Tests\Feature\Admin;

use App\Filament\Pages\OperatingSettings;
use App\Modules\Identity\Models\Admin;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** صفحة الإعدادات تعرض القيم المحفوظة وتحفظ التعديل (04 §الإعدادات). */
final class OperatingSettingsPageTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->actingAs(Admin::factory()->super()->create(), 'admin');
    }

    #[Test]
    public function الحقول_تعرض_القيم_الافتراضية(): void
    {
        Livewire::test(OperatingSettings::class)
            ->assertSchemaStateSet([
                'offers__window_now_minutes' => 30,
                'scheduling__service_hours_from' => '08:00',
                'scheduling__slots' => ['09:00' => '11:00', '11:00' => '13:00', '13:00' => '15:00', '15:00' => '17:00', '17:00' => '19:00', '19:00' => '21:00'],
                'payments__channel_cash_enabled' => true,
                'payments__instapay_address' => null,
                'settlements__payout_cycle' => 'weekly',
            ]);
    }

    #[Test]
    public function الحفظ_يكتب_القيمة_الجديدة(): void
    {
        Livewire::test(OperatingSettings::class)
            ->fillForm(['scheduling__max_open_orders' => 5, 'payments__instapay_address' => 'bremo@instapay', 'payments__instapay_display_name' => 'Bremo'])
            ->call('save')
            ->assertHasNoFormErrors();

        $settings = app(SettingsRepository::class);
        $this->assertSame(5, (int) $settings->get(Cfg::MaxOpenOrdersPerCustomer));
        $this->assertSame('bremo@instapay', $settings->get(Cfg::InstapayAddress));
        $this->assertSame('Bremo', $settings->get(Cfg::InstapayDisplayName));
        $this->getJson('/api/v1/config')->assertOk()->assertJsonPath('instapay.address', 'bremo@instapay')->assertJsonPath('instapay.display_name', 'Bremo');
        $this->assertSame(30, (int) $settings->get(Cfg::OffersWindowNowMinutes));
        $this->assertEquals(Cfg::ScheduledSlots->default(), $settings->array(Cfg::ScheduledSlots));
        $this->assertSame('08:00', $settings->get(Cfg::ServiceHoursFrom));
        $this->assertNull($settings->get(Cfg::DefaultCommissionRate));
    }

    #[Test]
    public function اعدادات_كود_الوصول_تحفظ_كقيم_صحيحة(): void
    {
        Livewire::test(OperatingSettings::class)
            ->fillForm([
                'execution__arrival_code_validity_minutes' => 15,
                'execution__arrival_code_max_attempts' => 3,
                'execution__arrival_code_lock_minutes' => 10,
            ])
            ->call('save')
            ->assertHasNoFormErrors();

        $settings = app(SettingsRepository::class);
        $this->assertSame(15, $settings->get(Cfg::ArrivalCodeValidityMinutes));
        $this->assertSame(3, $settings->get(Cfg::ArrivalCodeMaxAttempts));
        $this->assertSame(10, $settings->get(Cfg::ArrivalCodeLockMinutes));
    }

    #[Test]
    public function اعدادات_كود_الوصول_ترفض_القيم_خارج_الحدود_دون_حفظ(): void
    {
        Livewire::test(OperatingSettings::class)
            ->fillForm([
                'execution__arrival_code_validity_minutes' => 0,
                'execution__arrival_code_max_attempts' => 11,
                'execution__arrival_code_lock_minutes' => 61,
            ])
            ->call('save')
            ->assertHasFormErrors([
                'execution__arrival_code_validity_minutes' => 'min',
                'execution__arrival_code_max_attempts' => 'max',
                'execution__arrival_code_lock_minutes' => 'max',
            ]);

        $settings = app(SettingsRepository::class);
        $this->assertSame(10, $settings->get(Cfg::ArrivalCodeValidityMinutes));
        $this->assertSame(5, $settings->get(Cfg::ArrivalCodeMaxAttempts));
        $this->assertSame(5, $settings->get(Cfg::ArrivalCodeLockMinutes));
    }
}
