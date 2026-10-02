<?php

declare(strict_types=1);

namespace Tests\Feature\Admin;

use App\Filament\Resources\Admins\AdminResource;
use App\Filament\Resources\Admins\Pages\EditAdmin;
use App\Filament\Resources\Customers\Pages\ViewCustomer;
use App\Filament\Resources\Disputes\Pages\ViewDispute;
use App\Filament\Resources\Providers\Pages\ViewProviderProfile;
use App\Filament\Resources\Providers\ProviderProfileResource;
use App\Modules\Identity\Enums\AdminRole;
use App\Modules\Identity\Enums\UserStatus;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Offers\Enums\OfferStatus;
use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderEvent;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderDocument;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Enums\DisputeResolution;
use App\Modules\Support\Enums\DisputeStatus;
use App\Modules\Support\Models\Dispute;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;
use Livewire\Livewire;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** الدفعة 8أ — DEC-060: مراجعة الفني، الحظر، النزاعات، مستخدمو الإدارة، المصادقة الثنائية. */
final class AdminOperationsTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
    }

    /** 15 §قائمة المراجعة: القبول بعد توثيق الهاتف، والنتيجة ACTIVE بالصفة المختارة وNTF-22. */
    #[Test]
    public function قبول_الفني_بعد_توثيق_الهاتف_فقط(): void
    {
        $admin = Admin::factory()->create();
        $profile = ProviderProfile::factory()->create(['status' => ProviderStatus::PendingReview, 'phone_verified_at' => null]);
        $this->actingAs($admin, 'admin');

        Livewire::test(ViewProviderProfile::class, ['record' => $profile->getKey()])
            ->assertActionVisible('approve')
            ->assertActionDisabled('approve')
            ->callAction('verifyPhone')
            ->callAction('approve', ['employment_type' => EmploymentType::Employee->value])
            ->assertHasNoActionErrors();

        $profile->refresh();
        $this->assertSame(ProviderStatus::Active, $profile->status);
        $this->assertSame(EmploymentType::Employee, $profile->employment_type);
        $this->assertSame($admin->getKey(), $profile->reviewed_by);
        $this->assertNotNull($profile->phone_verified_at);
        $this->assertSame(1, $profile->user->notifications()->where('type', 'NTF-22')->count());
    }

    #[Test]
    public function رفض_الفني_بسبب_إلزامي_ويصله_السبب(): void
    {
        $profile = ProviderProfile::factory()->create(['status' => ProviderStatus::PendingReview]);
        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(ViewProviderProfile::class, ['record' => $profile->getKey()])
            ->callAction('reject', ['reason' => ''])
            ->assertHasActionErrors(['reason' => 'required']);
        $this->assertSame(ProviderStatus::PendingReview, $profile->refresh()->status);

        Livewire::test(ViewProviderProfile::class, ['record' => $profile->getKey()])
            ->callAction('reject', ['reason' => 'صورة البطاقة غير واضحة'])
            ->assertHasNoActionErrors();

        $profile->refresh();
        $this->assertSame(ProviderStatus::Rejected, $profile->status);
        $this->assertSame('صورة البطاقة غير واضحة', $profile->rejection_reason);
        $this->assertStringContainsString('صورة البطاقة غير واضحة', json_encode($profile->user->notifications()->where('type', 'NTF-22')->value('data'), JSON_UNESCAPED_UNICODE));
    }

    /** BR-131: الإيقاف يغلق العروض المقدمة ويخرجه من الإتاحة، وإعادة التفعيل تعيده ACTIVE. */
    #[Test]
    public function إيقاف_الفني_يغلق_عروضه_ويعاد_تفعيله(): void
    {
        $profile = ProviderProfile::factory()->create();
        $offer = Offer::query()->create([
            'order_id' => Order::factory()->marketplace()->create()->getKey(),
            'provider_profile_id' => $profile->getKey(),
            'source' => 'PROVIDER', 'status' => OfferStatus::Submitted, 'price' => '300.00', 'submitted_at' => now(),
        ]);
        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(ViewProviderProfile::class, ['record' => $profile->getKey()])
            ->callAction('suspend', ['reason' => 'شكاوى متكررة من التأخير'])
            ->assertHasNoActionErrors();

        $profile->refresh();
        $this->assertSame(ProviderStatus::Suspended, $profile->status);
        $this->assertFalse($profile->available_now);
        $this->assertSame(OfferStatus::Closed, $offer->refresh()->status);

        Livewire::test(ViewProviderProfile::class, ['record' => $profile->getKey()])->callAction('reactivate');
        $this->assertSame(ProviderStatus::Active, $profile->refresh()->status);
    }

    /** 23، 32: مستندات الهوية داخل جلسة اللوحة فقط وبلا تخزين مؤقت. */
    #[Test]
    public function مستندات_الهوية_للإدارة_فقط(): void
    {
        Storage::fake('local');
        $profile = ProviderProfile::factory()->create(['status' => ProviderStatus::PendingReview]);
        $path = UploadedFile::fake()->image('id.jpg')->store('provider-documents', 'local');
        $document = ProviderDocument::query()->create(['provider_profile_id' => $profile->getKey(), 'type' => 'ID_FRONT', 'path' => $path]);

        $this->get(route('admin.provider-documents.show', $document))->assertRedirect(route('filament.admin.auth.login'));

        $this->actingAs(Admin::factory()->create(), 'admin')
            ->get(route('admin.provider-documents.show', $document))
            ->assertOk()
            ->assertHeader('X-Robots-Tag', 'noindex, nofollow');
        $this->get(ProviderProfileResource::getUrl('view', ['record' => $profile]))
            ->assertOk()
            ->assertSee(route('admin.provider-documents.show', $document), false);
    }

    /** BR-003، EC-18: الحظر يلغي الرموز والطلبات المفتوحة، والدخول يُرفض، ورفع الحظر يعيده. */
    #[Test]
    public function حظر_العميل_ورفعه(): void
    {
        $customer = User::factory()->create(['password' => 'Password-2026']);
        $customer->createToken('app');
        $open = Order::factory()->create(['customer_id' => $customer->getKey()]);
        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(ViewCustomer::class, ['record' => $customer->getKey()])
            ->callAction('block', ['reason' => 'إساءة متكررة للفنيين'])
            ->assertHasNoActionErrors();

        $customer->refresh();
        $this->assertSame(UserStatus::Blocked, $customer->status);
        $this->assertSame(0, $customer->tokens()->count());
        $open->refresh();
        $this->assertSame(OrderStatus::Cancelled, $open->status);
        $this->assertSame(CancelReason::AccountBlocked, $open->cancel_reason_code);
        $this->postJson('/api/v1/auth/login', ['email' => $customer->email, 'password' => 'Password-2026'])
            ->assertJsonPath('error.code', 'ACCOUNT_BLOCKED');

        Livewire::test(ViewCustomer::class, ['record' => $customer->getKey()])->callAction('unblock');
        $this->assertSame(UserStatus::Active, $customer->refresh()->status);
    }

    /** 23: مدير التشغيل يحسم بلا استرداد، والمدير العام وحده يرى نتائج الاسترداد. */
    #[Test]
    public function نتائج_النزاع_حسب_الدور(): void
    {
        $dispute = $this->dispute(OrderStatus::Disputed, postClose: false);

        $operations = ViewDispute::allowedResolutions($dispute, Admin::factory()->create());
        $super = ViewDispute::allowedResolutions($dispute, Admin::factory()->super()->create());

        $this->assertNotContains(DisputeResolution::CloseWithRefund, $operations);
        $this->assertNotContains(DisputeResolution::CancelFullRefund, $operations);
        $this->assertContains(DisputeResolution::CancelFullRefund, $super);
        $this->assertSame([DisputeResolution::CloseAsIs], ViewDispute::allowedResolutions(
            $this->dispute(OrderStatus::Closed, postClose: true), Admin::factory()->create(),
        ));
    }

    #[Test]
    public function حسم_نزاع_قبل_الإغلاق_يغلق_الطلب(): void
    {
        $dispute = $this->dispute(OrderStatus::Disputed, postClose: false);
        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(ViewDispute::class, ['record' => $dispute->getKey()])
            ->callAction('resolve', ['resolution' => DisputeResolution::CloseAsIs->value, 'note' => 'تمت مراجعة السجل والمحادثة'])
            ->assertHasNoActionErrors();

        $this->assertSame(DisputeStatus::Resolved, $dispute->refresh()->status);
        $this->assertSame(OrderStatus::Closed, $dispute->order->refresh()->status);
    }

    /** BR-120: نزاع بعد الإغلاق يُحسم بلا انتقال، ويُسجَّل EVT-081. */
    #[Test]
    public function حسم_نزاع_بعد_الإغلاق_يبقي_الطلب_مغلقا(): void
    {
        $dispute = $this->dispute(OrderStatus::Closed, postClose: true);
        $this->actingAs(Admin::factory()->create(), 'admin');

        Livewire::test(ViewDispute::class, ['record' => $dispute->getKey()])
            ->callAction('resolve', ['resolution' => DisputeResolution::CloseAsIs->value, 'note' => 'لا يوجد ما يستدعي الاسترداد'])
            ->assertHasNoActionErrors();

        $this->assertSame(DisputeStatus::Resolved, $dispute->refresh()->status);
        $this->assertSame(OrderStatus::Closed, $dispute->order->refresh()->status);
        $this->assertTrue(OrderEvent::query()->where('order_id', $dispute->order_id)->where('event_code', 'EVT-081')->exists());
    }

    /** 23: مستخدمو الإدارة للمدير العام وحده، ولا يُترك النظام بلا مدير عام نشط. */
    #[Test]
    public function مستخدمو_الإدارة_للمدير_العام_مع_الحماية(): void
    {
        $this->actingAs(Admin::factory()->create(), 'admin')
            ->get(AdminResource::getUrl('index'))
            ->assertForbidden();

        $this->flushSession();
        $super = Admin::factory()->super()->create();
        $this->actingAs($super, 'admin')->get(AdminResource::getUrl('index'))->assertOk();

        Livewire::test(EditAdmin::class, ['record' => $super->getKey()])
            ->fillForm(['is_active' => false])
            ->call('save')
            ->assertHasErrors();
        $this->assertTrue($super->refresh()->is_active);

        $other = Admin::factory()->create(['role' => AdminRole::Operations]);
        Livewire::test(EditAdmin::class, ['record' => $other->getKey()])
            ->fillForm(['is_active' => false])
            ->call('save')
            ->assertHasNoErrors();
        $this->assertFalse($other->refresh()->is_active);
    }

    /** ASM-14: حساب بلا مصادقة ثنائية يُحوَّل لإعدادها قبل أي صفحة. */
    #[Test]
    public function المصادقة_الثنائية_إلزامية(): void
    {
        $this->actingAs(Admin::factory()->withoutTwoFactor()->create(), 'admin')
            ->get('/admin')
            ->assertRedirectContains('multi-factor-authentication');
    }

    private function dispute(OrderStatus $status, bool $postClose): Dispute
    {
        $order = Order::factory()->status($status)->create();

        return Dispute::query()->create([
            'order_id' => $order->getKey(), 'opened_by_type' => 'CUSTOMER', 'opened_by_id' => $order->customer_id,
            'reason_code' => 'QUALITY', 'description' => 'التنفيذ لم يكتمل كما اتفقنا.',
            'is_post_close' => $postClose, 'status' => DisputeStatus::Open,
        ]);
    }
}
