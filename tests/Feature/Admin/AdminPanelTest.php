<?php

declare(strict_types=1);

namespace Tests\Feature\Admin;

use App\Filament\Pages\OperatingSettings;
use App\Filament\Resources\Orders\OrderResource;
use App\Filament\Widgets\OperationsOverview;
use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Models\Order;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** لوحة التشغيل — 16، و23 §فصل المال والإعدادات للمدير العام. */
final class AdminPanelTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function اللوحة_محمية_والدخول_يعيد_التوجيه(): void
    {
        $this->get('/admin')->assertRedirect();
        $this->get('/admin/login')->assertOk()->assertSee('rtl', false);
    }

    #[Test]
    public function مدير_التشغيل_يرى_الطلبات(): void
    {
        Order::factory()->count(3)->create();

        $this->actingAs(Admin::factory()->create(), 'admin')
            ->get(OrderResource::getUrl('index'))
            ->assertOk();
    }

    /** AC-ADM-01 — الإعدادات للمدير العام وحده. */
    #[Test]
    public function الإعدادات_ممنوعة_على_مدير_التشغيل(): void
    {
        $this->actingAs(Admin::factory()->create(), 'admin')
            ->get(OperatingSettings::getUrl())
            ->assertForbidden();
    }

    #[Test]
    public function المدير_العام_يفتح_الإعدادات(): void
    {
        $this->actingAs(Admin::factory()->super()->create(), 'admin')
            ->get(OperatingSettings::getUrl())
            ->assertOk();
    }

    /** شريط وضع التشغيل يُعرض مع الصفحة نفسها لا بتحميل كسول (39). */
    #[Test]
    public function اللوحة_الرئيسية_تعرض_وضع_التشغيل(): void
    {
        Order::factory()->count(2)->create();

        $this->actingAs(Admin::factory()->create(), 'admin')
            ->get('/admin')
            ->assertOk()
            ->assertSee('وضع الموظفين')
            ->assertSee('المعاينة')
            ->assertSee('bzr-mode-badge', false);
    }

    /** عدّادات التشغيل — ويدجت كسول، فيُختبر كمكوّن مستقل (16). */
    #[Test]
    public function عدادات_التشغيل_تعرض_الطلبات_بلا_تعيين(): void
    {
        Order::factory()->count(2)->create();

        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(OperationsOverview::class)
            ->assertOk()
            ->assertSee('طلبات بلا تعيين')
            ->assertSee('نزاعات مفتوحة');
    }

    #[Test]
    public function صفحة_الطلب_تفتح_وتعرض_وضع_التشغيل(): void
    {
        $order = Order::factory()->create();

        $this->actingAs(Admin::factory()->create(), 'admin')
            ->get(OrderResource::getUrl('view', ['record' => $order]))
            ->assertOk()
            ->assertSee('وضع الموظفين');
    }
}
