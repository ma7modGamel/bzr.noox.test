<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * الدفعة 9أ، المرحلة 2 (DEC-061): ردود رئيسية العميل والطلبات كما يراها تطبيق العميل، في كل حالة يمر بها الطلب.
 *
 * كل تشغيل يتحقق من الحقول التي يقرؤها التطبيقان. ومع RECORD_API_RESPONSES=1 تُكتب الردود
 * إلى androidapp/app/src/test/resources/api/customer/ لتقرأها اختبارات القراءة في أندرويد.
 */
final class MobileCustomerOrderResponsesTest extends TestCase
{
    use RefreshDatabase;

    /** Real ids in this run → stable ids in the recorded files. */
    private array $ids = [];

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

    #[Test]
    public function ردود_الرئيسية_والطلبات_تحمل_حقول_الموديل(): void
    {
        $customer = User::factory()->create(['name' => 'منى حسن', 'phone' => '01011111111']);
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id, 'label' => 'البيت', 'is_default' => true]);
        $category = Category::query()->where('name', 'سباكة')->sole();
        $category->problemTypes()->where('is_other', false)->firstOrFail()->update([
            'employee_price_min' => '300.00',
            'employee_price_max' => '500.00',
        ]);

        $this->record('config', $this->getJson('/api/v1/config')->assertOk()
            ->assertJsonStructure(['operating_mode', 'option_lists', 'option_defaults', 'service_hours' => ['from', 'to']]));
        $this->record('catalog', $this->getJson('/api/v1/catalog')->assertOk()
            ->assertJsonStructure(['data' => [['id', 'name', 'icon_path', 'problem_types' => [['id', 'name', 'is_other']]]]]));
        $this->record('catalog_city', $this->getJson('/api/v1/catalog?city_id='.$address->city_id)->assertOk());
        $this->record('addresses', $this->customerGet($customer, '/api/v1/addresses')
            ->assertJsonStructure(['data' => [['id', 'label', 'is_default', 'city' => ['id'], 'area' => ['name'], 'address_text']]]));

        $orderId = $this->publish($customer, $address, $category, 'NOW');
        $this->record('order_open', $this->order($customer, $orderId));
        $this->record('orders_current', $this->customerGet($customer, '/api/v1/orders?scope=current&page=1')
            ->assertJsonStructure(['data' => [['id', 'number', 'status', 'status_label', 'category', 'problem_type', 'location', 'created_at']]]));

        $order = Order::query()->findOrFail($orderId);
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $provider->user->update(['name' => 'كريم السباك', 'phone' => '01022222222']);
        // decimal:2 fields arrive as strings ("4.80"); record a real value so the model is tested on it.
        $provider->forceFill(['rating_avg' => '4.80'])->save();
        app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());
        $this->record('order_confirmed', $this->order($customer, $orderId));

        $this->providerPost($provider->user, "/api/v1/provider/orders/{$orderId}/start-trip", ['lat' => 30.0580, 'lng' => 31.3380])->assertOk();
        CarbonImmutable::setTestNow(now()->addSeconds(31));
        $this->providerPost($provider->user, "/api/v1/provider/orders/{$orderId}/location", ['lat' => 30.0590, 'lng' => 31.3390])->assertNoContent();
        $this->record('order_on_the_way', $this->order($customer, $orderId));
        $this->record('tracking_on_the_way', $this->customerGet($customer, "/api/v1/orders/{$orderId}/tracking")
            ->assertJsonStructure(['last_location' => ['lat', 'lng'], 'eta_minutes', 'eta_approximate']));

        $this->providerPost($provider->user, "/api/v1/provider/orders/{$orderId}/arrived", ['lat' => 30.0600, 'lng' => 31.3400])->assertOk();
        $quoteId = $this->providerPost($provider->user, "/api/v1/provider/orders/{$orderId}/proposals", [
            'type' => 'EXECUTION_QUOTE',
            'amount' => 450,
            'reason' => 'تغيير خرطوم وصيانة الخلاط',
        ])->assertCreated()->json('data.id');
        $this->customerPost($customer, "/api/v1/orders/{$orderId}/proposals/{$quoteId}/decide", ['approve' => true])->assertOk();
        $this->providerPost($provider->user, "/api/v1/provider/orders/{$orderId}/complete")->assertOk();
        $this->providerPost($provider->user, "/api/v1/provider/orders/{$orderId}/cash-received", ['amount' => 450])->assertOk();
        $this->record('order_awaiting_confirmation', $this->order($customer, $orderId));
        $this->customerPost($customer, "/api/v1/orders/{$orderId}/confirm-completion")->assertOk();
        $this->record('order_closed', $this->order($customer, $orderId));

        $cancelledId = $this->publish($customer, $address, $category, 'NOW');
        $this->customerPost($customer, "/api/v1/orders/{$cancelledId}/cancel", [
            'reason_code' => 'NO_LONGER_NEEDED',
            'expected_version' => Order::query()->findOrFail($cancelledId)->version,
        ])->assertOk();
        $this->record('order_cancelled', $this->order($customer, $cancelledId));
        $this->record('orders_past', $this->customerGet($customer, '/api/v1/orders?scope=past&page=1')->assertJsonCount(2, 'data'));
    }

    private function publish(User $customer, CustomerAddress $address, Category $category, string $timing): int
    {
        return $this->actingAs($customer, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'CUSTOMER', 'Idempotency-Key' => (string) Str::uuid()])
            ->postJson('/api/v1/orders', [
                'customer_address_id' => $address->id,
                'category_id' => $category->id,
                'problem_type_id' => $category->problemTypes()->where('is_other', false)->value('id'),
                'timing_type' => $timing,
                'materials_responsibility' => 'UNSURE',
                'terms_accepted' => true,
                'description' => 'تسريب أسفل الحوض منذ يومين.',
            ])
            ->assertCreated()
            ->json('data.id');
    }

    private function order(User $customer, int $orderId): TestResponse
    {
        return $this->customerGet($customer, "/api/v1/orders/{$orderId}")->assertJsonStructure(['data' => [
            'id', 'number', 'status', 'status_label', 'display_status', 'version', 'category' => ['id', 'name'],
            'problem_type' => ['id', 'name'], 'timing' => ['type'], 'location', 'deadlines', 'amounts' => ['payment_status'],
            'termination', 'available_actions', 'created_at',
        ]]);
    }

    private function customerGet(User $user, string $uri): TestResponse
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])->getJson($uri)->assertOk();
    }

    private function customerPost(User $user, string $uri, array $payload = []): TestResponse
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])->postJson($uri, $payload);
    }

    private function providerPost(User $user, string $uri, array $payload = []): TestResponse
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'PROVIDER'])->postJson($uri, $payload);
    }

    /** Writes the body with stable ids, so re-recording only shows real contract changes. */
    private function record(string $name, TestResponse $response): void
    {
        if (! env('RECORD_API_RESPONSES')) {
            return;
        }

        $directory = base_path('androidapp/app/src/test/resources/api/customer');
        if (! is_dir($directory)) {
            mkdir($directory, 0755, true);
        }
        file_put_contents(
            "{$directory}/{$name}.json",
            json_encode($this->stable($response->json()), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES).PHP_EOL,
        );
    }

    private function stable(mixed $value, ?string $key = null): mixed
    {
        if (is_array($value)) {
            $result = [];
            foreach ($value as $childKey => $child) {
                $result[$childKey] = $this->stable($child, is_string($childKey) ? $childKey : $key);
            }

            return $result;
        }
        if (is_int($value) && $key !== null && ($key === 'id' || str_ends_with($key, '_id'))) {
            return $this->ids["{$key}:{$value}"] ??= 1000 + count($this->ids);
        }

        return $value;
    }
}
