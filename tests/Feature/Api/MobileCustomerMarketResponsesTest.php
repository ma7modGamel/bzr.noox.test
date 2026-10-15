<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Catalog\Models\Category;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\Review;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use PHPUnit\Framework\Attributes\Test;
use Tests\Support\RecordsMobileResponses;
use Tests\TestCase;

/**
 * 9أ المرحلة 3 (DEC-061) — ردود العروض وملف الفني (C06–C08) كما يقرؤها التطبيقان.
 * ومع RECORD_API_RESPONSES=1 تُكتب إلى androidapp/app/src/test/resources/api/customer/.
 */
final class MobileCustomerMarketResponsesTest extends TestCase
{
    use RecordsMobileResponses;
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
        $this->setOperatingFlags(offersEnabled: true);
    }

    #[Test]
    public function ردود_العروض_وملف_الفني(): void
    {
        $customer = User::factory()->create();
        $category = Category::query()->where('name', 'سباكة')->sole();
        $problem = $category->problemTypes()->where('is_other', false)->firstOrFail();
        $order = Order::factory()->marketplace()->create([
            'customer_id' => $customer->id, 'category_id' => $category->id, 'problem_type_id' => $problem->id,
        ]);

        $first = $this->provider($order, 'كريم السباك', '4.80', 12);
        $first->specialties()->sync([$problem->id]);
        $second = $this->provider($order, 'سامح عادل', '4.20', 3);
        $this->offer($order, $first, '450.00', eta: 40, includes: 'تغيير الخرطوم وتركيب الخلاط');
        $this->offer($order, $second, '380.00', eta: null, includes: null, note: 'يمكن الحضور بعد ساعة');

        $past = Order::factory()->status(OrderStatus::Closed)->create(['provider_profile_id' => $first->id]);
        Review::query()->create([
            'order_id' => $past->id, 'provider_profile_id' => $first->id, 'customer_id' => $past->customer_id,
            'quality' => 5, 'punctuality' => 4, 'conduct' => 5, 'comment' => 'شغل نظيف ومواعيد مظبوطة',
        ]);

        $this->recordResponse('customer', 'offers', $this->customerGet($customer, "/api/v1/orders/{$order->id}/offers?sort=rating")
            ->assertJsonCount(2, 'data')
            ->assertJsonStructure(['data' => [['id', 'price', 'eta_minutes', 'inspection_fee_deductible', 'includes_text', 'note',
                'provider' => ['id', 'name', 'is_verified', 'rating_avg', 'completed_orders']]]]));

        $this->recordResponse('customer', 'provider_context', $this->customerGet($customer, "/api/v1/providers/{$first->id}?order_id={$order->id}")
            ->assertJsonStructure(['data' => ['id', 'name', 'bio', 'is_verified', 'available_now', 'experience_years', 'rating_avg',
                'completed_orders', 'rating_breakdown' => ['quality', 'punctuality', 'conduct'], 'specialties', 'reviews', 'available_actions']]));
    }

    private function provider(Order $order, string $name, string $rating, int $completed): ProviderProfile
    {
        $profile = ProviderProfile::factory()->independent()->servingFor($order->category_id, $order->area_id)->create([
            'bio' => 'فني متخصص في أعمال السباكة.', 'experience_years' => 9,
            'rating_avg' => $rating, 'rating_quality_avg' => '4.90', 'rating_punctuality_avg' => '4.50', 'rating_conduct_avg' => '5.00',
            'ratings_count' => 7, 'completed_orders_count' => $completed,
        ]);
        $profile->user->update(['name' => $name]);

        return $profile;
    }

    private function offer(Order $order, ProviderProfile $profile, string $price, ?int $eta, ?string $includes, ?string $note = null): void
    {
        Offer::query()->create([
            'order_id' => $order->id, 'provider_profile_id' => $profile->id, 'source' => 'PROVIDER', 'price' => $price,
            'eta_minutes' => $eta, 'inspection_fee_deductible' => false, 'includes_text' => $includes, 'note' => $note,
            'status' => 'SUBMITTED', 'submitted_at' => now(),
        ]);
    }

    private function customerGet(User $user, string $uri): TestResponse
    {
        return $this->actingAs($user, 'sanctum')->withHeaders(['X-App-Mode' => 'CUSTOMER'])->getJson($uri)->assertOk();
    }
}
