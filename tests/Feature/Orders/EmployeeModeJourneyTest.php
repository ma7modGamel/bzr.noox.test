<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\CompleteWorkAction;
use App\Modules\Orders\Actions\ConfirmCompletionAction;
use App\Modules\Orders\Actions\MarkArrivedAction;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Actions\RecordLocationAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Actions\ConfirmCashReceivedAction;
use App\Modules\Payments\Enums\OrderPaymentStatus;
use App\Modules\Pricing\Actions\DecideProposalAction;
use App\Modules\Pricing\Actions\SubmitProposalAction;
use App\Modules\Pricing\Enums\PriceReviewReason;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * الرحلة الكاملة للمرحلة الأولى (39):
 * نشر ← تعيين ← تحرك ← وصول ← معاينة مجانية ← عرض تنفيذ ← موافقة ← خامات ← إنهاء ← نقدي ← إغلاق.
 */
final class EmployeeModeJourneyTest extends TestCase
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

    #[Test]
    public function الرحلة_الكاملة_من_النشر_حتى_الإغلاق(): void
    {
        [$customer, $provider, $admin, $order] = $this->publishAndAssign();

        // T-05 بدء التحرك + BR-110 إرسال الموقع
        $order = app(StartTripAction::class)->execute($order, $provider);
        $this->assertSame(OrderStatus::OnTheWay, $order->status);

        app(RecordLocationAction::class)->execute($order, $provider, 30.0590, 31.3390);
        CarbonImmutable::setTestNow(CarbonImmutable::now()->addSeconds(30));
        app(RecordLocationAction::class)->execute($order, $provider, 30.0598, 31.3398);
        $this->assertSame(2, $order->trackingPoints()->count());

        // T-10 الوصول: نقطة الوصول تُحفظ والمسار يُحذف (BR-111، BR-112)
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);
        $this->assertSame(OrderStatus::Arrived, $order->status);
        $this->assertSame(0, $order->trackingPoints()->count());
        $this->assertNotNull($order->arrived_lat);
        $this->assertLessThan(50, $order->arrival_distance_m);

        // T-12 عرض التنفيذ من الموقع — المصدر الوحيد للسعر في المرحلة الأولى
        $quote = app(SubmitProposalAction::class)->execute(
            $order, $provider, ProposalType::ExecutionQuote, '450.00', 'تغيير خرطوم وصيانة الخلاط',
        );
        $order->refresh();
        $this->assertSame(OrderStatus::AwaitingQuoteApproval, $order->status);

        // T-14 موافقة العميل
        $order = app(DecideProposalAction::class)->approve($order, $quote, ActorType::Customer, $customer->id);
        $this->assertSame(OrderStatus::InProgress, $order->status);

        // خامات أثناء التنفيذ — لا تغيّر الحالة (12)
        $materials = app(SubmitProposalAction::class)->execute(
            $order, $provider, ProposalType::Materials, '200.00', 'خرطوم وخلاط',
        );
        $order = app(DecideProposalAction::class)->approve($order->refresh(), $materials, ActorType::Customer, $customer->id);
        $this->assertSame(OrderStatus::InProgress, $order->status);

        // T-17 الإنهاء: المبالغ تُثبَّت (BR-044)
        $order = app(CompleteWorkAction::class)->execute($order, $provider);
        $this->assertSame(OrderStatus::AwaitingPayment, $order->status);
        $this->assertSame('450.00', $order->labor_total);
        $this->assertSame('200.00', $order->materials_total);
        $this->assertSame('650.00', $order->final_amount);

        // T-19 استلام نقدي كامل (BR-053)
        $order = app(ConfirmCashReceivedAction::class)
            ->execute($order, '650.00', ActorType::Provider, $provider->id);
        $this->assertSame(OrderStatus::AwaitingConfirmation, $order->status);
        $this->assertSame(OrderPaymentStatus::Paid, $order->payment_status);

        // T-21 تأكيد العميل ← الإغلاق والعمولة (BR-060، BR-065)
        $order = app(ConfirmCompletionAction::class)->execute($order, ActorType::Customer, $customer->id);
        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame('450.00', $order->commission_amount); // 100% من المصنعية — وضع الموظفين
        $this->assertNotNull($order->settlement_eligible_at);

        // BR-061 — قابل للتسوية بعد CFG-060 = 72 ساعة
        $this->assertSame(72, (int) $order->closed_at->diffInHours($order->settlement_eligible_at));

        // السجل الكامل يسمح بإعادة بناء قصة الطلب (AC-ADM-02)
        $codes = $order->events()->pluck('event_code')->map(fn ($c) => $c->value)->all();
        $this->assertSame([
            'EVT-001', 'EVT-011', 'EVT-020', 'EVT-022', 'EVT-040',
            'EVT-041', 'EVT-040', 'EVT-041', 'EVT-050', 'EVT-061', 'EVT-070',
        ], $codes);
    }

    /** BR-056 — رفض عرض التنفيذ بعد معاينة مجانية يغلق الطلب بلا أي مبلغ (T-28). */
    #[Test]
    public function رفض_عرض_التنفيذ_بعد_معاينة_مجانية_يغلق_بلا_مبلغ(): void
    {
        [$customer, $provider, , $order] = $this->publishAndAssign();

        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $quote = app(SubmitProposalAction::class)->execute(
            $order, $provider, ProposalType::ExecutionQuote, '450.00', 'يحتاج تغيير المواسير',
        );

        $order = app(DecideProposalAction::class)->reject($order->refresh(), $quote, ActorType::Customer, $customer->id);

        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame('0.00', $order->final_amount);
        $this->assertSame(OrderPaymentStatus::Waived, $order->payment_status);
        $this->assertSame(0, $order->payments()->count());
    }

    /** BR-042 — عرض التنفيذ مرة واحدة مهما كانت نتيجته. */
    #[Test]
    public function عرض_التنفيذ_لا_يتكرر(): void
    {
        [, $provider, , $order] = $this->publishAndAssign();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::ExecutionQuote, '450.00', 'سبب كافٍ');

        $this->expectException(BusinessRuleViolationException::class);
        app(SubmitProposalAction::class)->execute($order->refresh(), $provider, ProposalType::ExecutionQuote, '500.00', 'سبب آخر');
    }

    #[Test]
    public function السعر_خارج_الدليل_يمر_للعميل_بسبب_ويتعلم_للمراجعة(): void
    {
        [, $provider, , $order] = $this->publishAndAssign();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $quote = app(SubmitProposalAction::class)->execute(
            $order,
            $provider,
            ProposalType::ExecutionQuote,
            '650.00',
            'إصلاح كامل بعد المعاينة',
            outsidePriceGuideReason: 'حالة المواسير تحتاج عملا إضافيا',
        );

        $this->assertSame('300.00', $quote->price_guide_min);
        $this->assertSame('500.00', $quote->price_guide_max);
        $this->assertTrue($quote->outside_price_guide);
        $this->assertSame(PriceReviewReason::OutsideRange, $quote->price_review_reason);
        $this->assertSame('حالة المواسير تحتاج عملا إضافيا', $quote->outside_price_guide_reason);
        $this->assertSame(OrderStatus::AwaitingQuoteApproval, $order->refresh()->status);
    }

    #[Test]
    public function السعر_خارج_الدليل_يرفض_بلا_سبب(): void
    {
        [, $provider, , $order] = $this->publishAndAssign();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $this->expectException(BusinessRuleViolationException::class);
        $this->expectExceptionMessage('سبب السعر خارج دليل الأسعار');

        app(SubmitProposalAction::class)->execute(
            $order, $provider, ProposalType::ExecutionQuote, '650.00', 'إصلاح كامل بعد المعاينة',
        );
    }

    #[Test]
    public function مشكلة_أخرى_معفاة_من_الدليل_وكل_سعر_فيها_يتعلم_للمراجعة(): void
    {
        [, $provider, , $order] = $this->publishAndAssign(otherProblem: true, withGuide: false);
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $quote = app(SubmitProposalAction::class)->execute(
            $order, $provider, ProposalType::ExecutionQuote, '900.00', 'تفاصيل الحالة بعد المعاينة',
        );

        $this->assertTrue($quote->outside_price_guide);
        $this->assertSame(PriceReviewReason::OtherProblem, $quote->price_review_reason);
        $this->assertNull($quote->outside_price_guide_reason);
        $this->assertSame(OrderStatus::AwaitingQuoteApproval, $order->refresh()->status);
    }

    #[Test]
    public function النوع_العادي_بلا_دليل_يرفض_عرض_التنفيذ(): void
    {
        [, $provider, , $order] = $this->publishAndAssign(withGuide: false);
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        try {
            app(SubmitProposalAction::class)->execute(
                $order, $provider, ProposalType::ExecutionQuote, '450.00', 'تفاصيل التنفيذ المطلوبة',
            );
            $this->fail('كان يجب رفض العرض بلا دليل سعر.');
        } catch (BusinessRuleViolationException $exception) {
            $this->assertSame('BR-046', $exception->context['rule']);
            $this->assertSame('PRICE_GUIDE_MISSING', $exception->context['reason']);
        }
    }

    /** BR-045 — لا إنهاء مع مقترح معلّق. */
    #[Test]
    public function الإنهاء_مرفوض_مع_مقترح_معلق(): void
    {
        [$customer, $provider, , $order] = $this->publishAndAssign();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $quote = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::ExecutionQuote, '450.00', 'سبب كافٍ');
        $order = app(DecideProposalAction::class)->approve($order->refresh(), $quote, ActorType::Customer, $customer->id);

        app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::Materials, '200.00', 'خامات');

        $this->expectException(BusinessRuleViolationException::class);
        app(CompleteWorkAction::class)->execute($order->refresh(), $provider);
    }

    /** BR-111 — الوصول البعيد يحتاج تأكيدًا صريحًا ويُسجَّل للإدارة (AC-EXE-03). */
    #[Test]
    public function الوصول_البعيد_يحذر_ثم_يُسجل_بعد_التأكيد(): void
    {
        [, $provider, , $order] = $this->publishAndAssign();
        $order = app(StartTripAction::class)->execute($order, $provider);

        try {
            app(MarkArrivedAction::class)->execute($order, $provider, 30.0700, 31.3500);
            $this->fail('كان يجب رفض الوصول البعيد بلا تأكيد.');
        } catch (BusinessRuleViolationException $e) {
            $this->assertSame('BR-111', $e->context['rule']);
        }

        $order = app(MarkArrivedAction::class)
            ->execute($order->refresh(), $provider, 30.0700, 31.3500, confirmedFarArrival: true);

        $this->assertSame(OrderStatus::Arrived, $order->status);
        $this->assertGreaterThan(500, $order->arrival_distance_m);

        $event = $order->events()->where('event_code', 'EVT-022')->sole();
        $this->assertTrue($event->meta['far_arrival']);
    }

    /** BR-053 — لا دفع جزئي. */
    #[Test]
    public function الاستلام_النقدي_الجزئي_مرفوض(): void
    {
        [$customer, $provider, , $order] = $this->publishAndAssign();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $quote = app(SubmitProposalAction::class)->execute($order, $provider, ProposalType::ExecutionQuote, '450.00', 'سبب كافٍ');
        $order = app(DecideProposalAction::class)->approve($order->refresh(), $quote, ActorType::Customer, $customer->id);
        $order = app(CompleteWorkAction::class)->execute($order, $provider);

        $this->expectException(BusinessRuleViolationException::class);
        app(ConfirmCashReceivedAction::class)->execute($order, '400.00', ActorType::Provider, $provider->id);
    }

    /** @return array{User, ProviderProfile, Admin, Order} */
    private function publishAndAssign(bool $otherProblem = false, bool $withGuide = true): array
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();

        $problemType = $category->problemTypes()->where('is_other', $otherProblem)->firstOrFail();

        if ($withGuide && ! $problemType->is_other) {
            $problemType->update([
                'employee_price_min' => '300.00',
                'employee_price_max' => '500.00',
            ]);
        }

        $order = app(PublishRequestAction::class)->execute($customer, new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $problemType->getKey(),
            timingType: TimingType::Now,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: true,
            description: 'تسريب أسفل الحوض.',
        ));

        $provider = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();

        $admin = Admin::factory()->create();
        $order = app(AssignProviderAction::class)->execute($order, $provider, $admin);

        return [$customer, $provider, $admin, $order];
    }
}
