<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Resources\Providers\Pages\ListProviderProfiles;
use App\Filament\Resources\Providers\Pages\ViewProviderProfile;
use App\Modules\Catalog\Models\Category;
use App\Modules\Geography\Models\Area;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Actions\EnableEmployeeOnboarding;
use App\Modules\Providers\Actions\ReviewProviderAction;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Symfony\Component\HttpKernel\Exception\HttpException;
use Tests\TestCase;

final class StaffTesterFeedbackTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
        $this->setOperatingFlags(false);
        Storage::fake('local');
    }

    #[Test]
    public function customer_cannot_enter_or_submit_technician_application_in_employee_mode(): void
    {
        $customer = User::factory()->create();
        $this->actingAs($customer, 'sanctum');
        $this->assertNotContains('open_provider_mode', $this->getJson('/api/v1/me')->assertOk()->json('user.available_actions'));
        $this->getJson('/api/v1/provider/application')->assertUnprocessable()->assertJsonPath('error.code', 'BUSINESS_RULE_VIOLATION');
        $this->postJson('/api/v1/provider/application', $this->payload($customer))->assertUnprocessable()->assertJsonPath('error.code', 'BUSINESS_RULE_VIOLATION');
        $this->assertDatabaseMissing('provider_profiles', ['user_id' => $customer->id]);
    }

    #[Test]
    public function admin_enables_existing_account_then_employee_submits_for_review_without_self_approval(): void
    {
        $employee = User::factory()->create();
        $this->actingAs(Admin::factory()->create(), 'admin');
        Livewire::test(ListProviderProfiles::class)
            ->callAction('enableEmployeeOnboarding', ['user_id' => $employee->id])->assertHasNoActionErrors();
        $profile = $employee->providerProfile()->firstOrFail();
        $this->assertSame(EmploymentType::Employee, $profile->employment_type);
        $this->assertFalse($profile->available_now);
        $this->assertNull($profile->submitted_at);
        Livewire::test(ViewProviderProfile::class, ['record' => $profile->id])->assertActionDisabled('approve');

        $this->actingAs($employee, 'sanctum');
        $this->assertContains('open_provider_mode', $this->getJson('/api/v1/me')->assertOk()->json('user.available_actions'));
        $this->getJson('/api/v1/provider/application')->assertOk()->assertJsonPath('data.status', 'NOT_STARTED');
        $this->postJson('/api/v1/provider/application', $this->payload($employee))->assertOk()
            ->assertJsonPath('data.status', 'PENDING_REVIEW')->assertJsonPath('data.available_actions', []);
        $profile->refresh();
        $this->assertNotNull($profile->submitted_at);
        $this->assertSame(EmploymentType::Employee, $profile->employment_type);
        $this->assertFalse($profile->available_now);
        $this->postJson('/api/v1/provider/application', $this->payload($employee))->assertConflict();
        $admin = Admin::factory()->create();
        app(ReviewProviderAction::class)->verifyPhone($profile, $admin);
        app(ReviewProviderAction::class)->approve($profile, $admin, EmploymentType::Employee);
        $this->assertSame(ProviderStatus::Active, $profile->refresh()->status);
    }

    #[Test]
    public function existing_profile_is_not_overwritten_by_an_employee_invitation(): void
    {
        $profile = ProviderProfile::factory()->independent()->create();
        $this->expectException(BusinessRuleViolationException::class);
        try {
            app(EnableEmployeeOnboarding::class)->execute($profile->user, Admin::factory()->create());
        } finally {
            $this->assertSame(EmploymentType::Independent, $profile->refresh()->employment_type);
            $this->assertSame(ProviderStatus::Active, $profile->status);
        }
    }

    #[Test]
    public function inactive_admin_cannot_enable_employee_onboarding(): void
    {
        $employee = User::factory()->create();
        $this->expectException(HttpException::class);
        try {
            app(EnableEmployeeOnboarding::class)->execute($employee, Admin::factory()->create(['is_active' => false]));
        } finally {
            $this->assertDatabaseMissing('provider_profiles', ['user_id' => $employee->id]);
        }
    }

    #[Test]
    public function incomplete_invitation_cannot_be_approved_even_after_phone_verification(): void
    {
        $admin = Admin::factory()->create();
        $profile = app(EnableEmployeeOnboarding::class)->execute(User::factory()->create(), $admin);
        app(ReviewProviderAction::class)->verifyPhone($profile, $admin);
        $this->expectException(BusinessRuleViolationException::class);
        try {
            app(ReviewProviderAction::class)->approve($profile, $admin, EmploymentType::Employee);
        } finally {
            $this->assertSame(ProviderStatus::PendingReview, $profile->refresh()->status);
        }
    }

    #[Test]
    public function marketplace_keeps_public_application_entry_without_enabling_it_for_launch(): void
    {
        $this->setOperatingFlags(true);
        $this->actingAs(User::factory()->create(), 'sanctum');
        $this->assertContains('open_provider_mode', $this->getJson('/api/v1/me')->assertOk()->json('user.available_actions'));
        $this->getJson('/api/v1/provider/application')->assertOk()->assertJsonPath('data.status', 'NOT_STARTED');
    }

    #[Test]
    public function new_employee_order_notifies_active_admins_with_order_link(): void
    {
        $active = Admin::factory()->create();
        $inactive = Admin::factory()->create(['is_active' => false]);
        $order = Order::factory()->create(['status' => OrderStatus::Open]);
        OrderEvent::query()->create([
            'order_id' => $order->id, 'event_code' => OrderEventCode::Published,
            'actor_type' => ActorType::Customer, 'actor_id' => $order->customer_id,
            'from_status' => null, 'to_status' => OrderStatus::Open, 'created_at' => now(),
        ]);
        $notification = $active->notifications()->sole();
        $this->assertStringContainsString((string) $order->number, $notification->data['title']);
        $this->assertStringContainsString('/orders/'.$order->id, $notification->data['actions'][0]['url']);
        $this->assertSame(0, $inactive->notifications()->count());
        $this->assertSame(0, $order->customer->notifications()->count());
    }

    /** @return array<string, mixed> */
    private function payload(User $user): array
    {
        $category = Category::query()->where('is_active', true)->firstOrFail();
        $payload = [
            'experience_years' => 3, 'bio' => 'خبرة في صيانة المنازل',
            'category_ids' => [$category->id], 'specialty_ids' => [$category->problemTypes()->where('is_active', true)->firstOrFail()->id],
            'area_ids' => [Area::query()->where('is_active', true)->firstOrFail()->id],
            'payout_method' => 'INSTAPAY', 'payout_details' => 'employee@example.test',
        ];
        foreach (['profile_photo_media_id', 'id_front_media_id', 'id_back_media_id'] as $field) {
            $path = 'tests/'.$user->id.'/'.$field.'.jpg';
            Storage::disk('local')->put($path, 'image');
            $payload[$field] = OrderMedia::query()->create([
                'uploaded_by' => $user->id, 'type' => 'IMAGE', 'path' => $path, 'size_bytes' => 5, 'expires_at' => now()->addHour(),
            ])->id;
        }

        return $payload;
    }
}
