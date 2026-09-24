<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Actions\ConfirmInstapayTransferAction;
use App\Modules\Payments\Actions\RejectInstapayTransferAction;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Enums\TransferRejectionReason;
use App\Modules\Payments\Models\Payment;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Database\Seeders\SettingsSeeder;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** DEC-050 / BR-057 — تحويل إنستاباي يدوي بتأكيد المدير العام. */
final class InstapayTransferTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $settings = app(SettingsRepository::class);
        $settings->set(Cfg::InstapayAddress, 'bremo@instapay');
        $settings->set(Cfg::InstapayDisplayName, 'Bremo');
    }

    #[Test]
    public function الإعدادات_تعيد_القنوات_المفعلة_وبيانات_إنستاباي_فقط(): void
    {
        $config = $this->getJson('/api/v1/config')->assertOk();

        $this->assertSame(['CASH', 'INSTAPAY_MANUAL'], array_column($config->json('option_lists.payment_gateways'), 'code'));
        $config->assertJsonPath('instapay.address', 'bremo@instapay')
            ->assertJsonPath('instapay.display_name', 'Bremo')
            ->assertJsonPath('instapay.link', null);

        app(SettingsRepository::class)->set(Cfg::ChannelInstapayEnabled, false);
        $config = $this->getJson('/api/v1/config')->assertOk();
        $this->assertSame(['CASH'], array_column($config->json('option_lists.payment_gateways'), 'code'));
        $config->assertJsonPath('instapay', null);
    }

    #[Test]
    public function القناة_لا_تظهر_قبل_إدخال_عنوان_المنصة(): void
    {
        app(SettingsRepository::class)->set(Cfg::InstapayAddress, '');

        $config = $this->getJson('/api/v1/config')->assertOk();
        $this->assertNotContains('INSTAPAY_MANUAL', array_column($config->json('option_lists.payment_gateways'), 'code'));
    }

    #[Test]
    public function إرسال_التحويل_يبقي_الطلب_بانتظار_الدفع_ويعرض_حالة_انتظار_التأكيد(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $before = $this->actingAs($customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk();
        $this->assertContains('submit_instapay_transfer', $before->json('data.available_actions'));
        $this->assertNotContains('pay_electronic', $before->json('data.available_actions')); // فوري معطّل (OD-10)
        $before->assertJsonPath('data.payment_reference', '#'.$order->number);

        $response = $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/instapay-transfers", [
                'transfer_reference' => 'IPN-778812',
                'expected_version' => $order->version,
            ])
            ->assertCreated()
            ->assertJsonPath('data.status', 'PENDING_VERIFICATION')
            ->assertJsonPath('data.gateway', 'INSTAPAY_MANUAL')
            ->assertJsonPath('data.transfer_reference', 'IPN-778812')
            ->assertJsonPath('order.status', 'AWAITING_PAYMENT')
            ->assertJsonPath('order.display_status', 'order.status.customer.AWAITING_TRANSFER_VERIFICATION');

        $actions = $response->json('order.available_actions');
        $this->assertNotContains('submit_instapay_transfer', $actions);
        $this->assertNotContains('change_payment_method', $actions);
        $this->assertDatabaseHas('order_events', [
            'order_id' => $order->id,
            'event_code' => OrderEventCode::InstapayTransferSubmitted->value,
        ]);
    }

    #[Test]
    public function الرقم_المرجعي_إلزامي_ولا_يقبل_تحويلا_ثانيا_معلقا(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/instapay-transfers", ['expected_version' => $order->version])
            ->assertUnprocessable();

        $this->submit($order, $customer, 'IPN-1');
        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/instapay-transfers", [
                'transfer_reference' => 'IPN-2',
                'expected_version' => $order->refresh()->version,
            ])
            ->assertUnprocessable()
            ->assertJsonPath('error.code', 'BUSINESS_RULE_VIOLATION');
    }

    #[Test]
    public function الفني_لا_يسجل_نقديا_والعميل_لا_يغير_الطريقة_أثناء_الانتظار(): void
    {
        [$order, $customer, $provider] = $this->awaitingPaymentOrder();
        $this->submit($order, $customer, 'IPN-3');
        $order->refresh();

        $this->actingAs($customer, 'sanctum')
            ->patchJson("/api/v1/orders/{$order->id}/payment-method", [
                'payment_method' => 'CASH',
                'expected_version' => $order->version,
            ])
            ->assertUnprocessable();

        $this->actingAs($provider->user, 'sanctum')
            ->withHeader('X-App-Mode', 'provider')
            ->postJson("/api/v1/provider/orders/{$order->id}/cash-received", [
                'amount' => '700.00',
                'expected_version' => $order->version,
            ])
            ->assertUnprocessable();
    }

    #[Test]
    public function تأكيد_المدير_العام_يغلق_الطلب_بأثر_t20(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $this->submit($order, $customer, 'IPN-4');
        $payment = Payment::query()->where('order_id', $order->id)->sole();

        app(ConfirmInstapayTransferAction::class)->execute($payment, Admin::factory()->super()->create());

        $order->refresh();
        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame(OrderPaymentStatus::Paid, $order->payment_status);
        $this->assertSame(PaymentMethod::Electronic, $order->payment_method);
        $this->assertSame(PaymentStatus::Succeeded, $payment->refresh()->status);
        $this->assertNotNull($payment->verified_at);
        $this->assertSame(1, $order->events()->where('event_code', OrderEventCode::ElectronicPaymentSucceeded)->count());
    }

    #[Test]
    public function مدير_التشغيل_لا_يؤكد_ولا_يرفض(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $this->submit($order, $customer, 'IPN-5');
        $payment = Payment::query()->where('order_id', $order->id)->sole();
        $operations = Admin::factory()->create();

        try {
            app(ConfirmInstapayTransferAction::class)->execute($payment, $operations);
            $this->fail('Operations manager confirmed a transfer.');
        } catch (AuthorizationException) {
        }

        $this->expectException(AuthorizationException::class);
        app(RejectInstapayTransferAction::class)->execute($payment, $operations, TransferRejectionReason::NotReceived);
    }

    #[Test]
    public function الرفض_يفشل_الدفعة_ويبلغ_العميل_ويسمح_بإعادة_المحاولة(): void
    {
        [$order, $customer] = $this->awaitingPaymentOrder();
        $this->submit($order, $customer, 'IPN-6');
        $payment = Payment::query()->where('order_id', $order->id)->sole();

        app(RejectInstapayTransferAction::class)->execute(
            $payment,
            Admin::factory()->super()->create(),
            TransferRejectionReason::AmountMismatch,
            'وصل 500 فقط',
        );

        $this->assertSame(PaymentStatus::Failed, $payment->refresh()->status);
        $this->assertSame('AMOUNT_MISMATCH', $payment->failure_reason);
        $this->assertSame(OrderStatus::AwaitingPayment, $order->refresh()->status);

        $notifications = $this->actingAs($customer, 'sanctum')->getJson('/api/v1/notifications')->assertOk();
        $notifications->assertJsonPath('data.0.code', 'NTF-30');

        $retry = $this->actingAs($customer, 'sanctum')->getJson("/api/v1/orders/{$order->id}")->assertOk();
        $this->assertContains('submit_instapay_transfer', $retry->json('data.available_actions'));
        $this->assertContains('change_payment_method', $retry->json('data.available_actions'));
        $this->submit($order->refresh(), $customer, 'IPN-7');
    }

    #[Test]
    public function قناة_معطلة_ترفض_الإرسال(): void
    {
        app(SettingsRepository::class)->set(Cfg::ChannelInstapayEnabled, false);
        [$order, $customer] = $this->awaitingPaymentOrder();

        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/instapay-transfers", [
                'transfer_reference' => 'IPN-8',
                'expected_version' => $order->version,
            ])
            ->assertStatus(409);
    }

    private function submit(Order $order, User $customer, string $reference): void
    {
        $this->actingAs($customer, 'sanctum')
            ->postJson("/api/v1/orders/{$order->id}/instapay-transfers", [
                'transfer_reference' => $reference,
                'expected_version' => $order->version,
            ])
            ->assertCreated();
    }

    /** @return array{0: Order, 1: User, 2: ProviderProfile} */
    private function awaitingPaymentOrder(): array
    {
        $customer = User::factory()->create();
        $provider = ProviderProfile::factory()->create();
        $order = Order::factory()->status(OrderStatus::AwaitingPayment)->create([
            'customer_id' => $customer->id,
            'provider_profile_id' => $provider->id,
            'payment_method' => PaymentMethod::Electronic,
            'payment_status' => OrderPaymentStatus::Unpaid,
            'labor_total' => '500.00',
            'materials_total' => '200.00',
            'final_amount' => '700.00',
            'commission_rate' => '1.0000',
        ]);

        return [$order, $customer, $provider];
    }
}
