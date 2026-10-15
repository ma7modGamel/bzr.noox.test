<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class OrderWaitingPresentationTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
    }

    #[Test]
    public function staff_receives_actual_assignment_deadline_without_technician_counts_or_progress(): void
    {
        $order = Order::factory()->create();
        $response = $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk();
        $response->assertJsonPath('data.waiting.message', 'طلبك في انتظار تعيين فني من الإدارة.')
            ->assertJsonPath('data.waiting.deadline_at', $order->selection_deadline_at->toIso8601String());
        $this->assertSame(['message', 'deadline_label', 'deadline_at'], array_keys($response->json('data.waiting')));
        $order->update(['selection_deadline_at' => now()->subMinute()]);
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.waiting.message', 'انتهت المهلة المحددة. حدّث حالة الطلب لمعرفة الإجراءات المتاحة.');
    }

    #[Test]
    public function marketplace_shows_offer_window_then_no_offers_without_a_fake_deadline(): void
    {
        $order = Order::factory()->marketplace()->create();
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()
            ->assertJsonPath('data.waiting.message', 'طلبك مفتوح لاستقبال عروض الفنيين.')
            ->assertJsonPath('data.waiting.deadline_at', $order->offers_close_at->toIso8601String());
        $order->update(['offers_close_at' => now()->subMinute()]);
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.waiting.message', 'انتهت مهلة استقبال العروض ولم يصل عرض متاح.')
            ->assertJsonPath('data.waiting.deadline_at', null);
        $order->offers()->create(['provider_profile_id' => ProviderProfile::factory()->create()->id, 'source' => 'PROVIDER', 'status' => 'SUBMITTED', 'price' => 300, 'submitted_at' => now()]);
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.waiting.message', 'انتهت مهلة استقبال العروض. يمكنك اختيار عرض من العروض المتاحة.')
            ->assertJsonPath('data.waiting.deadline_at', $order->selection_deadline_at->toIso8601String());
    }

    #[Test]
    public function assigned_orders_do_not_advertise_an_assignment_wait(): void
    {
        $order = Order::factory()->status(OrderStatus::Confirmed)->create(['provider_profile_id' => ProviderProfile::factory()]);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.waiting', null);
    }
}
