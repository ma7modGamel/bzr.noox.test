<?php

declare(strict_types=1);

namespace Tests\Feature\Seeders;

use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\DemoSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class DemoSeederTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function يملأ_الأسعار_والموظفين_والعملاء_ويتكرر_بأمان(): void
    {
        Storage::fake('local');
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
        $this->app->detectEnvironment(fn (): string => 'production');
        app(DemoSeeder::class)->run();
        app(DemoSeeder::class)->run();

        $regular = ProblemType::query()->where('is_other', false);
        $this->assertSame((clone $regular)->count(), (clone $regular)->whereNotNull('employee_price_min')->count());
        $this->assertSame(0, (clone $regular)->whereColumn('employee_price_min', '>', 'employee_price_max')->count());
        $this->assertSame(0, ProblemType::query()->where('is_other', true)->whereNotNull('employee_price_min')->count());

        $active = ProviderProfile::query()->where('status', ProviderStatus::Active);
        $this->assertSame(10, (clone $active)->where('employment_type', EmploymentType::Employee)->count());
        $this->assertSame(3, (clone $active)->where('employment_type', EmploymentType::Independent)->count());
        $this->assertSame(2, ProviderProfile::query()->where('status', ProviderStatus::PendingReview)->count());
        $this->assertSame(1, ProviderProfile::query()->where('status', ProviderStatus::Suspended)->count());
        $this->assertSame(0, (clone $active)->doesntHave('categories')->count());
        $this->assertSame(0, (clone $active)->doesntHave('areas')->count());

        $this->assertSame(4, Order::query()->where('status', OrderStatus::Open)->count());
        foreach (Order::query()->get() as $order) {
            $this->assertGreaterThan(0, app(ProviderEligibility::class)->query($order)->count(), "لا يوجد فني مؤهل للطلب #{$order->number}");
        }

        $this->assertSame(8, User::query()->has('addresses')->count());

        $this->postJson('/api/v1/auth/login', ['email' => 'customer1@bremo.test', 'password' => DemoSeeder::PASSWORD])->assertOk();
        $this->postJson('/api/v1/auth/login', ['email' => 'employee1@bremo.test', 'password' => DemoSeeder::PASSWORD])->assertOk();
        $this->postJson('/api/v1/auth/login', ['email' => 'push-test@bremo.test', 'password' => 'PushTest-2026'])->assertOk();
        $this->postJson('/api/v1/auth/login', ['email' => 'blocked1@bremo.test', 'password' => DemoSeeder::PASSWORD])
            ->assertJsonPath('error.code', 'ACCOUNT_BLOCKED');
    }
}
