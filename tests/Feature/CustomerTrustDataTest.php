<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Communication\Models\Conversation;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class CustomerTrustDataTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
    }

    #[Test]
    public function account_counts_all_owned_completed_and_active_orders_without_exposing_customer_rating(): void
    {
        $customer = User::factory()->create(['customer_rating_avg' => 4.9]);
        Order::factory()->count(24)->status(OrderStatus::Closed)->create(['customer_id' => $customer->id]);
        foreach ([OrderStatus::Open, OrderStatus::InProgress, OrderStatus::AwaitingConfirmation, OrderStatus::Disputed] as $status) {
            Order::factory()->status($status)->create(['customer_id' => $customer->id]);
        }
        foreach ([OrderStatus::Cancelled, OrderStatus::Expired] as $status) {
            Order::factory()->status($status)->create(['customer_id' => $customer->id]);
        }
        Order::factory()->status(OrderStatus::Closed)->create();
        $this->actingAs($customer, 'sanctum')->getJson('/api/v1/me')->assertOk()
            ->assertJsonPath('user.order_statistics.completed', 24)->assertJsonPath('user.order_statistics.active', 4)
            ->assertJsonPath('user.addresses_count', 0)->assertJsonMissingPath('user.rating_avg');
    }

    #[Test]
    public function unread_counts_only_incoming_messages_and_opening_chat_marks_the_displayed_page_read(): void
    {
        [$conversation, $customer, $provider] = $this->conversation();
        for ($index = 0; $index < 51; $index++) {
            $conversation->messages()->create(['sender_user_id' => $provider->user_id, 'body' => 'رسالة واردة', 'was_masked' => false]);
        }
        $own = $conversation->messages()->create(['sender_user_id' => $customer->id, 'body' => 'رسالة صادرة', 'was_masked' => false]);
        $this->actingAs($customer, 'sanctum')->getJson('/api/v1/conversations')->assertOk()
            ->assertJsonPath('data.0.unread_count', 51)->assertJsonPath('data.0.provider.available_now', true)
            ->assertJsonMissingPath('data.0.customer.rating_avg');
        $this->getJson("/api/v1/conversations/{$conversation->id}/messages")->assertOk()
            ->assertJsonPath('conversation.unread_count', 2)->assertJsonPath('data.0.read_at', now()->toIso8601String());
        $this->assertNull($own->refresh()->read_at);
        $this->getJson("/api/v1/conversations/{$conversation->id}/messages?page=2")->assertOk()->assertJsonPath('conversation.unread_count', 0);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->getJson('/api/v1/conversations')->assertOk()->assertJsonPath('data.0.unread_count', 1);
        $this->getJson("/api/v1/conversations/{$conversation->id}/messages")->assertOk()->assertJsonPath('conversation.unread_count', 0);
        $this->assertNotNull($own->refresh()->read_at);
    }

    #[Test]
    public function outsiders_cannot_mark_messages_read(): void
    {
        [$conversation, , $provider] = $this->conversation();
        $message = $conversation->messages()->create(['sender_user_id' => $provider->user_id, 'body' => 'رسالة خاصة', 'was_masked' => false]);
        $this->actingAs(User::factory()->create(), 'sanctum')->getJson("/api/v1/conversations/{$conversation->id}/messages")->assertNotFound();
        $this->assertNull($message->refresh()->read_at);
    }

    #[Test]
    public function unavailable_technician_does_not_get_a_presence_claim(): void
    {
        [$conversation, $customer, $provider] = $this->conversation();
        $provider->update(['available_now' => false]);
        $this->actingAs($customer, 'sanctum')->getJson('/api/v1/conversations')->assertOk()->assertJsonPath('data.0.provider.available_now', false);
    }

    #[Test]
    public function profile_images_are_served_only_inside_the_customers_order_context(): void
    {
        Storage::fake('local');
        [$conversation, $customer, $provider] = $this->conversation();
        $path = UploadedFile::fake()->image('profile.jpg')->store('avatars', 'local');
        $provider->user->update(['avatar_path' => $path]);
        $photo = UploadedFile::fake()->image('work.jpg')->store('portfolio', 'local');
        $provider->portfolioItems()->create(['image_path' => $photo, 'caption' => 'صيانة حوض', 'sort' => 1]);
        $response = $this->actingAs($customer, 'sanctum')->getJson("/api/v1/providers/{$provider->id}?order_id={$conversation->order_id}")->assertOk();
        $avatar = $response->json('data.avatar_url');
        $portfolio = $response->json('data.portfolio.0.image_url');
        $this->get($avatar)->assertOk()->assertHeader('Cache-Control', 'no-store, private');
        $this->get($portfolio)->assertOk()->assertHeader('Content-Type', 'image/jpeg');
        $foreignItem = ProviderProfile::factory()->create()->portfolioItems()->create(['image_path' => $photo, 'caption' => 'فني آخر', 'sort' => 1]);
        $this->getJson(route('api.providers.image', ['provider' => $provider->id, 'image' => $foreignItem->id, 'order_id' => $conversation->order_id]))->assertNotFound();
        $this->actingAs(User::factory()->create(), 'sanctum')->getJson($avatar)->assertNotFound();
        $this->getJson($portfolio)->assertNotFound();
    }

    #[Test]
    public function profile_does_not_return_an_avatar_url_when_no_photo_exists(): void
    {
        [$conversation, $customer, $provider] = $this->conversation();
        $this->actingAs($customer, 'sanctum')->getJson("/api/v1/providers/{$provider->id}?order_id={$conversation->order_id}")->assertOk()->assertJsonPath('data.avatar_url', null);
        $this->getJson(route('api.providers.image', ['provider' => $provider->id, 'image' => 'avatar', 'order_id' => $conversation->order_id]))->assertNotFound();
    }

    /** @return array{Conversation, User, ProviderProfile} */
    private function conversation(): array
    {
        $customer = User::factory()->create();
        $provider = ProviderProfile::factory()->create(['available_now' => true]);
        $order = Order::factory()->status(OrderStatus::Confirmed)->create(['customer_id' => $customer->id, 'provider_profile_id' => $provider->id]);
        $conversation = Conversation::query()->create(['order_id' => $order->id, 'customer_id' => $customer->id, 'provider_profile_id' => $provider->id, 'status' => 'OPEN']);

        return [$conversation, $customer, $provider];
    }
}
