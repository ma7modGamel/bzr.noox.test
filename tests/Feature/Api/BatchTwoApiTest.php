<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Communication\Models\Conversation;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Jobs\PurgeExpiredMedia;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Orders\Models\ShareLink;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\SupportReasonSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class BatchTwoApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, TermsSeeder::class, SupportReasonSeeder::class]);
        CarbonImmutable::setTestNow('2026-10-05 09:00:00');
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function config_يعيد_عقد_التطبيقين_الكامل(): void
    {
        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonPath('tokens_version', '1')
            ->assertJsonPath('slots.duration_minutes', 120)
            ->assertJsonPath('limits.max_offers', 10)
            ->assertJsonPath('media_limits.audio_seconds', 120)
            ->assertJsonPath('media_limits.audio_mb', 5)
            ->assertJsonPath('media_limits.audio_channels', 1)
            ->assertJsonPath('media_limits.audio_bitrate_bps', 64000)
            ->assertJsonPath('minimum_supported_app_version.android', '1.0.0')
            ->assertJsonStructure([
                'service_slots',
                'media_limits',
                'option_lists' => [
                    'customer_cancellation_reasons' => [['code', 'label']],
                    'dispute_reasons' => [['code', 'label']],
                    'provider_report_reasons' => [['code', 'label']],
                ],
            ]);
    }

    #[Test]
    public function تمثيل_الطلب_يعيد_الإجراءات_والمهل_والحالة_والخطوات(): void
    {
        [$order, $customer, $provider] = $this->assignedOrder();

        $customerResponse = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}")
            ->assertOk()
            ->assertJsonPath('data.display_status', 'order.status.customer.CONFIRMED')
            ->assertJsonPath('data.stepper.0.state', 'done')
            ->assertJsonPath('data.stepper.1.state', 'active')
            ->assertJsonPath('data.deadlines.proposal_expires_at', null);

        $this->assertEqualsCanonicalizing(
            ['cancel', 'share_visit', 'call', 'chat'],
            $customerResponse->json('data.available_actions'),
        );

        $providerResponse = $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson("/api/v1/orders/{$order->id}")
            ->assertOk()
            ->assertJsonPath('data.display_status', 'order.status.provider.CONFIRMED');

        $this->assertEqualsCanonicalizing(
            ['start_trip', 'back_out', 'call', 'chat'],
            $providerResponse->json('data.available_actions'),
        );
    }

    #[Test]
    public function الشريط_null_في_الحالات_التي_تعرض_كارت_الحالة(): void
    {
        $customer = User::factory()->create();

        foreach ([OrderStatus::Open, OrderStatus::Cancelled, OrderStatus::Expired] as $status) {
            $order = Order::factory()->create(['customer_id' => $customer->id, 'status' => $status]);

            $this->actingAs($customer, 'sanctum')
                ->getJson("/api/v1/orders/{$order->id}")
                ->assertOk()
                ->assertJsonPath('data.stepper', null);
        }
    }

    #[Test]
    public function قائمة_المقترحات_تعيد_الإجمالي_المتوقع_للواجهة(): void
    {
        $customer = User::factory()->create();
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::InProgress)->create([
            'customer_id' => $customer->id,
            'provider_profile_id' => $provider->id,
            'labor_total' => '450.00',
            'final_amount' => '450.00',
        ]);
        $proposal = PriceProposal::query()->create([
            'order_id' => $order->id,
            'provider_profile_id' => $provider->id,
            'type' => ProposalType::Materials,
            'amount' => '200.00',
            'reason' => 'وصلة ومحبس جديدان',
            'status' => ProposalStatus::Pending,
            'expires_at' => now()->addHour(),
        ]);

        $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}/proposals")
            ->assertOk()
            ->assertJsonPath('data.0.id', $proposal->id)
            ->assertJsonPath('data.0.projected_total', '200.00');
    }

    #[Test]
    public function رفع_الصورة_مؤقت_وخاص_بصاحبه(): void
    {
        Storage::fake('local');
        $owner = User::factory()->create();

        $response = $this->actingAs($owner, 'sanctum')
            ->post('/api/v1/media', ['file' => UploadedFile::fake()->image('problem.jpg', 300, 200)], [
                'Accept' => 'application/json',
            ])
            ->assertCreated()
            ->assertJsonPath('media.type', 'IMAGE');

        $mediaId = $response->json('media.id');

        $this->actingAs(User::factory()->create(), 'sanctum')
            ->get("/api/v1/media/{$mediaId}", ['Accept' => 'application/json'])
            ->assertNotFound();
    }

    #[Test]
    public function المحادثة_قبل_الاختيار_تحجب_وسائل_التواصل(): void
    {
        $customer = User::factory()->create();
        $order = Order::factory()->marketplace()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()->create();
        Offer::query()->create([
            'order_id' => $order->id,
            'provider_profile_id' => $provider->id,
            'source' => 'PROVIDER',
            'price' => '250.00',
            'status' => 'SUBMITTED',
            'submitted_at' => now(),
        ]);

        $conversationId = $this->actingAs($customer, 'sanctum')
            ->postJson('/api/v1/conversations', [
                'order_id' => $order->id,
                'provider_profile_id' => $provider->id,
            ])
            ->assertCreated()
            ->json('data.id');

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/conversations/{$conversationId}/messages", [
                'body' => 'كلمني على 01012345678',
            ])
            ->assertCreated()
            ->assertJsonPath('data.body', 'كلمني على •••')
            ->assertJsonPath('data.was_masked', true);

        $this->actingAs(User::factory()->create(), 'sanctum')
            ->getJson("/api/v1/conversations/{$conversationId}/messages")
            ->assertNotFound();

        $this->assertNotNull(Conversation::query()->findOrFail($conversationId)->messages()->value('original_body_encrypted'));
    }

    #[Test]
    public function التتبع_للعميل_صاحب_الطلب_ويحترم_فاصل_التحديث(): void
    {
        [$order, $customer, $provider] = $this->assignedOrder();

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/start-trip", ['lat' => 30.05, 'lng' => 31.33])
            ->assertOk();

        CarbonImmutable::setTestNow(now()->addSeconds(31));

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/location", ['lat' => 30.06, 'lng' => 31.34])
            ->assertNoContent();

        $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}/tracking")
            ->assertOk()
            ->assertJsonPath('last_location.lat', '30.0600000');

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/location", ['lat' => 30.07, 'lng' => 31.35])
            ->assertUnprocessable()
            ->assertJsonPath('error.rule', 'BR-110');
    }

    #[Test]
    public function رابط_المشاركة_يظل_صالحا_حتى_الإلغاء_أو_نهاية_الطلب(): void
    {
        [$order, $customer] = $this->assignedOrder();

        $linkId = $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/share-links")
            ->assertCreated()
            ->assertJsonPath('data.expires_at', null)
            ->json('data.id');

        $this->assertSame(43, strlen(ShareLink::query()->findOrFail($linkId)->token));

        $this->actingAs($customer, 'sanctum')
            ->deleteJson("/api/v1/share-links/{$linkId}")
            ->assertNoContent();

        $this->assertNotNull(ShareLink::query()->findOrFail($linkId)->revoked_at);
    }

    #[Test]
    public function المحادثة_تبقى_للقراءة_بعد_الإغلاق_والهاتف_يختفي_بعد_المهلة(): void
    {
        [$order, $customer] = $this->assignedOrder();
        $order->forceFill([
            'status' => OrderStatus::Closed,
            'closed_at' => now()->subHours(25),
        ])->save();

        $response = $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}")
            ->assertOk()
            ->assertJsonPath('data.provider.phone', null);

        $this->assertContains('chat', $response->json('data.available_actions'));
        $this->assertNotContains('call', $response->json('data.available_actions'));
    }

    #[Test]
    public function التقييم_متاح_مرة_واحدة_داخل_النافذة_ويحدث_المتوسطات(): void
    {
        $customer = User::factory()->create();
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->create([
            'customer_id' => $customer->id,
            'provider_profile_id' => $provider->id,
            'status' => OrderStatus::Closed,
            'closed_at' => now()->subDay(),
        ]);

        $payload = ['quality' => 5, 'punctuality' => 4, 'conduct' => 3, 'comment' => 'خدمة جيدة'];

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/review", $payload)
            ->assertCreated()
            ->assertJsonPath('data.average', 4);

        $this->assertSame('4.00', $provider->refresh()->rating_avg);

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/review", $payload)
            ->assertUnprocessable()
            ->assertJsonPath('error.rule', 'BR-090');

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", ['stars' => 5])
            ->assertCreated();

        $this->assertSame('5.00', $customer->refresh()->customer_rating_avg);
    }

    #[Test]
    public function مهمة_التنظيف_تحذف_الرفع_المؤقت_المنتهي(): void
    {
        Storage::fake('local');
        Storage::disk('local')->put('order-media/expired.jpg', 'image');
        $media = OrderMedia::query()->create([
            'uploaded_by' => User::factory()->create()->id,
            'expires_at' => now()->subMinute(),
            'type' => 'IMAGE',
            'path' => 'order-media/expired.jpg',
            'size_bytes' => 5,
        ]);

        $this->assertSame(1, (new PurgeExpiredMedia)->handle());
        $this->assertDatabaseMissing('order_media', ['id' => $media->id]);
        Storage::disk('local')->assertMissing('order-media/expired.jpg');
    }

    /** @return array{Order, User, ProviderProfile} */
    private function assignedOrder(): array
    {
        $customer = User::factory()->create();
        $order = Order::factory()->create(['customer_id' => $customer->id]);
        $provider = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();

        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        return [$order, $customer, $provider];
    }
}
