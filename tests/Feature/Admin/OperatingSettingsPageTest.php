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
            ->fillForm(['scheduling__max_open_orders' => 5, 'payments__instapay_address' => 'bremo@instapay'])
            ->call('save')
            ->assertHasNoFormErrors();

        $settings = app(SettingsRepository::class);
        $this->assertSame(5, (int) $settings->get(Cfg::MaxOpenOrdersPerCustomer));
        $this->assertSame('bremo@instapay', $settings->get(Cfg::InstapayAddress));
        $this->assertSame(30, (int) $settings->get(Cfg::OffersWindowNowMinutes));
        $this->assertEquals(Cfg::ScheduledSlots->default(), $settings->array(Cfg::ScheduledSlots));
        $this->assertSame('08:00', $settings->get(Cfg::ServiceHoursFrom));
        $this->assertNull($settings->get(Cfg::DefaultCommissionRate));
    }
}
