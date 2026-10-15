<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Str;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Symfony\Component\Process\Process;
use Tests\TestCase;

final class VideoDurationLimitTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    #[TestWith(['30', true])]
    #[TestWith(['30.1', false])]
    #[TestWith(['60', false])]
    public function actual_video_duration_is_limited_to_thirty_seconds(string $duration, bool $accepted): void
    {
        Storage::fake('local');
        $path = sys_get_temp_dir().'/bremo-'.Str::uuid().'.mp4';
        (new Process(['ffmpeg', '-v', 'error', '-y', '-f', 'lavfi', '-i', 'color=c=black:s=32x32:r=10', '-t', $duration, '-an', '-c:v', 'libx264', '-pix_fmt', 'yuv420p', $path]))->mustRun();
        try {
            $response = $this->actingAs(User::factory()->create(), 'sanctum')
                ->post('/api/v1/media', ['file' => new UploadedFile($path, 'problem.mp4', 'video/mp4', null, true), 'duration_sec' => 1], ['Accept' => 'application/json']);
            if ($accepted) {
                $response->assertCreated()->assertJsonPath('media.type', 'VIDEO')->assertJsonPath('media.duration_sec', 30);
                $this->assertDatabaseCount('order_media', 1);
                Storage::disk('local')->assertExists(OrderMedia::query()->sole()->path);
            } else {
                $response->assertUnprocessable()->assertJsonPath('error.rule', 'BR-014')
                    ->assertJsonPath('error.message', 'مدة الفيديو يجب ألا تتجاوز 30 ثانية. اختر فيديو أقصر.');
                $this->assertDatabaseCount('order_media', 0);
                $this->assertSame([], Storage::disk('local')->allFiles());
            }
        } finally {
            unlink($path);
        }
    }

    #[Test]
    public function config_advertises_the_same_video_limit_and_preserves_images_and_audio(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->getJson('/api/v1/config')->assertOk()->assertJsonPath('media_limits.video_seconds', 30)
            ->assertJsonPath('media_limits.images', 5)->assertJsonPath('media_limits.audio_seconds', 120);
    }

    #[Test]
    #[TestWith([30, true])]
    #[TestWith([31, false])]
    public function publishing_revalidates_the_duration_of_previously_uploaded_temporary_video(int $duration, bool $accepted): void
    {
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        $this->travelTo(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();
        ProviderProfile::factory()->servingFor($category->id, $address->area_id)->create();
        $video = $this->video($customer, $duration);
        $response = $this->actingAs($customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')->postJson('/api/v1/orders', [
            'customer_address_id' => $address->id, 'category_id' => $category->id,
            'problem_type_id' => $category->problemTypes()->where('is_other', false)->value('id'),
            'timing_type' => 'NOW', 'materials_responsibility' => 'UNSURE', 'description' => 'تسريب أسفل الحوض منذ يومين.',
            'terms_accepted' => true, 'media_ids' => [$video->id],
        ]);
        if ($accepted) {
            $response->assertCreated();
            $this->assertNotNull($video->refresh()->order_id);
        } else {
            $response->assertUnprocessable()->assertJsonPath('error.rule', 'BR-014');
            $this->assertDatabaseCount('orders', 0);
            $this->assertNull($video->refresh()->order_id);
        }
    }

    #[Test]
    public function updating_a_request_cannot_attach_a_temporary_video_over_the_new_limit(): void
    {
        $this->seed(SettingsSeeder::class);
        $this->travelTo(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
        $order = Order::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $order->customer_id]);
        $order->category->cities()->syncWithoutDetaching([$address->city_id => ['is_active' => true]]);
        ProviderProfile::factory()->servingFor($order->category_id, $address->area_id)->create();
        $video = $this->video($order->customer, 31);
        $this->actingAs($order->customer, 'sanctum')->withHeader('X-App-Mode', 'CUSTOMER')
            ->patchJson("/api/v1/orders/{$order->id}", ['customer_address_id' => $address->id, 'category_id' => $order->category_id, 'problem_type_id' => $order->problem_type_id, 'timing_type' => 'NOW', 'materials_responsibility' => 'UNSURE', 'description' => 'تسريب أسفل الحوض منذ يومين.', 'media_ids' => [$video->id], 'expected_version' => $order->version])
            ->assertUnprocessable()->assertJsonPath('error.rule', 'BR-014');
        $this->assertNull($video->refresh()->order_id);
    }

    private function video(User $customer, int $duration): OrderMedia
    {
        return OrderMedia::query()->create(['uploaded_by' => $customer->id, 'type' => 'VIDEO', 'duration_sec' => $duration,
            'path' => 'legacy-video.mp4', 'size_bytes' => 1000, 'expires_at' => now()->addHour()]);
    }
}
