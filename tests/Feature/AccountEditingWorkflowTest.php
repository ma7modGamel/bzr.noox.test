<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\OrderMedia;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Notification;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class AccountEditingWorkflowTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function changing_email_requires_the_current_password_and_reverification(): void
    {
        Notification::fake();
        $user = User::factory()->create(['password' => 'current-password', 'email_verified_at' => now()]);
        $this->actingAs($user, 'sanctum')->patchJson('/api/v1/me', ['email' => 'new@example.test'])
            ->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => ['current_password']]]);
        $this->patchJson('/api/v1/me', ['email' => 'new@example.test', 'current_password' => 'wrong-password'])
            ->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => ['current_password']]]);
        $this->assertNotNull($user->fresh()->email_verified_at);
        $this->patchJson('/api/v1/me', ['name' => 'Updated name', 'email' => 'new@example.test', 'phone' => '01012345678', 'current_password' => 'current-password'])
            ->assertOk()->assertJsonPath('user.email', 'new@example.test')->assertJsonPath('user.is_verified', false);
        $this->assertNull($user->fresh()->email_verified_at);
        Notification::assertSentTo($user, VerifyEmail::class);
    }

    #[Test]
    public function unchanged_email_keeps_verification_without_asking_for_a_password(): void
    {
        Notification::fake();
        $user = User::factory()->create(['email_verified_at' => now()]);
        $this->actingAs($user, 'sanctum')->patchJson('/api/v1/me', ['email' => $user->email, 'name' => 'New name'])
            ->assertOk()->assertJsonPath('user.is_verified', true)->assertJsonPath('user.name', 'New name');
        Notification::assertNothingSent();
    }

    #[Test]
    public function email_cannot_take_another_accounts_address(): void
    {
        $user = User::factory()->create(['password' => 'current-password']);
        $other = User::factory()->create();
        $this->actingAs($user, 'sanctum')->patchJson('/api/v1/me', ['email' => $other->email, 'current_password' => 'current-password'])
            ->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => ['email']]]);
        $this->assertSame($user->email, $user->fresh()->email);
    }

    #[Test]
    public function account_photo_upload_is_private_owned_and_replaces_the_old_photo(): void
    {
        Storage::fake('local');
        $old = UploadedFile::fake()->image('old.jpg')->store('avatars', 'local');
        $user = User::factory()->create(['avatar_path' => $old]);
        $this->actingAs($user, 'sanctum');
        $previousUrl = $this->getJson('/api/v1/me')->assertOk()->json('user.avatar_url');
        $id = $this->postJson('/api/v1/media', ['file' => UploadedFile::fake()->image('new.jpg')])->assertCreated()->json('media.id');
        $path = OrderMedia::findOrFail($id)->path;
        $response = $this->patchJson('/api/v1/me', ['avatar_media_id' => $id])->assertOk();
        $url = $response->json('user.avatar_url');
        $this->assertNotSame($previousUrl, $url);
        $this->assertSame($path, $user->fresh()->avatar_path);
        Storage::disk('local')->assertMissing($old);
        Storage::disk('local')->assertExists($path);
        $this->assertDatabaseMissing('order_media', ['id' => $id]);
        $this->get($url)->assertOk();
        $this->actingAs(User::factory()->create(), 'sanctum')->get($url)->assertNotFound();
    }

    #[Test]
    public function avatar_rejects_foreign_and_expired_uploads_without_changing_the_account(): void
    {
        Storage::fake('local');
        $user = User::factory()->create();
        $other = User::factory()->create();
        $this->actingAs($other, 'sanctum');
        $foreignId = $this->postJson('/api/v1/media', ['file' => UploadedFile::fake()->image('foreign.jpg')])->assertCreated()->json('media.id');
        $this->actingAs($user, 'sanctum')->patchJson('/api/v1/me', ['avatar_media_id' => $foreignId])
            ->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => ['avatar_media_id']]]);
        $expiredId = $this->postJson('/api/v1/media', ['file' => UploadedFile::fake()->image('expired.jpg')])->assertCreated()->json('media.id');
        OrderMedia::findOrFail($expiredId)->update(['expires_at' => now()->subMinute()]);
        $this->patchJson('/api/v1/me', ['avatar_media_id' => $expiredId])->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => ['avatar_media_id']]]);
        $this->assertNull($user->fresh()->avatar_path);
        $this->assertModelExists(OrderMedia::findOrFail($foreignId));
    }

    #[Test]
    public function reading_notifications_is_scoped_to_the_current_account_and_mode(): void
    {
        $user = User::factory()->create();
        $other = User::factory()->create();
        $customer = $user->notifications()->create(['id' => fake()->uuid(), 'type' => 'test', 'app_mode' => 'CUSTOMER', 'data' => ['title' => 'Customer']]);
        $provider = $user->notifications()->create(['id' => fake()->uuid(), 'type' => 'test', 'app_mode' => 'PROVIDER', 'data' => ['title' => 'Provider']]);
        $foreign = $other->notifications()->create(['id' => fake()->uuid(), 'type' => 'test', 'app_mode' => 'CUSTOMER', 'data' => ['title' => 'Other']]);
        $this->actingAs($user, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->postJson('/api/v1/notifications/read', ['notification_ids' => [$customer->id, $provider->id, $foreign->id]])->assertNoContent();
        $this->assertNotNull($customer->fresh()->read_at);
        $this->assertNull($provider->fresh()->read_at);
        $this->assertNull($foreign->fresh()->read_at);
        $this->getJson('/api/v1/notifications')->assertOk()->assertJsonPath('meta.unread_count', 0)->assertJsonPath('data.0.read_at', $customer->fresh()->read_at->toIso8601String());
    }
}
