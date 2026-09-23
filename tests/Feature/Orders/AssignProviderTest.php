<?php

declare(strict_types=1);

namespace Tests\Feature\Orders;

use App\Modules\Identity\Models\Admin;
use App\Modules\Offers\Enums\OfferSource;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Orders\Actions\AssignProviderAction;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\OrderEventCode;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\StateMachine\OrderStateMachine;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\FeatureDisabledException;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** T-27 / T-29 — BR-007، AC-PH1-02، AC-PH1-03، AC-PH1-04. */
final class AssignProviderTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(SettingsSeeder::class);
    }

    /** AC-PH1-03 */
    #[Test]
    public function التعيين_يؤكد_الطلب_وينشئ_صف_تعيين_بسعر_صفر(): void
    {
        [$order, $provider, $admin] = $this->scenario();

        $order = app(AssignProviderAction::class)->execute($order, $provider, $admin);

        $this->assertSame(OrderStatus::Confirmed, $order->status);
        $this->assertSame($provider->getKey(), $order->provider_profile_id);
        $this->assertSame($admin->getKey(), $order->assigned_by_admin_id);
        $this->assertNotNull($order->assigned_at);

        // BR-065 — الموظف بأجر: كامل المصنعية للمنصة
        $this->assertSame('1.0000', $order->commission_rate);

        $assignment = $order->refresh()->acceptedOffer;
        $this->assertNotNull($assignment);
        $this->assertSame(OfferSource::AdminAssignment, $assignment->source);
        $this->assertSame(OfferStatus::Accepted, $assignment->status);
        $this->assertSame('0.00', $assignment->price);

        // BR-007 — لا يظهر صف التعيين كعرض لأي طرف
        $this->assertSame(0, $order->offers()->count());

        // EVT-011 باسم المسؤول (AC-ADM-03)
        $event = $order->events()->where('event_code', OrderEventCode::ProviderAssigned->value)->sole();
        $this->assertSame(ActorType::Admin, $event->actor_type);
        $this->assertSame($admin->getKey(), $event->actor_id);
        $this->assertSame('T-27', $event->meta['transition']);
    }

    /** AC-PH1-02 — الإجراء المعطّل يُرفض قبل أي كتابة. */
    #[Test]
    public function التعيين_مرفوض_في_وضع_السوق(): void
    {
        [$order, $provider, $admin] = $this->scenario();
        $this->setOperatingFlags(offersEnabled: true);

        $this->expectException(FeatureDisabledException::class);

        try {
            app(AssignProviderAction::class)->execute($order, $provider, $admin);
        } finally {
            $this->assertSame(OrderStatus::Open, $order->refresh()->status);
        }
    }

    /** AC-PH1-04 — مقدم خدمة لديه طلب NOW نشط لا يُعيَّن (نفس شروط BR-034). */
    #[Test]
    public function التعيين_مرفوض_لمقدم_خدمة_مشغول_بطلب_الآن(): void
    {
        [$order, $provider, $admin] = $this->scenario();
        app(AssignProviderAction::class)->execute($order, $provider, $admin);

        $second = Order::factory()->create();

        $this->expectException(BusinessRuleViolationException::class);
        app(AssignProviderAction::class)->execute($second, $provider, $admin);
    }

    #[Test]
    public function التعيين_مرفوض_لمقدم_خدمة_خارج_المنطقة_أو_الفئة(): void
    {
        [$order, , $admin] = $this->scenario();
        $outsider = ProviderProfile::factory()->create(); // بلا فئة ولا منطقة

        $this->expectException(BusinessRuleViolationException::class);
        app(AssignProviderAction::class)->execute($order, $outsider, $admin);
    }

    /** T-29 */
    #[Test]
    public function إعادة_التعيين_تسحب_الأول_وتثبت_البديل(): void
    {
        [$order, $provider, $admin] = $this->scenario();
        $order = app(AssignProviderAction::class)->execute($order, $provider, $admin);

        $substitute = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();

        $order = app(AssignProviderAction::class)->reassign($order, $substitute, $admin, 'الموظف الأول في طلب متأخر');

        $this->assertSame(OrderStatus::Confirmed, $order->status);
        $this->assertSame($substitute->getKey(), $order->provider_profile_id);
        $this->assertSame(1, $order->reopen_count);

        $backedOut = $order->allOfferRows()
            ->where('provider_profile_id', $provider->getKey())
            ->sole();
        $this->assertSame(OfferStatus::BackedOut, $backedOut->status);
    }

    #[Test]
    public function آلة_الحالات_ترفض_قبول_عرض_في_وضع_الموظفين(): void
    {
        [$order] = $this->scenario();

        $this->expectException(FeatureDisabledException::class);

        app(OrderStateMachine::class)->apply(
            order: $order,
            action: 'acceptOffer',
            actorType: ActorType::Customer,
            actorId: $order->customer_id,
        );
    }

    /** @return array{Order, ProviderProfile, Admin} */
    private function scenario(): array
    {
        $order = Order::factory()->create();

        $provider = ProviderProfile::factory()
            ->servingFor($order->category_id, $order->area_id)
            ->create();

        return [$order, $provider, Admin::factory()->create()];
    }
}
