<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class RepairPhotosTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        Storage::fake('local');
    }

    #[Test]
    public function completion_requires_both_repair_phases_and_accepts_uploaded_proof(): void
    {
        [$order, $provider] = $this->repairOrder();
        $this->complete($order, $provider)->assertUnprocessable();
        $this->attach($order, $provider, $this->image($provider->user), 'REPAIR_BEFORE')->assertOk();
        $this->complete($order, $provider)->assertUnprocessable();
        $this->attach($order, $provider, $this->image($provider->user), 'REPAIR_AFTER')->assertOk();
        $this->complete($order, $provider)->assertOk()->assertJsonPath('data.status', 'AWAITING_PAYMENT');
        $this->assertCount(2, $order->media()->get());
        $this->assertSame(0, $order->media()->whereNotNull('expires_at')->count());
    }

    #[Test]
    public function request_photos_do_not_count_as_repair_proof(): void
    {
        [$order, $provider] = $this->repairOrder();
        OrderMedia::findOrFail($this->image($order->customer))->update(['order_id' => $order->id, 'expires_at' => null]);
        $this->complete($order, $provider)->assertUnprocessable();
        $this->assertSame(OrderStatus::InProgress, $order->refresh()->status);
    }

    #[Test]
    public function images_cannot_be_stolen_reused_expired_or_added_after_completion(): void
    {
        [$order, $provider] = $this->repairOrder();
        $this->attach($order, $provider, $this->image(User::factory()->create()), 'REPAIR_BEFORE')->assertUnprocessable();
        $expiredId = $this->image($provider->user);
        OrderMedia::findOrFail($expiredId)->update(['expires_at' => now()->subMinute()]);
        $this->attach($order, $provider, $expiredId, 'REPAIR_BEFORE')->assertUnprocessable();
        $id = $this->image($provider->user);
        $this->attach($order, $provider, $id, 'REPAIR_BEFORE')->assertOk();
        $this->attach($order, $provider, $id, 'REPAIR_AFTER')->assertUnprocessable();
        $this->attach($order, $provider, $id, 'REPAIR_BEFORE')->assertOk();
        $other = ProviderProfile::factory()->create();
        $this->attach($order, $other, $this->image($other->user), 'REPAIR_BEFORE')->assertNotFound();
        $order->update(['status' => OrderStatus::AwaitingPayment]);
        $this->attach($order, $provider, $this->image($provider->user), 'REPAIR_AFTER')->assertUnprocessable();
    }

    #[Test]
    public function after_photo_is_only_available_during_repair_and_each_phase_has_a_limit(): void
    {
        [$order, $provider] = $this->repairOrder();
        $order->update(['status' => OrderStatus::Arrived]);
        $this->attach($order, $provider, $this->image($provider->user), 'REPAIR_AFTER')->assertUnprocessable();
        for ($i = 0; $i < 5; $i++) {
            $this->attach($order, $provider, $this->image($provider->user), 'REPAIR_BEFORE')->assertOk();
        }
        $this->attach($order, $provider, $this->image($provider->user), 'REPAIR_BEFORE')->assertUnprocessable();
    }

    #[Test]
    public function customer_can_review_with_optional_after_photos_and_without_photos(): void
    {
        [$order] = $this->repairOrder();
        $order->update(['status' => OrderStatus::Closed, 'closed_at' => now()]);
        $id = $this->image($order->customer);
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')->postJson("/api/v1/orders/{$order->id}/review", [
            'quality' => 5, 'punctuality' => 4, 'conduct' => 5, 'media_ids' => [$id],
        ])->assertCreated();
        $this->assertDatabaseHas('order_media', ['id' => $id, 'order_id' => $order->id, 'purpose' => 'CUSTOMER_REVIEW', 'expires_at' => null]);
        $this->assertCount(1, $order->review->photos);
        $this->getJson("/api/v1/media/{$id}")->assertOk();
        [$other] = $this->repairOrder();
        $other->update(['status' => OrderStatus::Closed, 'closed_at' => now()]);
        $this->actingAs($other->customer, 'sanctum')->postJson("/api/v1/orders/{$other->id}/review", [
            'quality' => 5, 'punctuality' => 5, 'conduct' => 5,
        ])->assertCreated();
    }

    #[Test]
    public function invalid_review_photo_rolls_back_rating(): void
    {
        [$order, $provider] = $this->repairOrder();
        $order->update(['status' => OrderStatus::Closed, 'closed_at' => now()]);
        $foreignId = $this->image($provider->user);
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')->postJson("/api/v1/orders/{$order->id}/review", [
            'quality' => 5, 'punctuality' => 5, 'conduct' => 5, 'media_ids' => [$foreignId],
        ])->assertUnprocessable();
        $this->assertDatabaseCount('reviews', 0);
        $this->assertSame(0, $provider->refresh()->ratings_count);
    }

    private function image(User $user): int
    {
        return (int) $this->actingAs($user, 'sanctum')->post('/api/v1/media', ['file' => UploadedFile::fake()->image('proof.jpg')], ['Accept' => 'application/json'])->assertCreated()->json('media.id');
    }

    private function attach(Order $order, ProviderProfile $provider, int $mediaId, string $purpose): TestResponse
    {
        return $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->postJson("/api/v1/provider/orders/{$order->id}/work-photos", [
            'media_id' => $mediaId, 'purpose' => $purpose, 'expected_version' => $order->refresh()->version,
        ]);
    }

    private function complete(Order $order, ProviderProfile $provider): TestResponse
    {
        return $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')->postJson("/api/v1/provider/orders/{$order->id}/complete", ['expected_version' => $order->refresh()->version]);
    }

    /** @return array{Order, ProviderProfile} */
    private function repairOrder(): array
    {
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::InProgress)->create(['provider_profile_id' => $provider->id]);
        PriceProposal::query()->create([
            'order_id' => $order->id, 'provider_profile_id' => $provider->id,
            'type' => ProposalType::ExecutionQuote, 'amount' => '500.00', 'reason' => 'إصلاح التسريب',
            'status' => ProposalStatus::Approved, 'expires_at' => now()->addHour(),
        ]);

        return [$order, $provider];
    }
}
