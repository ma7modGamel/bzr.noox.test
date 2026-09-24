<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\OperatingMode;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\EmailNotVerifiedException;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** T-01 — 07، BR-008، BR-009، BR-013..BR-019، AC-REQ، AC-PH1-01. */
final class PublishRequestTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        // داخل ساعات الخدمة 08:00–22:00 بتوقيت القاهرة (CFG-020)
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    /** AC-PH1-01 */
    #[Test]
    public function النشر_في_وضع_الموظفين_يثبت_الوضع_ويجعل_الطلب_معاينة_بلا_نافذة_عروض(): void
    {
        [$customer, $data] = $this->scenario();

        $order = app(PublishRequestAction::class)->execute($customer, $data);

        $this->assertSame(OrderStatus::Open, $order->status);
        $this->assertSame(OperatingMode::Employee, $order->operating_mode);
        $this->assertSame(PricingMode::Inspection, $order->pricing_mode);
        $this->assertNull($order->offers_close_at);
        $this->assertNull($order->budget_amount);

        // CFG-093: مهلة التعيين لطلب الآن = 60 دقيقة من النشر
        $this->assertSame(60, (int) CarbonImmutable::now()->diffInMinutes($order->selection_deadline_at));

        $event = $order->events()->where('event_code', OrderEventCode::Published->value)->sole();
        $this->assertSame('T-01', $event->meta['transition']);
        $this->assertSame(0, $event->meta['notified_providers']);
        $this->assertSame(1, $order->terms_version);
    }

    #[Test]
    public function النشر_في_وضع_السوق_يحسب_نافذة_العروض_ومهلة_الاختيار(): void
    {
        $this->setOperatingFlags(offersEnabled: true);
        [$customer, $data] = $this->scenario(pricingMode: PricingMode::Execution, budget: '300.00');

        $order = app(PublishRequestAction::class)->execute($customer, $data);

        $this->assertSame(OperatingMode::Marketplace, $order->operating_mode);
        $this->assertSame(PricingMode::Execution, $order->pricing_mode);
        $this->assertSame('300.00', $order->budget_amount);

        // CFG-010 = 30 دقيقة، CFG-013 = نهاية النافذة + 15
        $this->assertSame(30, (int) CarbonImmutable::now()->diffInMinutes($order->offers_close_at));
        $this->assertSame(45, (int) CarbonImmutable::now()->diffInMinutes($order->selection_deadline_at));
    }

    /** BR-009 — الوضع مثبت: تبديل المفتاح بعد النشر لا يمس الطلب القائم. */
    #[Test]
    public function تبديل_المفتاح_بعد_النشر_لا_يغير_وضع_الطلب_القائم(): void
    {
        [$customer, $data] = $this->scenario();
        $order = app(PublishRequestAction::class)->execute($customer, $data);

        $this->setOperatingFlags(offersEnabled: true);

        $this->assertSame(OperatingMode::Employee, $order->refresh()->operating_mode);
    }

    /** BR-013 */
    #[Test]
    public function مشكلة_أخرى_تتطلب_وصفًا(): void
    {
        $other = ProblemType::query()->where('is_other', true)->firstOrFail();
        [$customer, $data] = $this->scenario(problemTypeId: $other->id, description: 'قصير');

        $this->expectException(BusinessRuleViolationException::class);
        app(PublishRequestAction::class)->execute($customer, $data);
    }

    /** BR-019 */
    #[Test]
    public function حد_الطلبات_المفتوحة_يمنع_الرابع(): void
    {
        [$customer, $data] = $this->scenario();
        $action = app(PublishRequestAction::class);

        for ($i = 0; $i < 3; $i++) {
            $action->execute($customer, $data);
        }

        $this->expectException(BusinessRuleViolationException::class);
        $action->execute($customer, $data);
    }

    /** BR-001 / AC-ACC-01 — رمز مستقل في العقد ليفتح التطبيق شاشة التفعيل. */
    #[Test]
    public function البريد_غير_الموثق_يمنع_النشر(): void
    {
        [$customer, $data] = $this->scenario();
        $customer->forceFill(['email_verified_at' => null])->save();

        try {
            app(PublishRequestAction::class)->execute($customer->refresh(), $data);
            $this->fail('كان يجب رفض النشر ببريد غير موثق.');
        } catch (EmailNotVerifiedException $e) {
            $this->assertSame('EMAIL_NOT_VERIFIED', $e->errorCode());
            $this->assertSame(403, $e->httpStatus());
        }
    }

    /** BR-017 — خارج ساعات الخدمة لا يُقبل طلب "الآن". */
    #[Test]
    public function طلب_الآن_مرفوض_خارج_ساعات_الخدمة(): void
    {
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 23:30', 'Africa/Cairo')->utc());
        [$customer, $data] = $this->scenario();

        $this->expectException(BusinessRuleViolationException::class);
        app(PublishRequestAction::class)->execute($customer, $data);
    }

    /** BR-017 — الفترة المجدولة يجب أن تكون من فترات CFG-021 وبعد CFG-022. */
    #[Test]
    public function الفترة_المجدولة_تلتزم_بفترات_الإعداد(): void
    {
        [$customer] = $this->scenario();
        $address = CustomerAddress::query()->where('user_id', $customer->id)->sole();
        $category = Category::query()->where('name', 'سباكة')->sole();
        $problemType = $category->problemTypes()->where('is_other', false)->first();

        $valid = new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $problemType->id,
            timingType: TimingType::Scheduled,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: true,
            slotStart: CarbonImmutable::parse('2026-10-05 15:00', 'Africa/Cairo')->utc(),
        );

        $order = app(PublishRequestAction::class)->execute($customer, $valid);
        $this->assertNotNull($order->slot_start);
        $this->assertNotNull($order->slot_end);
        // وضع الموظفين: مهلة التعيين = بداية الفترة (CFG-093)
        $this->assertTrue($order->selection_deadline_at->equalTo($order->slot_start));

        $invalid = new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $problemType->id,
            timingType: TimingType::Scheduled,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: true,
            slotStart: CarbonImmutable::parse('2026-10-05 15:30', 'Africa/Cairo')->utc(), // ليست بداية فترة
        );

        $this->expectException(BusinessRuleViolationException::class);
        app(PublishRequestAction::class)->execute($customer, $invalid);
    }

    /** BR-018 */
    #[Test]
    public function عدم_الموافقة_على_الشروط_يمنع_النشر(): void
    {
        [$customer, $data] = $this->scenario(termsAccepted: false);

        $this->expectException(BusinessRuleViolationException::class);
        app(PublishRequestAction::class)->execute($customer, $data);
    }

    /** BR-020 — تحذير غير مانع. */
    #[Test]
    public function تكرار_نفس_الفئة_والعنوان_يعطي_تحذيرًا_لا_منعًا(): void
    {
        [$customer, $data] = $this->scenario();
        $action = app(PublishRequestAction::class);

        $this->assertFalse($action->duplicateWarning($customer, $data));

        $action->execute($customer, $data);

        $this->assertTrue($action->duplicateWarning($customer, $data));
        $this->assertInstanceOf(Order::class, $action->execute($customer, $data));
    }

    /** @return array{User, PublishRequestData} */
    private function scenario(
        ?int $problemTypeId = null,
        ?string $description = 'تسريب أسفل الحوض منذ يومين.',
        ?PricingMode $pricingMode = null,
        ?string $budget = null,
        bool $termsAccepted = true,
    ): array {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();

        $problemTypeId ??= $category->problemTypes()->where('is_other', false)->value('id');

        return [$customer, new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $problemTypeId,
            timingType: TimingType::Now,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: $termsAccepted,
            description: $description,
            pricingMode: $pricingMode,
            budgetAmount: $budget,
        )];
    }
}
