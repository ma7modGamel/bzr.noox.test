<?php

declare(strict_types=1);

namespace Tests\Feature\Api;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class ProviderExecutionApiTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    #[Test]
    public function config_returns_provider_reasons_as_backend_options(): void
    {
        $this->getJson('/api/v1/config')
            ->assertOk()
            ->assertJsonPath('option_lists.provider_cancellation_reasons.0.code', 'EMERGENCY')
            ->assertJsonPath('option_lists.provider_cancellation_reasons.0.label', 'ظرف طارئ')
            ->assertJsonPath('option_lists.proposal_types.0.code', 'EXECUTION_QUOTE')
            ->assertJsonPath('option_lists.proposal_types.0.label', 'عرض تنفيذ')
            ->assertJsonFragment(['code' => 'OTHER', 'label' => 'سبب آخر']);
    }

    #[Test]
    public function current_orders_return_full_execution_context_and_employee_price_guide(): void
    {
        [$provider, $order] = $this->assignedOrder(OrderStatus::Arrived);
        $order->problemType()->update([
            'employee_price_min' => '300.00',
            'employee_price_max' => '550.00',
            'is_other' => false,
        ]);

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/orders?scope=current')
            ->assertOk()
            ->assertJsonPath('data.0.id', $order->getKey())
            ->assertJsonPath('data.0.location.address_text', 'شارع الطيران، عمارة 12')
            ->assertJsonPath('data.0.execution_price_guide.minimum', '300.00')
            ->assertJsonPath('data.0.execution_price_guide.maximum', '550.00')
            ->assertJsonPath('data.0.execution_price_guide.currency', 'جنيه')
            ->assertJsonPath('data.0.price_guide_review_required', false)
            ->assertJsonPath('data.0.available_actions.0', 'submit_execution_quote');
    }

    #[Test]
    public function other_problem_has_no_required_range_and_is_marked_for_review(): void
    {
        [$provider, $order] = $this->assignedOrder(OrderStatus::Arrived);
        $order->problemType()->update([
            'employee_price_min' => null,
            'employee_price_max' => null,
            'is_other' => true,
        ]);

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->getJson('/api/v1/provider/orders?scope=current')
            ->assertOk()
            ->assertJsonPath('data.0.execution_price_guide', null)
            ->assertJsonPath('data.0.price_guide_review_required', true);
    }

    #[Test]
    public function execution_quote_claims_owned_uploaded_photo_atomically(): void
    {
        [$provider, $order] = $this->assignedOrder(OrderStatus::Arrived);
        $order->problemType()->update([
            'employee_price_min' => '300.00',
            'employee_price_max' => '550.00',
            'is_other' => false,
        ]);
        $media = OrderMedia::query()->create([
            'uploaded_by' => $provider->user_id,
            'expires_at' => now()->addDay(),
            'type' => 'IMAGE',
            'path' => 'order-media/proposal.jpg',
            'size_bytes' => 128,
        ]);

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->getKey()}/proposals", [
                'expected_version' => $order->version,
                'type' => 'EXECUTION_QUOTE',
                'amount' => 450,
                'reason' => 'إصلاح التسريب وتغيير الوصلة',
                'photo_media_id' => $media->getKey(),
            ])
            ->assertCreated()
            ->assertJsonPath('data.photo_path', 'order-media/proposal.jpg')
            ->assertJsonPath('data.outside_price_guide', false);

        $this->assertSame($order->getKey(), $media->fresh()->order_id);
        $this->assertNull($media->fresh()->expires_at);
    }

    #[Test]
    public function execution_quote_rejects_a_photo_owned_by_another_user_without_creating_proposal(): void
    {
        [$provider, $order] = $this->assignedOrder(OrderStatus::Arrived);
        $media = OrderMedia::query()->create([
            'uploaded_by' => User::factory()->create()->getKey(),
            'expires_at' => now()->addDay(),
            'type' => 'IMAGE',
            'path' => 'order-media/foreign.jpg',
            'size_bytes' => 128,
        ]);

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->getKey()}/proposals", [
                'expected_version' => $order->version,
                'type' => 'EXECUTION_QUOTE',
                'amount' => 450,
                'reason' => 'إصلاح التسريب وتغيير الوصلة',
                'photo_media_id' => $media->getKey(),
            ])
            ->assertUnprocessable()
            ->assertJsonPath('error.code', 'VALIDATION_FAILED')
            ->assertJsonStructure(['error' => ['fields' => ['photo_media_id']]]);

        $this->assertDatabaseCount('price_proposals', 0);
        $this->assertNull($media->fresh()->order_id);
    }

    #[Test]
    public function back_out_rejects_non_provider_reason_with_validation_error(): void
    {
        [$provider, $order] = $this->assignedOrder(OrderStatus::Confirmed);

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'PROVIDER')
            ->postJson("/api/v1/provider/orders/{$order->getKey()}/back-out", [
                'expected_version' => $order->version,
                'reason_code' => 'FOUND_ANOTHER',
            ])
            ->assertUnprocessable()
            ->assertJsonPath('error.code', 'VALIDATION_FAILED')
            ->assertJsonStructure(['error' => ['fields' => ['reason_code']]]);

        $this->assertSame(OrderStatus::Confirmed, $order->fresh()->status);
    }

    /** @return array{ProviderProfile, Order} */
    private function assignedOrder(OrderStatus $status): array
    {
        $order = Order::factory()->status($status)->create([
            'arrived_at' => $status === OrderStatus::Arrived ? now() : null,
        ]);
        $provider = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();
        $order->update(['provider_profile_id' => $provider->getKey()]);

        return [$provider, $order->fresh()];
    }
}
