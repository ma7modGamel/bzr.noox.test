<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Actions\AcceptOfferAction;
use App\Modules\Offers\Actions\SubmitOfferAction;
use App\Modules\Offers\Actions\WithdrawOfferAction;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\FeatureDisabledException;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/**
 * O-01..O-03 — 09، BR-030..BR-036. AC-OFR، AC-CNF.
 * مسار مبني بالكامل ومعطّل في المرحلة الأولى؛ يُختبر بـ CFG-090 مفعّلًا (37 §المخاطر).
 */
final class MarketplaceOffersTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class, TermsSeeder::class]);
        CarbonImmutable::setTestNow(CarbonImmutable::parse('2026-10-05 09:00', 'Africa/Cairo')->utc());

        $this->setOperatingFlags(offersEnabled: true);
        // OD-01 — وضع السوق لا يعمل بلا نسبة عمولة
        app(SettingsRepository::class)->set(Cfg::DefaultCommissionRate, '0.1000');
    }

    protected function tearDown(): void
    {
        CarbonImmutable::setTestNow();
        parent::tearDown();
    }

    /** AC-OFR-01 */
    #[Test]
    public function الفني_المؤهل_يقدم_عرضًا(): void
    {
        [$order, $provider] = $this->orderWithProvider();

        $offer = app(SubmitOfferAction::class)->execute($order, $provider, '350.00', etaMinutes: 25);

        $this->assertSame(OfferStatus::Submitted, $offer->status);
        $this->assertSame('350.00', $offer->price);
        $this->assertSame(25, $offer->eta_minutes);
        $this->assertSame('315.00', app(SubmitOfferAction::class)->netAmount('350.00')); // صافي بعد 10%
    }

    /** AC-OFR-02 — عرض نشط واحد. */
    #[Test]
    public function عرض_ثانٍ_مع_وجود_عرض_نشط_مرفوض(): void
    {
        [$order, $provider] = $this->orderWithProvider();
        app(SubmitOfferAction::class)->execute($order, $provider, '350.00', etaMinutes: 25);

        $this->expectException(BusinessRuleViolationException::class);
        app(SubmitOfferAction::class)->execute($order->refresh(), $provider, '300.00', etaMinutes: 20);
    }

    /** AC-OFR-03 — إعادة التقديم مرة واحدة فقط. */
    #[Test]
    public function إعادة_التقديم_مرة_واحدة_فقط(): void
    {
        [$order, $provider] = $this->orderWithProvider();

        $first = app(SubmitOfferAction::class)->execute($order, $provider, '350.00', etaMinutes: 25);
        app(WithdrawOfferAction::class)->execute($first, $provider);

        $second = app(SubmitOfferAction::class)->execute($order->refresh(), $provider, '320.00', etaMinutes: 25);
        $this->assertSame($first->id, $second->previous_offer_id);

        app(WithdrawOfferAction::class)->execute($second, $provider);

        $this->expectException(BusinessRuleViolationException::class);
        app(SubmitOfferAction::class)->execute($order->refresh(), $provider, '300.00', etaMinutes: 25);
    }

    /** BR-032 — حجب الأرقام من ملاحظة العرض. */
    #[Test]
    public function ملاحظة_العرض_تُحجب_منها_الأرقام(): void
    {
        [$order, $provider] = $this->orderWithProvider();

        $offer = app(SubmitOfferAction::class)->execute(
            $order, $provider, '350.00', etaMinutes: 25, note: 'كلمني على 01012345678',
        );

        $this->assertStringNotContainsString('01012345678', (string) $offer->note);
        $this->assertStringContainsString('•••', (string) $offer->note);
    }

    /** AC-OFR-07 — خصم رسوم المعاينة إلزامي في طلب المعاينة. */
    #[Test]
    public function طلب_المعاينة_يتطلب_تحديد_الخصم(): void
    {
        [$order, $provider] = $this->orderWithProvider(PricingMode::Inspection);

        $this->expectException(BusinessRuleViolationException::class);
        app(SubmitOfferAction::class)->execute($order, $provider, '100.00', etaMinutes: 25);
    }

    /** AC-CNF-01 — القبول يؤكد الطلب ويثبت العمولة ويحول الباقي إلى NOT_SELECTED. */
    #[Test]
    public function قبول_العرض_يؤكد_الطلب_ويثبت_العمولة(): void
    {
        [$order, $provider] = $this->orderWithProvider();
        $other = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();

        $chosen = app(SubmitOfferAction::class)->execute($order, $provider, '350.00', etaMinutes: 25);
        $rejected = app(SubmitOfferAction::class)->execute($order->refresh(), $other, '400.00', etaMinutes: 30);

        $order = app(AcceptOfferAction::class)
            ->execute($order->refresh(), $chosen, $order->customer_id, PaymentMethod::Cash);

        $this->assertSame(OrderStatus::Confirmed, $order->status);
        $this->assertSame($provider->id, $order->provider_profile_id);
        $this->assertSame('0.1000', $order->commission_rate);
        $this->assertSame(OfferStatus::Accepted, $chosen->refresh()->status);
        $this->assertSame(OfferStatus::NotSelected, $rejected->refresh()->status);
    }

    /**
     * AC-CNF-02 — الفني يقدّم على طلبين وهو متاح، ثم يُختار في الأول فيصير مشغولًا:
     * قبول عرضه في الثاني يُرفض (BR-034) ويُطلب اختيار عرض آخر.
     */
    #[Test]
    public function قبول_عرض_لفني_صار_مشغولًا_بطلب_الآن_مرفوض(): void
    {
        [$first, $provider] = $this->orderWithProvider();
        [$second] = $this->orderWithProvider();

        // العرضان يُقدَّمان والفني ما زال متاحًا
        $offer1 = app(SubmitOfferAction::class)->execute($first, $provider, '350.00', etaMinutes: 25);
        $offer2 = app(SubmitOfferAction::class)->execute($second, $provider, '300.00', etaMinutes: 20);

        app(AcceptOfferAction::class)->execute($first->refresh(), $offer1, $first->customer_id, PaymentMethod::Cash);

        $this->expectException(BusinessRuleViolationException::class);
        app(AcceptOfferAction::class)->execute($second->refresh(), $offer2, $second->customer_id, PaymentMethod::Cash);
    }

    /** BR-022 §6 — الفني المشغول بطلب NOW نشط لا يظهر له طلب NOW جديد أصلًا. */
    #[Test]
    public function الفني_المشغول_بطلب_الآن_لا_يقدم_عرضًا_جديدًا(): void
    {
        [$first, $provider] = $this->orderWithProvider();
        $offer = app(SubmitOfferAction::class)->execute($first, $provider, '350.00', etaMinutes: 25);
        app(AcceptOfferAction::class)->execute($first->refresh(), $offer, $first->customer_id, PaymentMethod::Cash);

        [$second] = $this->orderWithProvider();

        $this->expectException(BusinessRuleViolationException::class);
        app(SubmitOfferAction::class)->execute($second, $provider, '300.00', etaMinutes: 20);
    }

    /** AC-PH1-02 — نفس الإجراءات مرفوضة تمامًا في وضع الموظفين. */
    #[Test]
    public function تقديم_العرض_مرفوض_عند_تعطيل_المفتاح(): void
    {
        [$order, $provider] = $this->orderWithProvider();
        $this->setOperatingFlags(offersEnabled: false);

        $this->expectException(FeatureDisabledException::class);
        app(SubmitOfferAction::class)->execute($order, $provider, '350.00', etaMinutes: 25);
    }

    /** @return array{Order, ProviderProfile} */
    private function orderWithProvider(PricingMode $pricingMode = PricingMode::Execution): array
    {
        $customer = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $customer->id]);
        $category = Category::query()->where('name', 'سباكة')->sole();

        $order = app(PublishRequestAction::class)->execute($customer, new PublishRequestData(
            customerAddressId: $address->id,
            categoryId: $category->id,
            problemTypeId: $category->problemTypes()->where('is_other', false)->value('id'),
            timingType: TimingType::Now,
            materialsResponsibility: MaterialsResponsibility::Unsure,
            termsAccepted: true,
            description: 'تسريب أسفل الحوض.',
            pricingMode: $pricingMode,
        ));

        return [$order, ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create()];
    }
}
