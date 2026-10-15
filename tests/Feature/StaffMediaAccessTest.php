<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Communication\Models\Conversation;
use App\Modules\Communication\Models\Message;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\Review;
use App\Modules\Reviews\Models\ReviewTag;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\LazilyRefreshDatabase;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class StaffMediaAccessTest extends TestCase
{
    use LazilyRefreshDatabase;

    #[Test]
    public function message_image_is_private_to_its_conversation_and_survives_provider_reassignment(): void
    {
        $this->seed(SettingsSeeder::class);
        Storage::fake('local');
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::Arrived)->create(['provider_profile_id' => $provider->id]);
        $chat = Conversation::query()->create(['order_id' => $order->id, 'customer_id' => $order->customer_id, 'provider_profile_id' => $provider->id, 'status' => 'OPEN']);
        $message = Message::query()->create(['conversation_id' => $chat->id, 'sender_user_id' => $order->customer_id, 'body' => 'العطل الحالي', 'was_masked' => false]);
        $media = $this->photo($order, 'DESCRIPTION', $order->customer_id);
        $message->media()->attach($media->id);
        $url = "/api/v1/conversations/{$chat->id}/messages/{$message->id}/images/{$media->id}";
        $this->get($url)->assertUnauthorized();
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/conversations/{$chat->id}/messages")->assertOk()
            ->assertJsonPath('data.0.media.0.content_url', url($url));
        $this->assertSame('private-image', $this->get($url)->assertOk()->streamedContent());
        $newProvider = ProviderProfile::factory()->create();
        $order->update(['provider_profile_id' => $newProvider->id]);
        $this->actingAs($newProvider->user, 'sanctum')->get($url)->assertNotFound();
        $this->get("/api/v1/media/{$media->id}")->assertNotFound();
        $this->actingAs($provider->user, 'sanctum')->get($url)->assertOk();
        $this->get("/api/v1/media/{$media->id}")->assertOk();
        $other = $this->photo($order, 'DESCRIPTION', $order->customer_id);
        $this->get("/api/v1/conversations/{$chat->id}/messages/{$message->id}/images/{$other->id}")->assertNotFound();
    }

    #[Test]
    public function repair_evidence_is_only_exposed_to_customer_after_completion_and_never_to_outsiders(): void
    {
        $this->seed(SettingsSeeder::class);
        Storage::fake('local');
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::InProgress)->create(['provider_profile_id' => $provider->id]);
        $photo = $this->photo($order, 'REPAIR_BEFORE', $provider->user_id);
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonCount(0, 'data.work_photos');
        $this->get("/api/v1/media/{$photo->id}")->assertNotFound();
        $order->update(['completed_at' => now(), 'status' => OrderStatus::AwaitingPayment]);
        $this->getJson("/api/v1/orders/{$order->id}")->assertOk()->assertJsonPath('data.work_photos.0.content_url', url("/api/v1/media/{$photo->id}"));
        $this->get("/api/v1/media/{$photo->id}")->assertOk();
        $this->actingAs(User::factory()->create(), 'sanctum')->get("/api/v1/media/{$photo->id}")->assertNotFound();
    }

    #[Test]
    public function optional_current_fault_photo_is_readable_only_inside_its_order(): void
    {
        $this->seed(SettingsSeeder::class);
        Storage::fake('local');
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::AwaitingQuoteApproval)->create(['provider_profile_id' => $provider->id]);
        $photo = $this->photo($order, 'DESCRIPTION', $provider->user_id);
        $proposal = PriceProposal::query()->create(['order_id' => $order->id, 'provider_profile_id' => $provider->id, 'type' => ProposalType::ExecutionQuote,
            'amount' => '300.00', 'reason' => 'سبب التسريب والعمل المطلوب', 'photo_path' => $photo->path, 'status' => ProposalStatus::Pending, 'expires_at' => now()->addHour()]);
        $url = "/api/v1/orders/{$order->id}/proposals/{$proposal->id}/image";
        $this->actingAs($order->customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}/proposals")->assertOk()->assertJsonPath('data.0.photo_url', url($url));
        $this->get($url)->assertOk();
        $this->actingAs($provider->user, 'sanctum')->get($url)->assertOk();
        $this->actingAs(User::factory()->create(), 'sanctum')->get($url)->assertNotFound();
        $this->assertNull($order->refresh()->completed_at);
    }

    #[Test]
    public function review_photos_and_reviewer_area_have_profile_context_and_hidden_reviews_are_unreadable(): void
    {
        $this->seed(SettingsSeeder::class);
        Storage::fake('local');
        $provider = ProviderProfile::factory()->create();
        $reviewOrder = Order::factory()->status(OrderStatus::Closed)->create(['provider_profile_id' => $provider->id]);
        $review = Review::query()->create(['order_id' => $reviewOrder->id, 'provider_profile_id' => $provider->id, 'customer_id' => $reviewOrder->customer_id,
            'quality' => 5, 'punctuality' => 5, 'conduct' => 5, 'comment' => 'شغل جيد', 'tags' => ['QUALITY']]);
        $photo = $this->photo($reviewOrder, 'CUSTOMER_REVIEW', $reviewOrder->customer_id);
        ReviewTag::factory()->create(['code' => 'QUALITY', 'label' => 'شغل متقن']);
        $context = Order::factory()->status(OrderStatus::Arrived)->create(['provider_profile_id' => $provider->id]);
        $url = "/api/v1/providers/{$provider->id}/reviews/{$review->id}/photos/{$photo->id}?order_id={$context->id}";
        $this->actingAs($context->customer, 'sanctum')->getJson("/api/v1/providers/{$provider->id}?order_id={$context->id}")->assertOk()
            ->assertJsonPath('data.tag_statistics.0.count', 1)->assertJsonPath('data.tag_statistics.0.code', 'QUALITY')->assertJsonPath('data.reviews.0.photos.0.id', $photo->id)->assertJsonPath('data.reviews.0.area', $reviewOrder->area->name);
        $this->get($url)->assertOk();
        $review->update(['hidden_at' => now()]);
        $this->getJson("/api/v1/providers/{$provider->id}?order_id={$context->id}")->assertOk()->assertJsonCount(0, 'data.tag_statistics');
        $this->get($url)->assertNotFound();
        $this->actingAs(User::factory()->create(), 'sanctum')->get($url)->assertNotFound();
    }

    private function photo(Order $order, string $purpose, int $owner): OrderMedia
    {
        $path = 'test/'.fake()->uuid().'.jpg';
        Storage::disk('local')->put($path, 'private-image');

        return OrderMedia::query()->create(['order_id' => $order->id, 'uploaded_by' => $owner, 'path' => $path, 'type' => 'IMAGE', 'purpose' => $purpose, 'size_bytes' => 13]);
    }
}
