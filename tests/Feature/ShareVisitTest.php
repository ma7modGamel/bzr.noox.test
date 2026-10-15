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
        $provider->user->forceFill(['name' => 'محمد السيد', 'phone' => '01088888888'])->save();
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
            ->assertDontSee('01088888888')
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
        $order = app(MarkArrivedAction::class)->execute($order, $provider, (float) $order->lat, (float) $order->lng, arrivalCode: $this->arrivalCode($order));
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

    #[Test]
    public function customer_creates_reuses_and_revokes_a_temporary_visit_link(): void
    {
        $this->freezeTime();
        [$order, , $customer] = $this->assignedOrder();
        $this->postJson("/api/v1/orders/{$order->id}/share-links")->assertUnauthorized();
        $this->actingAs(User::factory()->create(), 'sanctum')->postJson("/api/v1/orders/{$order->id}/share-links")->assertNotFound();
        $response = $this->actingAs($customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/share-links")->assertCreated();
        $link = ShareLink::query()->findOrFail($response->json('data.id'));
        $response->assertJsonPath('data.expires_at', now()->addHours(24)->toIso8601String());
        $this->postJson("/api/v1/orders/{$order->id}/share-links")->assertCreated()->assertJsonPath('data.id', $link->id);
        $this->assertDatabaseCount('share_links', 1);
        $this->deleteJson("/api/v1/share-links/{$link->id}")->assertNoContent();
        $this->get($response->json('data.url'))->assertStatus(410);
        $replacement = $this->postJson("/api/v1/orders/{$order->id}/share-links")->assertCreated();
        $this->assertNotSame($link->id, $replacement->json('data.id'));
        $this->travel(24)->hours();
        $this->get($replacement->json('data.url'))->assertStatus(410);
    }

    #[Test]
    public function legacy_links_expire_and_a_reopened_order_cannot_be_shared(): void
    {
        $this->freezeTime();
        [$order] = $this->assignedOrder();
        $legacy = $this->shareLink($order);
        $legacy->forceFill(['created_at' => now()->subHours(24)])->save();
        $this->get(route('share.visit', $legacy->token))->assertStatus(410);
        $active = $this->shareLink($order);
        $order->update(['status' => OrderStatus::Open, 'provider_profile_id' => null]);
        $this->get(route('share.visit', $active->token))->assertStatus(410);
        $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/share-links")->assertUnprocessable();
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
