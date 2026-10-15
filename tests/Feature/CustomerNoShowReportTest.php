<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Resources\Orders\Pages\ViewOrder;
use App\Modules\Communication\Services\ConversationService;
use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Actions\ReportUnableAction;
use App\Modules\Orders\Actions\ResolveCustomerNoShowAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderPresentation;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class CustomerNoShowReportTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
    }

    #[Test]
    public function provider_report_notifies_administration_once_without_cancelling_the_order(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        $admin = Admin::factory()->create();
        $inactiveAdmin = Admin::factory()->create(['is_active' => false]);
        $body = ['expected_version' => $order->version];

        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson('/api/v1/provider/orders/'.$order->id.'/customer-no-show', $body)
            ->assertOk()->assertJsonPath('data.status', 'ARRIVED')->assertJsonPath('data.absence_report_pending', true);
        $this->postJson('/api/v1/provider/orders/'.$order->id.'/customer-no-show', $body)->assertOk();
        $this->postJson('/api/v1/provider/orders/'.$order->id.'/complete-inspection-only')->assertUnprocessable();

        $this->assertDatabaseCount('order_events', 1);
        $this->assertSame(OrderEventCode::CustomerNoShowReported, $order->events()->sole()->event_code);
        $this->assertNull($order->refresh()->cancelled_at);
        $this->assertSame(1, $admin->notifications()->count());
        $this->assertSame(0, $inactiveAdmin->notifications()->count());
        $this->assertNotContains('report_no_show', app(OrderPresentation::class)->availableActions($order, $provider->user, ActorType::Provider));
    }

    #[Test]
    public function rejects_reports_before_the_waiting_period_and_from_another_provider(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        $order->update(['arrived_at' => now()->subMinutes(19)]);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson('/api/v1/provider/orders/'.$order->id.'/customer-no-show')->assertUnprocessable();

        $other = ProviderProfile::factory()->create();
        $this->actingAs($other->user, 'sanctum')->postJson('/api/v1/provider/orders/'.$order->id.'/customer-no-show')->assertNotFound();
        $this->assertDatabaseCount('order_events', 0);
        $this->assertSame(OrderStatus::Arrived, $order->refresh()->status);
    }

    #[Test]
    public function administration_records_contact_and_cancels_the_confirmed_absence(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        app(ReportUnableAction::class)->customerNoShow($order, $provider);
        $admin = Admin::factory()->create();
        $this->actingAs($admin, 'admin');

        Livewire::test(ViewOrder::class, ['record' => $order->id])
            ->assertActionVisible('resolveNoShow')
            ->callAction('resolveNoShow', ['outcome' => 'CANCEL', 'contact_note' => 'اتصلت بالعميل وأكد عدم وجوده في المنزل.'])
            ->assertHasNoActionErrors();

        $this->assertDatabaseHas('orders', ['id' => $order->id, 'status' => 'CANCELLED', 'cancelled_by_type' => 'ADMIN', 'cancelled_by_id' => $admin->id, 'cancel_reason_code' => 'CUSTOMER_NO_SHOW']);
        $event = $order->events()->where('event_code', OrderEventCode::CustomerNoShowResolved->value)->sole();
        $this->assertSame(ActorType::Admin, $event->actor_type);
        $this->assertSame('اتصلت بالعميل وأكد عدم وجوده في المنزل.', $event->meta['contact_note']);
    }

    #[Test]
    public function administration_can_continue_the_visit_after_contacting_the_customer(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        app(ReportUnableAction::class)->customerNoShow($order, $provider);
        $admin = Admin::factory()->create();

        $result = app(ResolveCustomerNoShowAction::class)->execute($order, $admin, false, 'العميل موجود وتم توجيهه للرسائل مع الفني.');

        $this->assertSame(OrderStatus::Arrived, $result->status);
        $this->assertFalse($result->hasPendingNoShowReport());
        $this->assertDatabaseHas('order_events', ['order_id' => $order->id, 'event_code' => 'EVT-094', 'actor_id' => $admin->id]);
    }

    #[Test]
    public function administration_cannot_resolve_without_a_contact_result(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        app(ReportUnableAction::class)->customerNoShow($order, $provider);
        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(ViewOrder::class, ['record' => $order->id])
            ->callAction('resolveNoShow', ['outcome' => 'CANCEL', 'contact_note' => ''])
            ->assertHasActionErrors(['contact_note' => 'required']);

        $this->assertSame(OrderStatus::Arrived, $order->refresh()->status);
        $this->assertTrue($order->hasPendingNoShowReport());
    }

    #[Test]
    public function rejects_a_stale_second_resolution(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        app(ReportUnableAction::class)->customerNoShow($order, $provider);
        $admin = Admin::factory()->create();
        app(ResolveCustomerNoShowAction::class)->execute($order, $admin, false, 'تم التواصل مع العميل وهو موجود.');

        $this->expectException(BusinessRuleViolationException::class);
        app(ResolveCustomerNoShowAction::class)->execute($order, $admin, true, 'محاولة ثانية ببيانات قديمة.');
    }

    #[Test]
    public function keeps_phone_numbers_private_and_masks_contact_details_after_confirmation(): void
    {
        [$order, $provider] = $this->arrivedOrder();
        $customer = $order->customer;
        $this->actingAs($customer, 'sanctum')->getJson('/api/v1/orders/'.$order->id)
            ->assertOk()->assertJsonPath('data.provider.phone', null);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/orders/'.$order->id)->assertOk()->assertJsonPath('data.customer.phone', null);
        $this->assertNotContains('call', app(OrderPresentation::class)->availableActions($order, $customer, ActorType::Customer));

        $conversation = app(ConversationService::class)->ensureAssignedConversation($order, $provider->id);
        $message = app(ConversationService::class)->send($conversation, $customer, 'اتصل بي على 01012345678', []);
        $this->assertSame('اتصل بي على •••', $message->body);
        $this->assertTrue($message->was_masked);
    }

    /** @return array{Order, ProviderProfile} */
    private function arrivedOrder(): array
    {
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Arrived)->create([
            'provider_profile_id' => $provider->id, 'arrived_at' => now()->subMinutes(20),
        ]);

        return [$order->refresh(), $provider];
    }
}
