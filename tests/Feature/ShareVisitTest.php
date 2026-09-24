<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\MarkArrivedAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\ShareLink;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Actions\OpenDisputeAction;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ShareVisitTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function الصفحة_العامة_تعرض_الحد_المسموح_فقط_وتمنع_الفهرسة_والتخزين(): void
    {
        [$order, $provider, $customer] = $this->assignedOrder();
        $provider->user->forceFill(['name' => 'محمد السيد'])->save();
        $customer->forceFill(['name' => 'اسم العميل السري', 'phone' => '01099999999'])->save();
        $order->forceFill([
            'status' => OrderStatus::OnTheWay,
            'address_text' => 'عنوان سري لا يظهر',
            'final_amount' => '9876.54',
            'eta_minutes' => 17,
            'eta_approximate' => true,
        ])->save();
        $link = $this->shareLink($order);

        $response = $this->get(route('share.visit', $link->token))
            ->assertOk()
            ->assertHeader('Cache-Control', 'no-store, private')
            ->assertHeader('X-Robots-Tag', 'noindex, nofollow, noarchive')
            ->assertSee('محمد')
            ->assertSee('فني سباكة موثّق')
            ->assertSee('17 دقيقة')
            ->assertSee('تقريبي')
            ->assertSee((string) $order->number)
            ->assertSee($order->area->name)
            ->assertSee('content="30"', false)
            ->assertDontSee('السيد')
            ->assertDontSee('اسم العميل السري')
            ->assertDontSee('01099999999')
            ->assertDontSee('عنوان سري لا يظهر')
            ->assertDontSee('9876.54');

        $this->assertStringNotContainsString((string) $order->lat, $response->getContent());
        $this->assertStringNotContainsString((string) $order->lng, $response->getContent());
    }

    #[Test]
    public function الرابط_الملغي_أو_النهائي_أو_غير_الموجود_يعيد_410(): void
    {
        [$order] = $this->assignedOrder();
        $revoked = $this->shareLink($order, ['revoked_at' => now()]);

        $this->get(route('share.visit', $revoked->token))
            ->assertStatus(410)
            ->assertSee('انتهت صلاحية هذا الرابط');

        $final = $this->shareLink($order);
        $order->forceFill(['status' => OrderStatus::Cancelled, 'cancelled_at' => now()])->save();

        $this->get(route('share.visit', $final->token))->assertStatus(410);
        $this->get('/v/not-a-real-token')->assertStatus(410);
    }

    #[Test]
    public function النزاع_يعرض_آخر_مرحلة_on_hold_وتنبيه_المراجعة(): void
    {
        [$order, $provider, $customer] = $this->assignedOrder();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, (float) $order->lat, (float) $order->lng);
        app(OpenDisputeAction::class)->execute(
            $order,
            ActorType::Customer,
            $customer->id,
            'QUALITY',
            'الفني لم يبدأ العمل',
        );

        $link = $this->shareLink($order->refresh());

        $this->get(route('share.visit', $link->token))
            ->assertOk()
            ->assertSee('class="step on_hold"', false)
            ->assertSee('قيد المراجعة');
    }

    /** @return array{Order, ProviderProfile, User} */
    private function assignedOrder(): array
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        return [$order, $provider, $customer];
    }

    /** @param array<string, mixed> $overrides */
    private function shareLink(Order $order, array $overrides = []): ShareLink
    {
        return ShareLink::query()->create($overrides + [
            'order_id' => $order->id,
            'token' => Str::random(43),
        ]);
    }
}
