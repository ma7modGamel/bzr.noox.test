<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Communication\Models\Conversation;
use App\Modules\Communication\Models\Message;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class CustomerCommunicationHistoryTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function قائمة_المحادثات_تعيد_بيانات_الفني_والطلب_وآخر_رسالة(): void
    {
        [$order, $customer, $provider, $conversation] = $this->assignedOrder();
        $message = Message::query()->create([
            'conversation_id' => $conversation->id,
            'sender_user_id' => $provider->user_id,
            'body' => 'تم الوصول إلى العنوان',
        ]);

        $this->actingAs($customer, 'sanctum')
            ->getJson('/api/v1/conversations')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $conversation->id)
            ->assertJsonPath('data.0.order.id', $order->id)
            ->assertJsonPath('data.0.order.number', $order->number)
            ->assertJsonPath('data.0.provider.id', $provider->id)
            ->assertJsonPath('data.0.last_message.id', $message->id)
            ->assertJsonPath('data.0.last_message.body', 'تم الوصول إلى العنوان')
            ->assertJsonPath('data.0.last_message.is_mine', false)
            ->assertJsonPath('meta.current_page', 1);
    }

    #[Test]
    public function صفحة_المحادثة_تعيد_أحدث_خمسين_رسالة_مرتبة_زمنيا(): void
    {
        [, $customer, , $conversation] = $this->assignedOrder();

        foreach (range(1, 55) as $number) {
            Message::query()->create([
                'conversation_id' => $conversation->id,
                'sender_user_id' => $customer->id,
                'body' => "رسالة {$number}",
            ]);
        }

        $response = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/conversations/{$conversation->id}/messages")
            ->assertOk()
            ->assertJsonCount(50, 'data')
            ->assertJsonPath('data.0.body', 'رسالة 6')
            ->assertJsonPath('data.49.body', 'رسالة 55')
            ->assertJsonPath('data.49.is_mine', true)
            ->assertJsonPath('conversation.status', 'OPEN')
            ->assertJsonPath('meta.last_page', 2);

        $ids = array_column($response->json('data'), 'id');
        $sorted = $ids;
        sort($sorted);

        $this->assertSame($sorted, $ids);
    }

    #[Test]
    public function قائمة_طلباتي_تفصل_الحالية_عن_السابقة_ولا_تعيد_طلبات_عميل_آخر(): void
    {
        $customer = User::factory()->create();
        $current = Order::factory()->status(OrderStatus::Confirmed)->create(['customer_id' => $customer->id]);
        $past = Order::factory()->status(OrderStatus::Closed)->create([
            'customer_id' => $customer->id,
            'closed_at' => now()->subDay(),
        ]);
        Order::factory()->status(OrderStatus::Closed)->create();

        $this->actingAs($customer, 'sanctum')
            ->getJson('/api/v1/orders?scope=current')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $current->id);

        $this->actingAs($customer, 'sanctum')
            ->getJson('/api/v1/orders?scope=past')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.id', $past->id)
            ->assertJsonPath('data.0.termination.closed_at', $past->closed_at?->toIso8601String());
    }

    #[Test]
    public function الطلب_المغلق_يعيد_فتح_مشكلة_داخل_المهلة_فقط_ومن_دون_ملف_مفتوح(): void
    {
        $customer = User::factory()->create();
        $eligible = Order::factory()->status(OrderStatus::Closed)->create([
            'customer_id' => $customer->id,
            'closed_at' => now()->subHours(24),
        ]);
        $expired = Order::factory()->status(OrderStatus::Closed)->create([
            'customer_id' => $customer->id,
            'closed_at' => now()->subHours(73),
        ]);

        $eligibleResponse = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$eligible->id}")
            ->assertOk();
        $this->assertContains('open_dispute', $eligibleResponse->json('data.available_actions'));

        $expiredResponse = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$expired->id}")
            ->assertOk();
        $this->assertNotContains('open_dispute', $expiredResponse->json('data.available_actions'));

        Dispute::query()->create([
            'order_id' => $eligible->id,
            'opened_by_type' => 'CUSTOMER',
            'opened_by_id' => $customer->id,
            'reason_code' => 'QUALITY',
            'description' => 'وصف المشكلة الحالية',
            'is_post_close' => true,
            'status' => DisputeStatus::Open,
        ]);

        $existingResponse = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$eligible->id}")
            ->assertOk();
        $this->assertNotContains('open_dispute', $existingResponse->json('data.available_actions'));
    }

    /** @return array{Order, User, ProviderProfile, Conversation} */
    private function assignedOrder(): array
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();
        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());
        $conversation = Conversation::query()->where('order_id', $order->id)->sole();

        return [$order, $customer, $provider, $conversation];
    }
}
