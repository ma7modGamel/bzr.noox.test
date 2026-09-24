<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Payments\Services\FawryWebhookSignature;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class PaymentFlowTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        // CFG-055 معطّل افتراضيًا حتى OD-10؛ هذه الاختبارات تغطي مسار فوري نفسه.
        app(SettingsRepository::class)->set(Cfg::ChannelFawryEnabled, true);
    }

    #[Test]
    public function ملخص_الدفع_يعيد_الإجراءات_حسب_الطريقة_المختارة(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder(PaymentMethod::Cash);
        $cash = $this->actingAs($customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk();

        $this->assertContains('change_payment_method', $cash->json('data.available_actions'));
        $this->assertNotContains('pay_electronic', $cash->json('data.available_actions'));

        $response = $this->actingAs($customer, 'sanctum')
            ->patchJson("/api/v1/orders/{$order->id}/payment-method", [
                'payment_method' => 'ELECTRONIC',
                'expected_version' => $order->version,
            ])
            ->assertOk()
            ->assertJsonPath('data.amounts.payment_method', 'ELECTRONIC');

        $this->assertContains('pay_electronic', $response->json('data.available_actions'));
        $this->assertDatabaseHas('order_events', [
            'order_id' => $order->id,
            'event_code' => OrderEventCode::PaymentMethodChanged->value,
        ]);
    }

    #[Test]
    public function إنشاء_كود_منافذ_يعيد_مرجعا_ولا_يكشف_التوقيع(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $response = $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/payments", [
                'channel' => 'KIOSK',
                'simulation' => 'PENDING',
                'expected_version' => $order->version,
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'PENDING')
            ->assertJsonPath('data.channel', 'KIOSK')
            ->assertJsonMissingPath('data.signature');

        $this->assertMatchesRegularExpression('/^\d{10}$/', (string) $response->json('data.reference_number'));
        $this->assertNotNull($response->json('data.expires_at'));
    }

    #[Test]
    public function نجاح_المحاكي_يمر_بنفس_webhook_ويغلق_الطلب_مرة_واحدة(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/payments", [
                'channel' => 'CARD',
                'simulation' => 'SUCCESS',
                'expected_version' => $order->version,
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'SUCCEEDED');

        $order->refresh();
        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame(OrderPaymentStatus::Paid, $order->payment_status);
        $this->assertNotNull($order->closed_at);
        $this->assertSame(1, $order->events()->where('event_code', OrderEventCode::ElectronicPaymentSucceeded)->count());

        $payment = $order->payments()->sole();
        $this->postJson('/webhooks/fawry', $this->signedWebhook($payment, 'PAID'))
            ->assertOk()
            ->assertJsonPath('received', true);
        $this->assertSame(1, $order->events()->where('event_code', OrderEventCode::ElectronicPaymentSucceeded)->count());
    }

    #[Test]
    public function الفشل_والانتهاء_يبقيان_الطلب_بانتظار_الدفع(): void
    {
        foreach (['FAILURE' => PaymentStatus::Failed, 'EXPIRY' => PaymentStatus::Expired] as $simulation => $expected) {
            [$order, $customer] = $this->awaitingPaymentOrder();
            $this->actingAs($customer, 'sanctum')
                ->postJson("/api/v1/orders/{$order->id}/payments", [
                    'channel' => 'WALLET',
                    'simulation' => $simulation,
                    'expected_version' => $order->version,
                ])
                ->assertCreated()
                ->assertJsonPath('data.status', $expected->value);

            $this->assertSame(OrderStatus::AwaitingPayment, $order->refresh()->status);
            $this->assertSame($expected, $order->payments()->latest('id')->firstOrFail()->status);
        }
    }

    #[Test]
    public function webhook_بتوقيع_خاطئ_يعيد_مئتين_بلا_تغيير(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $paymentId = $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/payments", [
                'channel' => 'CARD',
                'expected_version' => $order->version,
            ])
            ->assertCreated()
            ->json('data.id');
        $payment = Payment::query()->findOrFail($paymentId);
        $payload = $this->signedWebhook($payment, 'PAID');
        $payload['signature'] = str_repeat('0', 64);

        $this->postJson('/webhooks/fawry', $payload)->assertOk()->assertJsonPath('received', true);
        $this->assertSame(PaymentStatus::Pending, $payment->refresh()->status);
        $this->assertSame(OrderStatus::AwaitingPayment, $order->refresh()->status);
    }

    #[Test]
    public function المبلغ_غير_المطابق_يفشل_المحاولة_ولا_يغلق_الطلب(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $paymentId = $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/payments", [
                'channel' => 'CARD',
                'expected_version' => $order->version,
            ])->json('data.id');
        $payment = Payment::query()->findOrFail($paymentId);

        $this->postJson('/webhooks/fawry', $this->signedWebhook($payment, 'PAID', '699.00'))->assertOk();
        $this->assertSame(PaymentStatus::Failed, $payment->refresh()->status);
        $this->assertSame('AMOUNT_MISMATCH', $payment->failure_reason);
        $this->assertSame(OrderStatus::AwaitingPayment, $order->refresh()->status);
    }

    /** @return array{Order, User} */
    private function awaitingPaymentOrder(PaymentMethod $method = PaymentMethod::Electronic): array
    {
        $customer = User::factory()->create();
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::AwaitingPayment)->create([
            'customer_id' => $customer->id,
            'provider_profile_id' => $provider->id,
            'payment_method' => $method,
            'payment_status' => OrderPaymentStatus::Unpaid,
            'labor_total' => '500.00',
            'materials_total' => '200.00',
            'final_amount' => '700.00',
            'commission_rate' => '1.0000',
        ]);

        return [$order, $customer];
    }

    /** @return array<string, mixed> */
    private function signedWebhook(Payment $payment, string $status, ?string $amount = null): array
    {
        $payload = [
            'referenceNumber' => $payment->gateway_reference,
            'merchantRefNumber' => $payment->merchant_ref,
            'paymentAmount' => $amount ?? $payment->amount,
            'orderAmount' => $payment->amount,
            'fawryFees' => '0.00',
            'paymentMethod' => $payment->channel,
            'orderStatus' => $status,
        ];
        $payload['signature'] = app(FawryWebhookSignature::class)->make($payload);

        return $payload;
    }
}
