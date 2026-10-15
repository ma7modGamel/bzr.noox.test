<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Catalog\Models\Category;
use App\Modules\Geography\Models\Area;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Actions\ReviewProviderAction;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\SupportReasonSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Facades\Storage;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\Support\AttachesRepairPhotos;
use Tests\Support\RecordsMobileResponses;
use Tests\TestCase;

/**
 * 9أ المرحلة 4 (DEC-061) — ردود مسار الفني (P01–P21، C19/C32 بوضع الفني) كما يقرؤها التطبيقان.
 * ومع RECORD_API_RESPONSES=1 تُكتب إلى androidapp/app/src/test/resources/api/provider/.
 */
final class MobileProviderResponsesTest extends TestCase
{
    use AttachesRepairPhotos;
    use RecordsMobileResponses;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class, SupportReasonSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
        Storage::fake('local');
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    #[Test]
    public function طلب_الانضمام(): void
    {
        $user = User::factory()->create(['name' => 'حسام الدين']);
        $this->record('application_not_started', $this->provider($user)->getJson('/api/v1/provider/application')->assertOk());

        $category = Category::query()->where('is_active', true)->orderBy('sort')->firstOrFail();
        $specialty = $category->problemTypes()->where('is_other', false)->firstOrFail();
        $area = Area::query()->where('is_active', true)->orderBy('sort')->firstOrFail();
        $submitted = $this->provider($user)->postJson('/api/v1/provider/application', [
            'experience_years' => 7, 'bio' => null,
            'category_ids' => [$category->id], 'specialty_ids' => [$specialty->id], 'area_ids' => [$area->id],
            'payout_method' => 'INSTAPAY', 'payout_details' => 'hossam@instapay',
            'profile_photo_media_id' => $this->image($user, 'profile.jpg'),
            'id_front_media_id' => $this->image($user, 'front.jpg'), 'id_back_media_id' => $this->image($user, 'back.jpg'),
        ])->assertCreated()->assertJsonPath('data.status', 'PENDING_REVIEW');
        $this->record('application_submitted', $submitted);

        $profile = ProviderProfile::query()->whereBelongsTo($user)->sole();
        $review = app(ReviewProviderAction::class);
        $review->reject($profile, Admin::factory()->create(), 'صورة البطاقة غير واضحة');
        $this->record('application_rejected', $this->provider($user)->getJson('/api/v1/provider/application')->assertOk()
            ->assertJsonPath('data.rejection_reason', 'صورة البطاقة غير واضحة'));
    }

    #[Test]
    public function عمل_الموظف(): void
    {
        $customer = User::factory()->create(['name' => 'منى حسن', 'phone' => '01011111111']);
        $order = Order::factory()->create(['customer_id' => $customer->id, 'description' => 'تسريب أسفل الحوض.']);
        $category = Category::query()->findOrFail($order->category_id);
        $category->problemTypes()->whereKey($order->problem_type_id)->update(['employee_price_min' => '300.00', 'employee_price_max' => '500.00']);
        $profile = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $profile->user->update(['name' => 'كريم السباك']);
        $tech = $profile->user;
        app(AssignProviderAction::class)->execute($order, $profile, Admin::factory()->create());

        $this->record('home_active', $this->provider($tech)->getJson('/api/v1/provider/home')->assertOk()
            ->assertJsonStructure(['data' => ['operating_mode', 'available_now', 'active_order' => ['id', 'number', 'display_status'], 'available_actions']]));
        $this->record('availability_off', $this->provider($tech)->putJson('/api/v1/provider/availability', ['available_now' => false])->assertOk());
        $this->record('orders_current', $this->provider($tech)->getJson('/api/v1/provider/orders?scope=current')->assertOk());

        $version = fn (): int => Order::query()->findOrFail($order->id)->version;
        $this->record('order_on_the_way', $this->provider($tech)->postJson("/api/v1/provider/orders/{$order->id}/start-trip", [
            'lat' => 30.058, 'lng' => 31.338, 'expected_version' => $version(),
        ])->assertOk());
        $this->provider($tech)->postJson("/api/v1/provider/orders/{$order->id}/arrived", ['lat' => 30.06, 'lng' => 31.34, 'expected_version' => $version(), 'arrival_code' => $this->arrivalCode($order->refresh())])->assertOk();
        $this->provider($tech)->postJson("/api/v1/provider/orders/{$order->id}/proposals", [
            'type' => 'EXECUTION_QUOTE', 'amount' => '450', 'reason' => 'تغيير خرطوم وصيانة الخلاط', 'expected_version' => $version(),
        ])->assertCreated();
        $proposalId = $order->proposals()->latest('id')->value('id');
        $this->actingAs($customer, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])
            ->postJson("/api/v1/orders/{$order->id}/proposals/{$proposalId}/decide", ['approve' => true])->assertOk();
        $this->record('order_in_progress', $this->provider($tech)->getJson('/api/v1/provider/orders?scope=current')->assertOk());
        $this->attachRepairPhotos($order, $profile);
        $this->record('order_completed', $this->provider($tech)->postJson("/api/v1/provider/orders/{$order->id}/complete", ['expected_version' => $version()])->assertOk());
        $this->record('order_cash_received', $this->provider($tech)->postJson("/api/v1/provider/orders/{$order->id}/cash-received", [
            'amount' => '450.00', 'expected_version' => $version(),
        ])->assertOk());

        $conversationId = $this->provider($tech)->getJson('/api/v1/conversations')->assertOk()->json('data.0.id');
        $this->actingAs($customer, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])
            ->postJson("/api/v1/conversations/{$conversationId}/messages", ['body' => 'شكرًا على السرعة.'])->assertCreated();
        $this->record('conversations', $this->provider($tech)->getJson('/api/v1/conversations')->assertOk());
        $this->record('conversation_messages', $this->provider($tech)->getJson("/api/v1/conversations/{$conversationId}/messages")->assertOk());
        $this->record('notifications', $this->provider($tech)->getJson('/api/v1/notifications')->assertOk());

        $this->actingAs($customer, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])
            ->postJson("/api/v1/orders/{$order->id}/confirm-completion")->assertOk();
        $this->record('orders_past', $this->provider($tech)->getJson('/api/v1/provider/orders?scope=past')->assertOk());
        $this->provider($tech)->postJson("/api/v1/provider/orders/{$order->id}/customer-rating", ['stars' => 5])->assertSuccessful();

        $this->record('profile', $this->provider($tech)->getJson('/api/v1/provider/profile')->assertOk()
            ->assertJsonStructure(['data' => ['name', 'rating_avg', 'completed_orders', 'experience_years', 'categories', 'specialty_ids', 'areas', 'area_ids', 'portfolio', 'available_actions']]));
        $this->record('profile_updated', $this->provider($tech)->patchJson('/api/v1/provider/profile', [
            'experience_years' => 12, 'bio' => 'سباك خبرة في التسريبات.',
            'specialty_ids' => [$order->problem_type_id], 'area_ids' => [$order->area_id],
        ])->assertOk());
        $this->record('portfolio_item', $this->provider($tech)->postJson('/api/v1/provider/portfolio', [
            'media_id' => $this->image($tech, 'work.jpg'), 'caption' => null,
        ])->assertCreated());
    }

    #[Test]
    public function السوق(): void
    {
        $this->setOperatingFlags(offersEnabled: true, inspectionFeeEnabled: true);
        app(SettingsRepository::class)->set(Cfg::DefaultCommissionRate, '0.1000');
        $order = Order::factory()->marketplace()->create();
        $profile = ProviderProfile::factory()->independent()->servingFor($order->category_id, $order->area_id)->create();
        $tech = $profile->user;

        $this->record('requests', $this->provider($tech)->getJson('/api/v1/provider/requests')->assertOk());
        $this->record('request_detail', $this->provider($tech)->getJson("/api/v1/provider/requests/{$order->id}")->assertOk()
            ->assertJsonStructure(['data' => ['order', 'media', 'own_offer', 'offer_context' => ['min_amount', 'commission_rate', 'eta_options', 'deductible_options', 'default_includes_text']]]));
        $offer = $this->provider($tech)->postJson("/api/v1/provider/requests/{$order->id}/offers", [
            'price' => '350', 'includes_text' => 'المصنعية فقط', 'note' => null, 'eta_minutes' => 30, 'inspection_fee_deductible' => true,
        ])->assertCreated();
        $this->record('offer_submitted', $offer);
        $this->record('offers', $this->provider($tech)->getJson('/api/v1/provider/offers')->assertOk());
        $this->record('offer_withdrawn', $this->provider($tech)->postJson('/api/v1/provider/offers/'.$offer->json('data.id').'/withdraw')->assertOk());
    }

    private function record(string $name, TestResponse $response): void
    {
        $this->recordResponse('provider', $name, $response);
    }

    private function provider(User $user): static
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'PROVIDER']);
    }

    private function image(User $user, string $path): int
    {
        Storage::disk('local')->put($path, 'image');

        return OrderMedia::query()->create([
            'uploaded_by' => $user->getKey(), 'type' => 'IMAGE', 'path' => $path, 'size_bytes' => 5, 'expires_at' => now()->addHour(),
        ])->getKey();
    }
}
