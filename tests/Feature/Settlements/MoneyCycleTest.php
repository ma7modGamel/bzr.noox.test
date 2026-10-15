<?php

declare(strict_types=1);

namespace Tests\Feature\Settlements;

use App\Filament\Pages\CommissionSummaryPage;
use App\Filament\Resources\Disputes\Pages\ViewDispute;
use App\Filament\Resources\Providers\Pages\ViewProviderProfile;
use App\Filament\Resources\Refunds\RefundResource;
use App\Filament\Resources\Settlements\SettlementResource;
use App\Modules\Identity\Models\Admin;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Payments\Actions\RecordRefundAction;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentStatus;
use App\Modules\Payments\Models\Payment;
use App\Modules\Providers\Enums\PayoutMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use App\Modules\Settlements\Actions\RecordProviderPayoutAction;
use App\Modules\Settlements\Actions\RecordProviderRemittanceAction;
use App\Modules\Settlements\Jobs\RefreshDuesBlocks;
use App\Modules\Settlements\Services\CommissionSummary;
use App\Modules\Settlements\Services\DuesBlocking;
use App\Modules\Settlements\Services\ProviderEarningsService;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use App\Support\Exceptions\BusinessRuleViolationException;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Str;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** الدفعة 7أ — DEC-066: الاسترداد والتحويل والسداد ومنع BR-063 وصفحات المال في اللوحة. */
final class MoneyCycleTest extends TestCase
{
    use RefreshDatabase;

    private Admin $super;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
        $this->super = Admin::factory()->super()->create();
    }

    /** BR-064 — التحويل لا يتجاوز المتاح، ويصل الفني NTF-23؛ ومدير التشغيل لا يسجل. */
    #[Test]
    public function التحويل_في_حدود_المتاح_ويبلغ_الفني(): void
    {
        $profile = ProviderProfile::factory()->independent()->create();
        $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Electronic, labor: '900.00', materials: '100.00', commission: '90.00');
        $payout = app(RecordProviderPayoutAction::class);

        $this->assertRule(fn () => $payout->execute($profile, $this->super, '911.00', PayoutMethod::Instapay, 'IPA-1', now()), 'BR-064');
        $this->assertRule(fn () => $payout->execute($profile, Admin::factory()->create(), '10.00', PayoutMethod::Instapay, 'IPA-1', now()), 'DEC-066');

        $payout->execute($profile, $this->super, '910.00', PayoutMethod::Instapay, 'IPA-1', now());

        $this->assertSame(0.0, app(ProviderEarningsService::class)->summary($profile)['balance']);
        $this->assertSame(1, $profile->user->notifications()->where('type', 'NTF-23')->count());
    }

    /** BR-063 — المهمة تمنع في وضع السوق فوق الحد وتبلغ مرة واحدة، والسداد يرفع المنع فورًا. */
    #[Test]
    public function المنع_بسبب_المستحقات_ورفعه_بالسداد(): void
    {
        $settings = app(SettingsRepository::class);
        $settings->set(Cfg::OffersEnabled, true);
        $settings->set(Cfg::ProviderDebtLimit, '500.00');
        $profile = ProviderProfile::factory()->independent()->create();
        $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Cash, labor: '6000.00', materials: '0.00', commission: '600.00');

        (new RefreshDuesBlocks)->handle(app(DuesBlocking::class));
        (new RefreshDuesBlocks)->handle(app(DuesBlocking::class));

        $this->assertTrue($profile->refresh()->isBlockedByDues());
        $this->assertSame(1, $profile->user->notifications()->where('type', 'NTF-24')->count());

        $remit = app(RecordProviderRemittanceAction::class);
        $this->assertRule(fn () => $remit->execute($profile, $this->super, '700.00', PayoutMethod::Instapay, 'R-1', now()), 'BR-063');
        $remit->execute($profile, $this->super, '150.00', PayoutMethod::Instapay, 'R-1', now());

        $this->assertFalse($profile->refresh()->isBlockedByDues());
    }

    /** BR-065 — لا منع في وضع الموظفين، وأي منع قائم يُرفع؛ ولا منع بلا حد (OD-02). */
    #[Test]
    public function لا_منع_في_وضع_الموظفين_ولا_بلا_حد(): void
    {
        $settings = app(SettingsRepository::class);
        $settings->set(Cfg::ProviderDebtLimit, '100.00');
        $employee = ProviderProfile::factory()->create(['dues_blocked_at' => now()]);
        $this->closedOrder($employee, OperatingMode::Employee, PaymentMethod::Cash, labor: '900.00', materials: '0.00', commission: '900.00');

        app(DuesBlocking::class)->refreshAll();
        $this->assertFalse($employee->refresh()->isBlockedByDues());

        $settings->set(Cfg::OffersEnabled, true);
        $settings->set(Cfg::ProviderDebtLimit, null);
        app(DuesBlocking::class)->refreshAll();
        $this->assertFalse($employee->refresh()->isBlockedByDues());
        $this->assertSame(0, $employee->user->notifications()->where('type', 'NTF-24')->count());
    }

    /** توريد الموظف أكبر من المستحق المغلق يحتاج تأكيدًا صريحًا (DEC-066). */
    #[Test]
    public function التوريد_فوق_المستحق_يحتاج_تأكيدا(): void
    {
        $profile = ProviderProfile::factory()->create();
        $this->closedOrder($profile, OperatingMode::Employee, PaymentMethod::Cash, labor: '450.00', materials: '200.00', commission: '450.00');
        $remit = app(RecordProviderRemittanceAction::class);

        $this->assertSame('450.00', $remit->debt($profile));
        $this->assertRule(fn () => $remit->execute($profile, $this->super, '600.00', PayoutMethod::Instapay, 'R-2', now()), 'BR-063');

        $remit->execute($profile, $this->super, '600.00', PayoutMethod::Instapay, 'R-2', now(), allowAboveDebt: true);
        $this->assertSame(150.0, app(ProviderEarningsService::class)->summary($profile)['balance']);
    }

    /** DEC-019 — استرداد جزئي بعد قرار نزاع: مبالغ الطلب، حالة الدفع، العمولة (BR-060)، EVT-082. */
    #[Test]
    public function الاسترداد_الجزئي_يحدث_الطلب_والعمولة(): void
    {
        $profile = ProviderProfile::factory()->independent()->create();
        $order = $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Electronic, labor: '900.00', materials: '100.00', commission: '90.00', rate: '0.1000');
        $this->paid($order);
        $dispute = $this->dispute($order, DisputeResolution::CloseWithRefund);
        $refund = app(RecordRefundAction::class);

        $this->assertRule(fn () => $refund->execute($dispute, $this->super, '901.00', '0', 'RF-1', 'استرداد جزء من المصنعية'), 'BR-060');
        $this->assertRule(fn () => $refund->execute($dispute, Admin::factory()->create(), '100.00', '0', 'RF-1', 'استرداد جزء من المصنعية'), 'DEC-066');

        $refund->execute($dispute, $this->super, '300.00', '50.00', 'RF-1', 'استرداد جزء من المصنعية');

        $order->refresh();
        $this->assertSame('300.00', $order->labor_refunded);
        $this->assertSame('350.00', $order->refunded_total);
        $this->assertSame(OrderPaymentStatus::PartiallyRefunded, $order->payment_status);
        $this->assertSame('60.00', $order->commission_amount);
        $this->assertTrue(OrderEvent::query()->where('order_id', $order->id)->where('event_code', 'EVT-082')->exists());
        $this->assertSame(['labor' => '600.00', 'materials' => '50.00', 'total' => '650.00'], $refund->remaining($order));
    }

    /** الإلغاء باسترداد كامل يُسجَّل بالمتبقي كله؛ والنقدي والنزاع بلا قرار استرداد مرفوضان. */
    #[Test]
    public function الاسترداد_الكامل_وحدود_الإتاحة(): void
    {
        $profile = ProviderProfile::factory()->independent()->create();
        $order = $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Electronic, labor: '400.00', materials: '100.00', commission: '40.00');
        $this->paid($order);
        $refund = app(RecordRefundAction::class);

        $this->assertRule(fn () => $refund->execute($this->dispute($order, DisputeResolution::CloseAsIs), $this->super, '10', '0', 'RF-2', 'سبب الاسترداد'), 'DEC-019');

        $full = $this->dispute($order, DisputeResolution::CancelFullRefund);
        $this->assertRule(fn () => $refund->execute($full, $this->super, '100', '0', 'RF-2', 'سبب الاسترداد'), 'DEC-019');
        $refund->execute($full, $this->super, '400', '100', 'RF-2', 'سبب الاسترداد');
        $this->assertSame(OrderPaymentStatus::Refunded, $order->refresh()->payment_status);

        $cash = $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Cash, labor: '400.00', materials: '0.00', commission: '40.00');
        $this->assertRule(fn () => $refund->execute($this->dispute($cash, DisputeResolution::CloseWithRefund), $this->super, '10', '0', 'RF-3', 'سبب الاسترداد'), 'DEC-019');
    }

    /** DEC-066 بند 6 — في وضع الموظفين ما استُرد من الخامات لا يُرد للموظف. */
    #[Test]
    public function كشف_الموظف_يخصم_الخامات_المستردة(): void
    {
        $profile = ProviderProfile::factory()->create();
        $order = $this->closedOrder($profile, OperatingMode::Employee, PaymentMethod::Electronic, labor: '450.00', materials: '200.00', commission: '450.00', rate: '1.0000');
        $this->paid($order);

        app(RecordRefundAction::class)->execute($this->dispute($order, DisputeResolution::CloseWithRefund), $this->super, '0', '80.00', 'RF-4', 'خامات لم تُستخدم');

        $this->assertSame(120.0, app(ProviderEarningsService::class)->summary($profile)['balance']);
    }

    /** صفحات المال للمدير العام وحده، والإجراءات تعمل من اللوحة. */
    #[Test]
    public function صفحات_المال_وإجراءاتها_في_اللوحة(): void
    {
        $profile = ProviderProfile::factory()->independent()->create();
        $order = $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Electronic, labor: '900.00', materials: '100.00', commission: '90.00', rate: '0.1000');
        $this->paid($order);
        $dispute = $this->dispute($order, DisputeResolution::CloseWithRefund);

        $this->actingAs(Admin::factory()->create(), 'admin');
        $this->get(SettlementResource::getUrl('index'))->assertForbidden();
        $this->get(RefundResource::getUrl('index'))->assertForbidden();
        $this->get(CommissionSummaryPage::getUrl())->assertForbidden();

        $this->flushSession();
        $this->actingAs($this->super, 'admin');
        $this->get(SettlementResource::getUrl('index'))->assertOk()->assertSee($profile->user->name);
        $this->get(CommissionSummaryPage::getUrl())->assertOk();
        Livewire::test(CommissionSummaryPage::class)
            ->fillForm(['from' => now()->subMonth()->toDateString(), 'to' => now()->toDateString()])
            ->assertSee('900.00')
            ->assertSee('90.00');

        Livewire::test(ViewProviderProfile::class, ['record' => $profile->getKey()])
            ->callAction('recordPayout', ['amount' => '100', 'method' => 'INSTAPAY', 'reference' => 'IPA-9', 'paid_at' => now()->subMinute()->toDateTimeString()])
            ->assertHasNoActionErrors();
        $this->assertSame(1, $profile->payouts()->count());

        Livewire::test(ViewDispute::class, ['record' => $dispute->getKey()])
            ->assertActionVisible('recordRefund')
            ->callAction('recordRefund', ['labor_amount' => '100', 'materials_amount' => '0', 'reference' => 'RF-9', 'reason' => 'خطأ في التنفيذ'])
            ->assertHasNoActionErrors();
        $this->assertSame('100.00', $order->refresh()->refunded_total);
        $this->get(RefundResource::getUrl('index'))->assertOk()->assertSee('RF-9');
    }

    /** ملخص العمولات مقسوم حسب الوضع، والإجمالي مجموعهما (DEC-066). */
    #[Test]
    public function ملخص_العمولات_حسب_الوضع(): void
    {
        $profile = ProviderProfile::factory()->create();
        $this->closedOrder($profile, OperatingMode::Employee, PaymentMethod::Cash, labor: '450.00', materials: '200.00', commission: '450.00');
        $this->closedOrder($profile, OperatingMode::Marketplace, PaymentMethod::Cash, labor: '900.00', materials: '0.00', commission: '90.00');

        $summary = app(CommissionSummary::class)->between(now()->subMonth(), now());

        $this->assertSame([1, '450.00'], [$summary['EMPLOYEE']['orders'], $summary['EMPLOYEE']['commission']]);
        $this->assertSame([1, '90.00'], [$summary['MARKETPLACE']['orders'], $summary['MARKETPLACE']['commission']]);
        $this->assertSame([2, '540.00', '200.00'], [$summary['TOTAL']['orders'], $summary['TOTAL']['commission'], $summary['TOTAL']['materials']]);
    }

    private function closedOrder(
        ProviderProfile $profile,
        OperatingMode $mode,
        PaymentMethod $method,
        string $labor,
        string $materials,
        string $commission,
        string $rate = '0.1000',
    ): Order {
        return Order::factory()->status(OrderStatus::Closed)->create([
            'provider_profile_id' => $profile->getKey(),
            'operating_mode' => $mode,
            'payment_method' => $method,
            'payment_status' => OrderPaymentStatus::Paid,
            'commission_rate' => $mode === OperatingMode::Employee ? '1.0000' : $rate,
            'labor_total' => $labor,
            'materials_total' => $materials,
            'final_amount' => bcadd($labor, $materials, 2),
            'commission_amount' => $commission,
            'closed_at' => now()->subDays(5),
            'settlement_eligible_at' => now()->subDays(2),
        ]);
    }

    private function paid(Order $order): Payment
    {
        return Payment::query()->create([
            'order_id' => $order->getKey(),
            'method' => PaymentMethod::Electronic,
            'amount' => $order->final_amount,
            'status' => PaymentStatus::Succeeded,
            'gateway' => 'INSTAPAY_MANUAL',
            'merchant_ref' => 'BZR-'.$order->getKey().'-'.Str::upper((string) Str::ulid()),
            'paid_at' => now()->subDays(5),
        ]);
    }

    private function dispute(Order $order, DisputeResolution $resolution): Dispute
    {
        return Dispute::query()->create([
            'order_id' => $order->getKey(), 'opened_by_type' => 'CUSTOMER', 'opened_by_id' => $order->customer_id,
            'reason_code' => 'QUALITY', 'description' => 'التنفيذ لم يكتمل كما اتفقنا.', 'is_post_close' => true,
            'status' => DisputeStatus::Resolved, 'resolution' => $resolution, 'resolution_note' => 'قرار بعد المراجعة',
            'resolved_by' => $this->super->getKey(), 'resolved_at' => now(),
        ]);
    }

    private function assertRule(callable $call, string $rule): void
    {
        try {
            $call();
        } catch (BusinessRuleViolationException $e) {
            $this->assertSame($rule, $e->context['rule'] ?? null, $e->getMessage());

            return;
        }

        $this->fail("Expected {$rule} to be enforced.");
    }
}
