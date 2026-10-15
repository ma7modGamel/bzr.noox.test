<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Filament\Resources\ReviewTags\Pages\CreateReviewTag;
use App\Filament\Resources\ReviewTags\Pages\EditReviewTag;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Reviews\Models\Review;
use App\Modules\Reviews\Models\ReviewTag;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Tests\TestCase;

final class ReviewTagManagementTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function admin_can_create_translate_reorder_and_disable_a_tag_without_changing_its_code(): void
    {
        $this->actingAs(Admin::factory()->super()->create(), 'admin');
        Livewire::test(CreateReviewTag::class)->fillForm(['code' => 'CAREFUL', 'label' => 'حريص على المكان', 'label_en' => 'Careful', 'sort' => 2, 'is_active' => true])->call('create')->assertHasNoFormErrors();
        $tag = ReviewTag::query()->sole();
        $this->getJson('/api/v1/config')->assertOk()->assertJsonPath('option_lists.service_review_tags.0.code', 'CAREFUL')->assertJsonPath('option_lists.service_review_tags.0.label', 'حريص على المكان');
        $this->withHeader('Accept-Language', 'en')->getJson('/api/v1/config')->assertOk()->assertJsonPath('option_lists.service_review_tags.0.label', 'Careful');
        Livewire::test(EditReviewTag::class, ['record' => $tag->getRouteKey()])->fillForm(['code' => 'CHANGED', 'label' => 'حريص', 'sort' => 3, 'is_active' => false])->call('save')->assertHasNoFormErrors();
        $this->assertSame('CAREFUL', $tag->refresh()->code);
        $this->getJson('/api/v1/config')->assertOk()->assertJsonCount(0, 'option_lists.service_review_tags');
    }

    #[Test]
    public function customer_review_keeps_codes_separate_from_the_exact_comment(): void
    {
        $tag = ReviewTag::factory()->create(['code' => 'QUALITY']);
        $order = $this->closedOrder();
        $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/review", ['quality' => 5, 'punctuality' => 4, 'conduct' => 5, 'comment' => 'شكرا على الإصلاح', 'tags' => [$tag->code]])->assertCreated();
        $review = Review::query()->sole();
        $this->assertSame(['QUALITY'], $review->tags);
        $this->assertSame('شكرا على الإصلاح', $review->comment);
    }

    #[Test]
    #[TestWith([['UNKNOWN']])]
    #[TestWith([['QUALITY', 'QUALITY']])]
    #[TestWith([['HIDDEN']])]
    public function invalid_duplicate_or_disabled_tags_do_not_create_a_partial_review(array $tags): void
    {
        ReviewTag::factory()->create(['code' => 'QUALITY']);
        ReviewTag::factory()->create(['code' => 'HIDDEN', 'is_active' => false]);
        $order = $this->closedOrder();
        $this->actingAs($order->customer, 'sanctum')->postJson("/api/v1/orders/{$order->id}/review", ['quality' => 5, 'punctuality' => 5, 'conduct' => 5, 'tags' => $tags])->assertUnprocessable();
        $this->assertDatabaseCount('reviews', 0);
    }

    #[Test]
    public function other_customers_cannot_submit_review_tags(): void
    {
        $order = $this->closedOrder();
        $this->actingAs(User::factory()->create(), 'sanctum')->postJson("/api/v1/orders/{$order->id}/review", ['quality' => 5, 'punctuality' => 5, 'conduct' => 5])->assertNotFound();
        $this->assertDatabaseCount('reviews', 0);
    }

    private function closedOrder(): Order
    {
        return Order::factory()->create(['provider_profile_id' => ProviderProfile::factory(), 'status' => OrderStatus::Closed, 'closed_at' => now()]);
    }
}
