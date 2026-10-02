<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Catalog\Models\Category;
use App\Modules\Communication\Services\ConversationService;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Contracts\PushSender;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Enums\NotificationCode;
use App\Modules\Notifications\Jobs\FlushOfferNotifications;
use App\Modules\Notifications\Jobs\SendPushNotification;
use App\Modules\Notifications\Jobs\SendRatingReminders;
use App\Modules\Notifications\Jobs\SendTripStartReminders;
use App\Modules\Notifications\Models\DeviceToken;
use App\Modules\Notifications\Push\FcmPushSender;
use App\Modules\Notifications\Push\PushMessage;
use App\Modules\Notifications\Push\PushResult;
use App\Modules\Notifications\Services\Notifier;
use App\Modules\Notifications\Services\OrderNotifications;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Contracts\Queue\Job;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Notifications\DatabaseNotification;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Queue;
use Illuminate\Support\Str;
use Mockery;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** الدفعة 6 — 17، DEC-058، AC-NTF-01..08. */
final class NotificationDeliveryTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    /** AC-NTF-07 + 31 §`/me/devices`. */
    #[Test]
    public function الجهاز_يسجل_وينتقل_بين_الحسابات_ويُحذف_عند_الخروج(): void
    {
        $first = User::factory()->create();
        $second = User::factory()->create();
        $token = str_repeat('a', 40);

        $this->actingAs($first, 'sanctum')->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->postJson('/api/v1/me/devices', ['token' => $token, 'platform' => 'ANDROID'])
            ->assertNoContent();
        $this->assertDatabaseHas('device_tokens', ['token' => $token, 'user_id' => $first->id, 'app_mode' => 'PROVIDER']);

        $this->actingAs($second, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])
            ->postJson('/api/v1/me/devices', ['token' => $token, 'platform' => 'ANDROID'])
            ->assertNoContent();
        $this->assertDatabaseHas('device_tokens', ['token' => $token, 'user_id' => $second->id, 'app_mode' => 'CUSTOMER']);
        $this->assertDatabaseCount('device_tokens', 1);

        // حساب آخر لا يحذف رمزًا ليس له.
        $this->actingAs($first, 'sanctum')->deleteJson('/api/v1/me/devices', ['token' => $token])->assertNoContent();
        $this->assertDatabaseCount('device_tokens', 1);

        $this->actingAs($second, 'sanctum')->deleteJson('/api/v1/me/devices', ['token' => $token])->assertNoContent();
        $this->assertDatabaseCount('device_tokens', 0);

        $this->actingAs($second, 'sanctum')
            ->postJson('/api/v1/me/devices', ['token' => $token, 'platform' => 'WEB'])
            ->assertUnprocessable();
    }

    /** AC-NTF-01 — الداخلي أولًا ثم Push لكل الأجهزة، والرابط من الخادم. */
    #[Test]
    public function بدء_التحرك_يحفظ_ntf08_ويرسل_push_لكل_أجهزة_العميل(): void
    {
        Queue::fake([SendPushNotification::class]);
        [$order, $provider] = $this->assignedOrder();
        $devices = DeviceToken::query()->insert([
            ['user_id' => $order->customer_id, 'token' => str_repeat('c', 40), 'platform' => 'ANDROID', 'app_mode' => 'CUSTOMER'],
            ['user_id' => $order->customer_id, 'token' => str_repeat('d', 40), 'platform' => 'IOS', 'app_mode' => 'PROVIDER'],
        ]);
        $this->assertTrue($devices);

        CarbonImmutable::setTestNow(now()->addMinute());
        app(StartTripAction::class)->execute($order, $provider, 30.0500, 31.3300);

        $notification = $this->latestFor($order->customer, 'NTF-08');
        $this->assertSame('bremo://orders/'.$order->getKey(), $notification->data['deep_link']);
        $this->assertSame('CUSTOMER', $notification->app_mode);
        Queue::assertPushed(SendPushNotification::class, 2);
        Queue::assertPushed(SendPushNotification::class, fn (SendPushNotification $job): bool => $job->notificationId === $notification->getKey());

        $this->actingAs($order->customer, 'sanctum')->getJson('/api/v1/notifications')
            ->assertOk()
            ->assertJsonPath('data.0.code', 'NTF-08')
            ->assertJsonPath('data.0.deep_link', 'bremo://orders/'.$order->getKey());
    }

    /** AC-NTF-04 — وضع الموظفين: NTF-28/29 فقط، بلا إشعارات السوق. */
    #[Test]
    public function التعيين_في_وضع_الموظفين_يرسل_ntf28_و_ntf29_فقط(): void
    {
        [$order, $provider] = $this->assignedOrder();

        $providerNotification = $this->latestFor($provider->user, 'NTF-28');
        $this->assertSame('bremo://provider/orders/'.$order->getKey(), $providerNotification->data['deep_link']);
        $this->assertSame('PROVIDER', $providerNotification->app_mode);
        $this->latestFor($order->customer, 'NTF-29');

        $this->assertSame(0, DatabaseNotification::query()
            ->whereIn('type', ['NTF-01', 'NTF-04', 'NTF-05', 'NTF-06', 'NTF-21'])
            ->count());
    }

    /** C32/P08 — التصفية حسب `X-App-Mode`. */
    #[Test]
    public function قائمة_الإشعارات_وعدادها_حسب_الوضع(): void
    {
        [$order, $provider] = $this->assignedOrder();
        $user = $provider->user;
        app(Notifier::class)->send($user, new NotificationContent(
            NotificationCode::ProviderArrived, 'وصل الفني', 'نص', 'orders/'.$order->getKey(), 'CUSTOMER', $order->getKey(),
        ));

        $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->getJson('/api/v1/notifications')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.code', 'NTF-28')
            ->assertJsonPath('meta.unread_count', 1);

        $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])
            ->getJson('/api/v1/notifications')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.code', 'NTF-09');

        $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->getJson('/api/v1/provider/home')
            ->assertOk()
            ->assertJsonPath('data.unread_notifications', 1)
            ->assertJsonPath('data.available_actions.1', 'open_notifications');
    }

    /** AC-NTF-03 — نص الرسالة لا يصل أبدًا. */
    #[Test]
    public function رسالة_المحادثة_ترسل_ntf25_بلا_نصها(): void
    {
        [$order, $provider] = $this->assignedOrder();
        $conversation = app(ConversationService::class)->ensureAssignedConversation($order, $provider->getKey());

        app(ConversationService::class)->send($conversation, $order->customer, 'البوابة الخلفية رمزها 4455', []);

        $notification = $this->latestFor($provider->user, 'NTF-25');
        $this->assertSame('bremo://provider/orders/'.$order->getKey().'/chat', $notification->data['deep_link']);
        $this->assertStringNotContainsString('4455', json_encode($notification->data, JSON_UNESCAPED_UNICODE));
        $this->assertStringNotContainsString('البوابة', $notification->data['body']);
    }

    /** AC-NTF-02 (EC-22) — إعادة 3 مرات بفواصل 10/60/300 ثم فشل نهائي، والداخلي باقٍ. */
    #[Test]
    public function فشل_push_يعاد_ثلاث_مرات_ثم_يفشل_والرمز_غير_الصالح_يحذف(): void
    {
        $user = User::factory()->create();
        $device = DeviceToken::query()->create(['user_id' => $user->id, 'token' => str_repeat('e', 40), 'platform' => 'ANDROID', 'app_mode' => 'CUSTOMER']);
        Queue::fake([SendPushNotification::class]);
        $id = app(Notifier::class)->send($user, new NotificationContent(NotificationCode::ProviderArrived, 'وصل الفني', 'نص', 'orders/1', 'CUSTOMER', 1));

        $sender = Mockery::mock(PushSender::class);
        $sender->shouldReceive('send')->andReturn(PushResult::Retry);

        foreach ([1 => 10, 2 => 60, 3 => 300] as $attempt => $delay) {
            $job = new SendPushNotification($id, $device->id);
            $queueJob = Mockery::mock(Job::class);
            $queueJob->shouldReceive('attempts')->andReturn($attempt);
            $queueJob->shouldReceive('release')->once()->with($delay);
            $queueJob->shouldReceive('isReleased', 'isDeleted', 'hasFailed')->andReturn(false);
            $job->setJob($queueJob);
            $job->handle($sender);
        }

        $job = new SendPushNotification($id, $device->id);
        $queueJob = Mockery::mock(Job::class);
        $queueJob->shouldReceive('attempts')->andReturn(4);
        $queueJob->shouldReceive('fail')->once();
        $job->setJob($queueJob);
        $job->handle($sender);

        $this->assertNotNull(DatabaseNotification::query()->find($id));

        $invalid = Mockery::mock(PushSender::class);
        $invalid->shouldReceive('send')->once()->andReturn(PushResult::InvalidToken);
        (new SendPushNotification($id, $device->id))->handle($invalid);
        $this->assertDatabaseMissing('device_tokens', ['id' => $device->id]);
        $this->assertNotNull(DatabaseNotification::query()->find($id));
    }

    /** FCM HTTP v1: الحمولة وتصنيف الأخطاء (17 §حمولة Push). */
    #[Test]
    public function مرسل_fcm_يوقع_الطلب_ويصنف_الاستجابات(): void
    {
        $key = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA]);
        openssl_pkey_export($key, $pem);
        $path = tempnam(sys_get_temp_dir(), 'fcm');
        file_put_contents($path, json_encode(['client_email' => 'push@bremoapp-a8b4d.iam.gserviceaccount.com', 'private_key' => $pem, 'token_uri' => 'https://oauth2.googleapis.com/token']));
        config(['services.fcm.project_id' => 'bremoapp-a8b4d', 'services.fcm.credentials' => $path]);
        Cache::flush();

        Http::fake([
            'oauth2.googleapis.com/*' => Http::response(['access_token' => 'ya29.test', 'expires_in' => 3599]),
            'fcm.googleapis.com/*' => Http::sequence()
                ->push(['name' => 'projects/bremoapp-a8b4d/messages/1'])
                ->push(['error' => ['status' => 'NOT_FOUND', 'details' => [['errorCode' => 'UNREGISTERED']]]], 404)
                ->push(['error' => ['status' => 'UNAVAILABLE']], 503)
                ->push(['error' => ['status' => 'INVALID_ARGUMENT', 'message' => 'The registration token is not a valid FCM registration token']], 400)
                ->push(['error' => ['status' => 'PERMISSION_DENIED']], 403),
        ]);

        $message = new PushMessage('uuid-1', 'NTF-08', 'الفني في الطريق', 'نص', 'bremo://orders/42', 'CUSTOMER', 'orders', 'order-42');
        $sender = app(FcmPushSender::class);

        $this->assertSame(PushResult::Sent, $sender->send('token-1', $message));
        $this->assertSame(PushResult::InvalidToken, $sender->send('token-1', $message));
        $this->assertSame(PushResult::Retry, $sender->send('token-1', $message));
        $this->assertSame(PushResult::InvalidToken, $sender->send('token-1', $message));
        $this->assertSame(PushResult::Fatal, $sender->send('token-1', $message));

        Http::assertSent(function ($request): bool {
            if (! str_contains($request->url(), 'fcm.googleapis.com')) {
                return false;
            }

            return $request->url() === 'https://fcm.googleapis.com/v1/projects/bremoapp-a8b4d/messages:send'
                && $request->hasHeader('Authorization', 'Bearer ya29.test')
                && $request['message']['data'] === ['notification_id' => 'uuid-1', 'code' => 'NTF-08', 'deep_link' => 'bremo://orders/42', 'app_mode' => 'CUSTOMER']
                && $request['message']['android']['notification'] === ['channel_id' => 'orders', 'tag' => 'order-42']
                && $request['message']['apns']['payload']['aps']['thread-id'] === 'order-42';
        });
        Http::assertSentCount(6); // رمز OAuth مرة واحدة (مخزن) + 5 إرسالات
        unlink($path);
    }

    /** AC-NTF-05 — مرة واحدة، ولا شيء لمن أوقفه. */
    #[Test]
    public function تذكير_التقييم_مرة_واحدة_ويحترم_تفضيل_c33(): void
    {
        [$order, $provider] = $this->assignedOrder();
        $order->forceFill(['status' => OrderStatus::Closed, 'closed_at' => now()->subHours(25)])->save();

        $this->actingAs($provider->user, 'sanctum')
            ->patchJson('/api/v1/me', ['rating_reminders_enabled' => false])
            ->assertOk()
            ->assertJsonPath('user.rating_reminders_enabled', false);

        app(SendRatingReminders::class)->handle(app(OrderNotifications::class), app(SettingsRepository::class));
        app(SendRatingReminders::class)->handle(app(OrderNotifications::class), app(SettingsRepository::class));

        $this->assertSame(1, $order->customer->notifications()->where('type', 'NTF-18')->count());
        $this->assertSame('bremo://orders/'.$order->getKey().'/rating', $this->latestFor($order->customer, 'NTF-18')->data['deep_link']);
        $this->assertSame(0, $provider->user->notifications()->where('type', 'NTF-18')->count());
    }

    /** NTF-07 — CFG-031 بعد التأكيد، مرة واحدة لكل تعيين. */
    #[Test]
    public function تذكير_بدء_التحرك_مرة_واحدة(): void
    {
        [$order, $provider] = $this->assignedOrder();
        CarbonImmutable::setTestNow(now()->addMinutes(app(SettingsRepository::class)->int(Cfg::TripStartReminderMinutes) + 1));

        app(SendTripStartReminders::class)->handle(app(OrderNotifications::class), app(SettingsRepository::class));
        app(SendTripStartReminders::class)->handle(app(OrderNotifications::class), app(SettingsRepository::class));

        $this->assertSame(1, $provider->user->notifications()->where('type', 'NTF-07')->count());
        $this->assertSame('bremo://provider/orders/'.$order->getKey(), $this->latestFor($provider->user, 'NTF-07')->data['deep_link']);
    }

    /** AC-NTF-08 — NTF-04 مجمّع كل 5 دقائق. */
    #[Test]
    public function عروض_السوق_تجمع_في_اشعار_واحد_كل_خمس_دقائق(): void
    {
        $this->setOperatingFlags(offersEnabled: true, inspectionFeeEnabled: true);
        app(SettingsRepository::class)->set(Cfg::DefaultCommissionRate, '0.1000');
        Queue::fake([FlushOfferNotifications::class, SendPushNotification::class]);
        $order = Order::factory()->marketplace()->create();

        foreach (range(1, 3) as $index) {
            $provider = ProviderProfile::factory()->independent()->servingFor($order->category_id, $order->area_id)->create();
            CarbonImmutable::setTestNow(now()->addSeconds(40));
            $this->actingAs($provider->user, 'sanctum')
                ->withHeaders(['X-App-Mode' => 'PROVIDER', 'X-Expected-Version' => (string) $order->refresh()->version, 'Idempotency-Key' => (string) Str::uuid()])
                ->postJson("/api/v1/provider/requests/{$order->getKey()}/offers", [
                    'price' => 300 + $index, 'eta_minutes' => 30, 'inspection_fee_deductible' => true,
                    'includes_text' => 'المصنعية فقط',
                ])
                ->assertCreated();
        }

        $customer = $order->customer;
        $this->assertSame(1, $customer->notifications()->where('type', 'NTF-04')->count());
        $this->assertSame('bremo://orders/'.$order->getKey().'/offers', $this->latestFor($customer, 'NTF-04')->data['deep_link']);

        $flush = null;
        Queue::assertPushed(FlushOfferNotifications::class, function (FlushOfferNotifications $job) use (&$flush): bool {
            $flush = $job;

            return true;
        });
        CarbonImmutable::setTestNow(now()->addMinutes(5));
        $flush->handle(app(OrderNotifications::class));

        $this->assertSame(2, $customer->notifications()->where('type', 'NTF-04')->count());
        $this->assertStringContainsString('عرضان جديدان', $this->latestFor($customer, 'NTF-04')->data['body']);
    }

    /** DEC-058 — App Links وUniversal Links وصفحة بديلة بلا بيانات. */
    #[Test]
    public function ملفات_الروابط_العميقة_والصفحة_البديلة(): void
    {
        config([
            'services.app_links.android_sha256' => ['AB:CD'],
            'services.app_links.apple_team_id' => 'TEAM123456',
        ]);

        $this->getJson('/.well-known/assetlinks.json')
            ->assertOk()
            ->assertJsonPath('0.target.package_name', 'com.bremo.app')
            ->assertJsonPath('0.target.sha256_cert_fingerprints.0', 'AB:CD');
        $this->getJson('/.well-known/apple-app-site-association')
            ->assertOk()
            ->assertJsonPath('applinks.details.0.appIDs.0', 'TEAM123456.com.bremo.app')
            ->assertJsonPath('applinks.details.0.components.0./', '/app/*');
        $this->get('/app/orders/42')
            ->assertOk()
            ->assertHeader('X-Robots-Tag', 'noindex, nofollow')
            ->assertSee('افتح الرابط من تطبيق', false)
            ->assertDontSee('#42', false);
    }

    /** @return array{Order, ProviderProfile} */
    private function assignedOrder(): array
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();
        $order = app(PublishRequestAction::class)->execute($customer, new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $category->problemTypes()->where('is_other', false)->value('id'),
            timingType: TimingType::Now,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: true,
            description: 'تسريب أسفل الحوض.',
        ));
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        return [$order->refresh()->load('customer'), $provider->load('user')];
    }

    private function latestFor(User $user, string $code): DatabaseNotification
    {
        return $user->notifications()->where('type', $code)->latest()->firstOrFail();
    }
}
