<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\MarkArrivedAction;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Jobs\ExpirePendingProposals;
use App\Modules\Orders\Jobs\ExpireStaleOrders;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderTermination;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Pricing\Actions\DecideProposalAction;
use App\Modules\Pricing\Actions\SubmitProposalAction;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Providers\Models\ProviderProfile;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** 30 §المهام المجدولة — AC-PH1-05، AC-PRC-03. */
final class ScheduledJobsTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    /** AC-PH1-05 — طلب بلا تعيين حتى CFG-093 ينتهي. */
    #[Test]
    public function الطلب_بلا_تعيين_ينتهي_عند_مهلة_التعيين(): void
    {
        $order = $this->publish();
        $this->assertSame(OrderStatus::Open, $order->status);

        // قبل المهلة: لا شيء
        CarbonImmutable::setTestNow(CarbonImmutable::now()->addMinutes(30));
        $this->assertSame(0, app(ExpireStaleOrders::class)->handle(
            app(OrderStateMachine::class),
            app(OrderTermination::class),
        ));

        // بعد CFG-093 = 60 دقيقة
        CarbonImmutable::setTestNow(CarbonImmutable::now()->addMinutes(31));
        $this->assertSame(1, app(ExpireStaleOrders::class)->handle(
            app(OrderStateMachine::class),
            app(OrderTermination::class),
        ));

        $order->refresh();
        $this->assertSame(OrderStatus::Expired, $order->status);
        $this->assertNotNull($order->expired_at);
        $this->assertContains(
            'EVT-005',
            $order->events()->pluck('event_code')->map(fn ($c) => $c->value)->all(),
        );
    }

    /** الطلب المعيَّن لا تمسه المهمة. */
    #[Test]
    public function الطلب_المعين_لا_ينتهي(): void
    {
        $order = $this->publish();
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());

        CarbonImmutable::setTestNow(CarbonImmutable::now()->addHours(3));

        app(ExpireStaleOrders::class)->handle(
            app(OrderStateMachine::class),
            app(OrderTermination::class),
        );

        $this->assertSame(OrderStatus::Confirmed, $order->refresh()->status);
    }

    /** AC-PRC-03 — انتهاء مهلة عرض التنفيذ بعد معاينة مجانية يغلق الطلب بلا مبلغ (T-28). */
    #[Test]
    public function انتهاء_مهلة_عرض_التنفيذ_يغلق_الطلب_المجاني(): void
    {
        $order = $this->publish();
        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $order = app(AssignProviderAction::class)->execute($order, $provider, Admin::factory()->create());
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);
        // BR-046: عرض التنفيذ في وضع الموظفين يتطلب دليل سعر فعّالًا لنوع المشكلة.
        $order->problemType()->firstOrFail()->update([
            'employee_price_min' => '300.00',
            'employee_price_max' => '500.00',
        ]);

        $quote = app(SubmitProposalAction::class)->execute(
            $order, $provider, ProposalType::ExecutionQuote, '450.00', 'يحتاج تغيير المواسير',
        );

        // CFG-040 = 60 دقيقة
        CarbonImmutable::setTestNow(CarbonImmutable::now()->addMinutes(61));

        $this->assertSame(1, app(ExpirePendingProposals::class)->handle(
            app(DecideProposalAction::class),
        ));

        $this->assertSame(ProposalStatus::Expired, $quote->refresh()->status);

        $order->refresh();
        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame('0.00', $order->final_amount);
        $this->assertSame(OrderPaymentStatus::Waived, $order->payment_status);
    }

    private function publish(): Order
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();

        return app(PublishRequestAction::class)->execute($customer, new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $category->problemTypes()->where('is_other', false)->value('id'),
            timingType: TimingType::Now,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: true,
            description: 'تسريب أسفل الحوض.',
        ));
    }
}
