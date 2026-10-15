<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Jobs\SendPushNotification;
use App\Modules\Notifications\Push\FcmPushSender;
use App\Modules\Notifications\Push\PushMessage;
use App\Modules\Notifications\Push\PushResult;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settlements\Models\ProviderRemittance;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Queue;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Tests\TestCase;

final class EarningsStatementAndPushReadinessTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->freezeTime();
    }

    #[Test]
    public function filter_and_export_include_all_matching_owned_transactions_without_changing_account_balance(): void
    {
        $provider = ProviderProfile::factory()->create();
        Order::factory()->count(25)->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $provider->id, 'closed_at' => '2026-10-01 12:00:00',
            'payment_method' => PaymentMethod::Cash, 'labor_total' => 100, 'final_amount' => 100,
        ]);
        $outside = Order::factory()->status(OrderStatus::Closed)->create(['provider_profile_id' => $provider->id,
            'closed_at' => '2026-09-30 23:59:59', 'payment_method' => PaymentMethod::Cash, 'labor_total' => 100, 'final_amount' => 100]);
        $other = Order::factory()->status(OrderStatus::Closed)->create(['closed_at' => '2026-10-01 12:00:00']);
        ProviderRemittance::query()->create(['provider_profile_id' => $provider->id, 'amount' => 300, 'method' => 'CASH', 'reference' => 'REM-TEST',
            'received_at' => '2026-10-01 23:59:59', 'admin_id' => Admin::factory()->create()->id]);
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER');
        $query = '?type=ORDER&date_from=2026-10-01&date_to=2026-10-01';
        $response = $this->getJson('/api/v1/provider/earnings'.$query)->assertOk()->assertJsonCount(20, 'data.transactions')
            ->assertJsonPath('data.meta.total', 25)->assertJsonPath('data.meta.last_page', 2)
            ->assertJsonPath('data.summary.balance', '-2300.00')->assertJsonPath('data.filter_options.0.code', 'ALL')
            ->assertJsonPath('data.filters.date_from', '2026-10-01');
        $this->getJson('/api/v1/provider/earnings'.$query.'&page=2')->assertOk()->assertJsonCount(5, 'data.transactions');
        $download = $this->get('/api/v1/provider/earnings/export'.$query)->assertOk()->assertDownload();
        $csv = $download->streamedContent();
        $this->assertStringStartsWith("\xEF\xBB\xBF", $csv);
        $lines = array_values(array_filter(explode("\n", trim(substr($csv, 3)))));
        $this->assertCount(26, $lines);
        foreach (array_slice($lines, 1) as $line) {
            $row = str_getcsv($line, ',', '"', '');
            $this->assertSame('ORDER', $row[2]);
            $this->assertSame('-100.00', $row[5]);
            $this->assertNotContains($row[0], ['order-'.$outside->id, 'order-'.$other->id]);
        }
        $this->getJson('/api/v1/provider/earnings?type=REMITTANCE&date_from=2026-10-01&date_to=2026-10-01')
            ->assertOk()->assertJsonCount(1, 'data.transactions')->assertJsonPath('data.transactions.0.amount', '300.00');
        $this->getJson('/api/v1/provider/earnings?date_from=2030-01-01')->assertOk()->assertJsonCount(0, 'data.transactions');
    }

    #[Test]
    #[TestWith(['type=UNKNOWN', 'type'])]
    #[TestWith(['date_from=not-a-date', 'date_from'])]
    #[TestWith(['date_from=2026-10-10&date_to=2026-10-01', 'date_to'])]
    public function invalid_filters_are_rejected_for_both_the_list_and_export(string $query, string $field): void
    {
        $provider = ProviderProfile::factory()->create();
        $this->actingAs($provider->user, 'sanctum')->withHeader('X-App-Mode', 'PROVIDER');
        foreach (['/api/v1/provider/earnings?', '/api/v1/provider/earnings/export?'] as $path) {
            $this->getJson($path.$query)->assertUnprocessable()->assertJsonStructure(['error' => ['fields' => [$field]]]);
        }
    }

    #[Test]
    public function export_requires_authentication_and_a_provider_profile(): void
    {
        $this->getJson('/api/v1/provider/earnings/export')->assertUnauthorized();
        $this->actingAs(User::factory()->create(), 'sanctum')->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/earnings/export')->assertUnprocessable();
    }

    #[Test]
    public function readiness_reports_missing_configuration_without_exposing_keys_or_claiming_device_delivery(): void
    {
        config(['services.fcm.driver' => 'log', 'services.fcm.project_id' => '', 'services.fcm.credentials' => '']);
        Artisan::call('notifications:push-check', ['--json' => true, '--ios-config' => '/missing/file.plist']);
        $data = json_decode(Artisan::output(), true, flags: JSON_THROW_ON_ERROR);
        $this->assertFalse($data['configuration_ready']);
        $this->assertFalse($data['checks']['service_account']);
        $this->assertFalse($data['device_delivery_verified']);
        $this->assertArrayNotHasKey('private_key', $data);
    }

    #[Test]
    public function matching_configuration_passes_readiness_but_still_requires_device_delivery_verification(): void
    {
        $key = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA]);
        openssl_pkey_export($key, $pem);
        $paths = [tempnam(sys_get_temp_dir(), 'push-key-'), tempnam(sys_get_temp_dir(), 'push-android-'), tempnam(sys_get_temp_dir(), 'push-ios-')];
        try {
            file_put_contents($paths[0], json_encode(['type' => 'service_account', 'client_email' => 'test@example.test', 'project_id' => 'test-project', 'private_key' => $pem]));
            file_put_contents($paths[1], json_encode(['project_info' => ['project_id' => 'test-project'], 'client' => [['client_info' => ['android_client_info' => ['package_name' => 'com.bremo.app']]]]]));
            file_put_contents($paths[2], '<plist><dict><key>PROJECT_ID</key><string>test-project</string><key>BUNDLE_ID</key><string>com.bremo.app</string></dict></plist>');
            config(['services.fcm.driver' => 'fcm', 'services.fcm.project_id' => 'test-project', 'services.fcm.credentials' => $paths[0]]);
            $this->assertSame(0, Artisan::call('notifications:push-check', ['--json' => true, '--android-config' => $paths[1], '--ios-config' => $paths[2]]));
            $output = Artisan::output();
            $result = json_decode($output, true, flags: JSON_THROW_ON_ERROR);
            $this->assertTrue($result['configuration_ready']);
            $this->assertFalse($result['device_delivery_verified']);
            $this->assertStringNotContainsString($pem, $output);
        } finally {
            foreach ($paths as $path) {
                unlink($path);
            }
        }
    }

    #[Test]
    public function a_missing_fcm_project_does_not_invalidate_a_device_token(): void
    {
        $key = openssl_pkey_new(['private_key_bits' => 2048, 'private_key_type' => OPENSSL_KEYTYPE_RSA]);
        openssl_pkey_export($key, $pem);
        $path = tempnam(sys_get_temp_dir(), 'bremo-push-');
        file_put_contents($path, json_encode(['client_email' => 'test@example.test', 'private_key' => $pem]));
        try {
            config(['services.fcm.project_id' => 'missing-project', 'services.fcm.credentials' => $path]);
            Cache::flush();
            Http::preventStrayRequests();
            Http::fake(['oauth2.googleapis.com/token' => Http::response(['access_token' => 'test']),
                'fcm.googleapis.com/v1/projects/missing-project/messages:send' => Http::response(['error' => ['status' => 'NOT_FOUND']], 404)]);
            $message = new PushMessage('id', 'NTF-08', 'Title', 'Body', 'bremo://orders/7', 'CUSTOMER', 'orders', null);
            $this->assertSame(PushResult::Fatal, app(FcmPushSender::class)->send('device-token', $message));
            Http::assertSentCount(2);
        } finally {
            unlink($path);
        }
    }

    #[Test]
    public function push_test_routes_only_to_an_order_owned_by_the_selected_recipient(): void
    {
        Queue::fake([SendPushNotification::class]);
        $order = Order::factory()->create();
        $this->artisan('notifications:push-test', ['user' => $order->customer_id, '--order' => $order->id])->assertSuccessful();
        $this->assertDatabaseHas('notifications', ['notifiable_id' => $order->customer_id]);
        $notification = $order->customer->notifications()->sole();
        $this->assertSame('bremo://orders/'.$order->id, $notification->data['deep_link']);
        $other = User::factory()->create();
        $this->artisan('notifications:push-test', ['user' => $other->id, '--order' => $order->id])->assertFailed();
        $this->assertSame(0, $other->notifications()->count());
        $this->artisan('notifications:push-test', ['user' => $order->customer_id, '--mode' => 'PROVIDER', '--order' => $order->id])->assertFailed();
        $provider = ProviderProfile::factory()->create();
        $order->update(['provider_profile_id' => $provider->id, 'status' => OrderStatus::Confirmed]);
        $this->artisan('notifications:push-test', ['user' => $provider->user_id, '--mode' => 'PROVIDER', '--order' => $order->id])->assertSuccessful();
        $this->assertSame('bremo://provider/orders/'.$order->id, $provider->user->notifications()->sole()->data['deep_link']);
        $this->artisan('notifications:push-test', ['user' => $order->customer_id, '--mode' => 'UNKNOWN', '--order' => $order->id])->assertFailed();
        Queue::assertNothingPushed();
    }
}
