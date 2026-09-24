<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Communication\Enums\ConversationStatus;
use App\Modules\Communication\Models\Conversation;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Actions\BackOutAction;
use App\Modules\Orders\Actions\CancelOrderAction;
use App\Modules\Orders\Actions\MarkArrivedAction;
use App\Modules\Orders\Actions\PublishRequestAction;
use App\Modules\Orders\Actions\ReportUnableAction;
use App\Modules\Orders\Actions\StartTripAction;
use App\Modules\Orders\Data\PublishRequestData;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderPresentation;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Actions\OpenDisputeAction;
use App\Modules\Support\Actions\ResolveDisputeAction;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\TermsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** 13-CANCELLATION-FLOW و BR-120 — AC-CAN، AC-DSP، AC-EXE-04، AC-EXE-05. */
final class CancellationAndDisputeTest extends TestCase
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

    /** AC-CAN-01 — الإلغاء في "في الطريق" مجاني وبلا أي مبلغ. */
    #[Test]
    public function العميل_يلغي_مجانًا_حتى_في_الطريق(): void
    {
        [$customer, $provider, , $order] = $this->assigned();
        $order = app(StartTripAction::class)->execute($order, $provider);

        $conversation = Conversation::query()
            ->where('order_id', $order->id)
            ->where('provider_profile_id', $provider->id)
            ->sole();

        $order = app(CancelOrderAction::class)->execute(
            $order, ActorType::Customer, $customer->id, CancelReason::NoLongerNeeded, 'تم حل المشكلة',
        );

        $this->assertSame(OrderStatus::Cancelled, $order->status);
        $this->assertNull($order->final_amount);
        $this->assertSame(CancelReason::NoLongerNeeded, $order->cancel_reason_code);
        $this->assertSame(ConversationStatus::ReadOnly, $conversation->refresh()->status);

        $this->actingAs($customer, 'sanctum')
            ->getJson("/api/v1/orders/{$order->id}")
            ->assertOk()
            ->assertJsonPath('data.termination.reason_label', CancelReason::NoLongerNeeded->getLabel())
            ->assertJsonPath('data.termination.note', 'تم حل المشكلة');
    }

    /** BR-070 / AC-EXE-04 — لا إلغاء للعميل بعد الوصول. */
    #[Test]
    public function العميل_لا_يلغي_بعد_الوصول(): void
    {
        [$customer, $provider, , $order] = $this->assigned();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $this->expectException(BusinessRuleViolationException::class);
        app(CancelOrderAction::class)->execute(
            $order, ActorType::Customer, $customer->id, CancelReason::NoLongerNeeded,
        );
    }

    /** BR-036 — الاعتذار يعيد الطلب لانتظار التعيين بمهلة جديدة، والعرض ← BACKED_OUT. */
    #[Test]
    public function اعتذار_الفني_يعيد_الطلب_لانتظار_التعيين(): void
    {
        [, $provider, , $order] = $this->assigned();

        $order = app(BackOutAction::class)->execute(
            $order, ActorType::Provider, $provider->id, CancelReason::Emergency, $provider,
        );

        $this->assertSame(OrderStatus::Open, $order->status);
        $this->assertNull($order->provider_profile_id);
        $this->assertNull($order->accepted_offer_id);
        $this->assertNull($order->assigned_at);
        $this->assertSame(1, $order->reopen_count);
        $this->assertNull($order->offers_close_at); // وضع الموظفين

        $backedOut = $order->allOfferRows()->where('provider_profile_id', $provider->id)->sole();
        $this->assertSame(OfferStatus::BackedOut, $backedOut->status);
    }

    /** الطلب بعد الاعتذار يقبل تعيينًا جديدًا. */
    #[Test]
    public function الطلب_بعد_الاعتذار_يقبل_تعيينًا_جديدًا(): void
    {
        [, $provider, $admin, $order] = $this->assigned();
        $order = app(BackOutAction::class)->execute($order, ActorType::Provider, $provider->id, CancelReason::Emergency, $provider);

        $substitute = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $order = app(AssignProviderAction::class)->execute($order, $substitute, $admin);

        $this->assertSame(OrderStatus::Confirmed, $order->status);
        $this->assertSame($substitute->id, $order->provider_profile_id);
    }

    /** AC-EXE-05 — غياب العميل يُسجَّل بعد CFG-033 فقط. */
    #[Test]
    public function غياب_العميل_يُسجل_بعد_المهلة_فقط(): void
    {
        [, $provider, , $order] = $this->assigned();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        try {
            app(ReportUnableAction::class)->customerNoShow($order, $provider);
            $this->fail('كان يجب الرفض قبل مرور 20 دقيقة.');
        } catch (BusinessRuleViolationException $e) {
            $this->assertSame('BR-071', $e->context['rule']);
        }

        CarbonImmutable::setTestNow(CarbonImmutable::now()->addMinutes(21));

        $order = app(ReportUnableAction::class)->customerNoShow($order->refresh(), $provider);

        $this->assertSame(OrderStatus::Cancelled, $order->status);
        $this->assertSame(CancelReason::CustomerNoShow, $order->cancel_reason_code);
        $this->assertNull($order->final_amount);
    }

    /** AC-DSP-01 — فتح مشكلة ينقل الطلب إلى DISPUTED. */
    #[Test]
    public function فتح_مشكلة_ينقل_الطلب_لقيد_المراجعة(): void
    {
        [$customer, $provider, , $order] = $this->assigned();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $dispute = app(OpenDisputeAction::class)->execute(
            $order, ActorType::Customer, $customer->id, 'QUALITY', 'الفني لم يبدأ العمل',
        );

        $this->assertSame(OrderStatus::Disputed, $order->refresh()->status);
        $this->assertSame(OrderStatus::Arrived, $order->refresh()->disputed_from_status);
        $this->assertSame('on_hold', app(OrderPresentation::class)->stepper($order->refresh())[2]['state']);
        $this->assertSame(DisputeStatus::Open, $dispute->status);
        $this->assertFalse($dispute->is_post_close);
    }

    /** T-23 — قرار الإدارة يغلق الطلب ويُسجَّل باسم المسؤول. */
    #[Test]
    public function قرار_الإدارة_يغلق_النزاع_والطلب(): void
    {
        [$customer, $provider, $admin, $order] = $this->assigned();
        $order = app(StartTripAction::class)->execute($order, $provider);
        $order = app(MarkArrivedAction::class)->execute($order, $provider, 30.0600, 31.3400);

        $dispute = app(OpenDisputeAction::class)->execute(
            $order, ActorType::Customer, $customer->id, 'QUALITY', 'الفني لم يبدأ العمل',
        );

        $order = app(ResolveDisputeAction::class)->execute(
            $order->refresh(), $dispute, $admin, DisputeResolution::CloseWithoutPayment, 'زيارة بلا تنفيذ — بلا مبلغ',
        );

        $this->assertSame(OrderStatus::Closed, $order->status);
        $this->assertSame('0.00', $order->commission_amount);
        $this->assertSame(DisputeStatus::Resolved, $dispute->refresh()->status);
        $this->assertSame($admin->id, $dispute->resolved_by);

        $event = $order->events()->where('event_code', 'EVT-081')->sole();
        $this->assertSame(ActorType::Admin, $event->actor_type);
    }

    /** @return array{User, ProviderProfile, Admin, Order} */
    private function assigned(): array
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
        ));

        $provider = ProviderProfile::factory()->servingFor($order->category_id, $order->area_id)->create();
        $admin = Admin::factory()->create();

        return [$customer, $provider, $admin, app(AssignProviderAction::class)->execute($order, $provider, $admin)];
    }
}
