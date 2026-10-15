<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use App\Modules\Settlements\Models\ProviderRemittance;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * ردود `GET /provider/earnings` كما يقرؤها التطبيقان (SCR-P18، DEC-068).
 * ومع RECORD_API_RESPONSES=1 تُكتب إلى androidapp/app/src/test/resources/api/provider/ لاختبارات القراءة في أندرويد.
 */
final class MobileProviderEarningsResponsesTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function employee_earnings(): void
    {
        $profile = ProviderProfile::factory()->create();
        $this->closed($profile, OperatingMode::Employee, PaymentMethod::Cash, '650.00', '200.00', '650.00');
        ProviderRemittance::query()->create([
            'provider_profile_id' => $profile->getKey(), 'amount' => '300.00', 'method' => 'INSTAPAY',
            'reference' => 'REM-1', 'received_at' => now()->subHour(), 'admin_id' => Admin::factory()->create()->getKey(),
        ]);

        $response = $this->earnings($profile)
            ->assertJsonPath('data.operating_mode', 'EMPLOYEE')
            ->assertJsonPath('data.dues.blocked', false)
            ->assertJsonPath('data.dues.limit', null);

        $this->record('earnings_employee', $response);
    }

    #[Test]
    public function marketplace_earnings_blocked_by_dues(): void
    {
        $settings = app(SettingsRepository::class);
        $settings->set(Cfg::OffersEnabled, true);
        $settings->set(Cfg::ProviderDebtLimit, '500.00');
        $profile = ProviderProfile::factory()->independent()->create(['dues_blocked_at' => now()]);
        $this->closed($profile, OperatingMode::Marketplace, PaymentMethod::Cash, '6500.00', '0.00', '650.00');

        $response = $this->earnings($profile)
            ->assertJsonPath('data.operating_mode', 'MARKETPLACE')
            ->assertJsonPath('data.dues.blocked', true)
            ->assertJsonPath('data.dues.debt', '650.00')
            ->assertJsonPath('data.dues.limit', '500.00');

        $this->record('earnings_marketplace_blocked', $response);
    }

    private function closed(ProviderProfile $profile, OperatingMode $mode, PaymentMethod $method, string $labor, string $materials, string $commission): void
    {
        Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $profile->getKey(), 'operating_mode' => $mode, 'payment_method' => $method,
            'labor_total' => $labor, 'materials_total' => $materials, 'final_amount' => bcadd($labor, $materials, 2),
            'commission_amount' => $commission, 'closed_at' => now()->subDays(2), 'settlement_eligible_at' => now()->subDay(),
        ]);
    }

    private function earnings(ProviderProfile $profile): TestResponse
    {
        return $this->actingAs($profile->user, 'sanctum')
            ->withHeaders(['X-App-Mode' => 'PROVIDER'])
            ->getJson('/api/v1/provider/earnings')
            ->assertOk()
            ->assertJsonStructure(['data' => [
                'operating_mode', 'currency', 'summary' => ['balance', 'available', 'pending'],
                'dues' => ['blocked', 'debt', 'limit'],
                'transactions' => [['id', 'type', 'title', 'detail', 'amount', 'occurred_at', 'available']],
                'meta' => ['current_page', 'last_page'],
            ]]);
    }

    private function record(string $name, TestResponse $response): void
    {
        if (! env('RECORD_API_RESPONSES')) {
            return;
        }

        $body = $response->json();
        foreach ($body['data']['transactions'] as $index => $transaction) {
            $body['data']['transactions'][$index]['id'] = preg_replace('/\d+$/', (string) ($index + 1), $transaction['id']);
            $body['data']['transactions'][$index]['title'] = preg_replace('/#\d+/', '#1042', $transaction['title']);
            $body['data']['transactions'][$index]['occurred_at'] = '2026-10-01T10:00:00+00:00';
        }

        $directory = base_path('androidapp/app/src/test/resources/api/provider');
        if (! is_dir($directory)) {
            mkdir($directory, 0755, true);
        }
        file_put_contents(
            "{$directory}/{$name}.json",
            json_encode($body, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES).PHP_EOL,
        );
    }
}
