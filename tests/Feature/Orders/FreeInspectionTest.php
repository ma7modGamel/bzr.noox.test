<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\CompleteInspectionOnlyAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Pricing\Models\PriceProposal;
use App\Modules\Pricing\Services\OrderAmounts;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** T-28 — BR-056، BR-008، AC-PH1-06، AC-PH1-07. */
final class FreeInspectionTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    /** AC-PH1-06 — الزيارة بلا تنفيذ تُغلق الطلب فورًا بلا مبلغ ولا محاولة دفع. */
    #[Test]
    public function المعاينة_المجانية_بلا_تنفيذ_تغلق_الطلب_بمبلغ_صفر(): void
    {
        $order = $this->arrivedOrder();

        $order = app(CompleteInspectionOnlyAction::class)
            ->execute($order, ActorType::Provider, $order->provider_profile_id);

        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame('0.00', $order->final_amount);
        $this->assertSame('0.00', $order->commission_amount);
        $this->assertSame(OrderPaymentStatus::Waived, $order->payment_status);
        $this->assertNotNull($order->closed_at);
        $this->assertSame(0, $order->payments()->count());

        $event = $order->events()->where('event_code', OrderEventCode::ClosedFreeInspection->value)->sole();
        $this->assertSame('T-28', $event->meta['transition']);
        $this->assertSame(OrderStatus::Arrived, $event->from_status);
    }

    /** AC-PH1-07 — عرض التنفيذ وحده يحدد المصنعية، والعمولة 100% في وضع الموظفين. */
    #[Test]
    public function عرض_التنفيذ_يحدد_المصنعية_والعمولة_كامل_المصنعية(): void
    {
        $order = $this->arrivedOrder();

        PriceProposal::query()->create([
            'order_id' => $order->id,
            'provider_profile_id' => $order->provider_profile_id,
            'type' => ProposalType::ExecutionQuote,
            'amount' => '450.00',
            'reason' => 'تغيير خرطوم وصيانة الخلاط',
            'status' => ProposalStatus::Approved,
            'expires_at' => now()->addHour(),
            'decided_at' => now(),
        ]);

        PriceProposal::query()->create([
            'order_id' => $order->id,
            'provider_profile_id' => $order->provider_profile_id,
            'type' => ProposalType::Materials,
            'amount' => '200.00',
            'reason' => 'خرطوم وخلاط',
            'status' => ProposalStatus::Approved,
            'expires_at' => now()->addHour(),
            'decided_at' => now(),
        ]);

        $amounts = OrderAmounts::for($order->refresh());

        $this->assertSame('450.00', $amounts->laborTotal);
        $this->assertSame('200.00', $amounts->materialsTotal);
        $this->assertSame('650.00', $amounts->finalAmount);
        $this->assertFalse($amounts->isZero());

        // BR-065 — الرصيد المحسوب سيكون −450 (المطلوب توريده) في الطلب النقدي
        $this->assertSame('450.00', $amounts->commission($order));
    }

    #[Test]
    public function الإنهاء_بالمعاينة_مرفوض_لطلب_تنفيذ(): void
    {
        $order = $this->arrivedOrder();
        $order->forceFill(['pricing_mode' => 'EXECUTION'])->save();

        $this->expectException(\App\Support\Exceptions\BusinessRuleViolationException::class);

        app(CompleteInspectionOnlyAction::class)
            ->execute($order->refresh(), ActorType::Provider, $order->provider_profile_id);
    }

    private function arrivedOrder(): Order
    {
        $order = Order::factory()->create();
        $provider = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();

        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        // مسار الزيارة: CONFIRMED ← ON_THE_WAY ← ARRIVED (T-05، T-10)
        $machine = app(\App\Modules\Orders\StateMachine\OrderStateMachine::class);
        $order = $machine->apply($order, 'startTrip', ActorType::Provider, $provider->getKey());

        return $machine->apply($order, 'markArrived', ActorType::Provider, $provider->getKey());
    }
}
