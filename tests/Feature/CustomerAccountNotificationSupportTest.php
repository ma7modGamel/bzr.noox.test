<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Mail\SupportMessageMail;
use App\Modules\Content\Actions\PublishLegalPageVersionAction;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use Database\Seeders\LegalPagesSeeder;
use Illuminate\Bus\Queueable;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Notifications\Notification;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Mail;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class CustomerAccountNotificationSupportTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function المساعدة_تعيد_الأسئلة_من_الخادم_وترسل_رسالة_الدعم_في_الطابور(): void
    {
        Mail::fake();
        $user = User::factory()->create();

        $this->getJson('/api/v1/support/faqs')
            ->assertOk()
            ->assertJsonPath('data.0.code', 'REQUEST_STATUS')
            ->assertJsonStructure(['data' => [['code', 'title', 'body']]]);

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/support/messages', [
                'subject' => 'مساعدة في طلب',
                'message' => 'أحتاج إلى مساعدة في متابعة الطلب الحالي.',
            ])
            ->assertAccepted();

        Mail::assertQueued(SupportMessageMail::class, function (SupportMessageMail $mail) use ($user): bool {
            return $mail->userId === $user->getKey() && $mail->hasTo(config('mail.support_address'));
        });
    }

    #[Test]
    public function الإشعارات_تعاد_للصاحب_فقط_ويمكن_تعليم_المحدد_كمقروء(): void
    {
        $user = User::factory()->create();
        $other = User::factory()->create();
        $user->notify(new CustomerDatabaseNotification('NTF-09', 'وصل الفني', 'الفني عند موقع الخدمة.', 'bzr://orders/44'));
        $other->notify(new CustomerDatabaseNotification('NTF-16', 'اكتمل الطلب', 'اكتملت الخدمة.', 'bzr://orders/77'));
        $notificationId = $user->notifications()->value('id');

        $this->actingAs($user, 'sanctum')
            ->getJson('/api/v1/notifications')
            ->assertOk()
            ->assertJsonCount(1, 'data')
            ->assertJsonPath('data.0.code', 'NTF-09')
            ->assertJsonPath('meta.unread_count', 1)
            ->assertJsonMissing(['code' => 'NTF-16']);

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/notifications/read', ['notification_ids' => [$notificationId]])
            ->assertNoContent();

        $this->assertNotNull($user->notifications()->findOrFail($notificationId)->read_at);
    }

    #[Test]
    public function الحساب_يحدّث_البيانات_وكلمة_المرور_بعد_التحقق_من_الحالية(): void
    {
        $user = User::factory()->create(['password' => 'old-password']);

        $this->actingAs($user, 'sanctum')
            ->patchJson('/api/v1/me', ['name' => 'اسم جديد', 'phone' => '01012345678'])
            ->assertOk()
            ->assertJsonPath('user.name', 'اسم جديد')
            ->assertJsonPath('user.phone', '01012345678');

        $this->actingAs($user, 'sanctum')
            ->postJson('/api/v1/me/password', [
                'current_password' => 'old-password',
                'password' => 'new-password',
                'password_confirmation' => 'new-password',
            ])
            ->assertNoContent();

        $this->assertTrue(Hash::check('new-password', $user->fresh()->password));
    }

    #[Test]
    public function حذف_الحساب_يرفض_مع_طلب_نشط_ثم_يخفي_البيانات_مع_السجل_النهائي(): void
    {
        $user = User::factory()->create();
        $address = CustomerAddress::factory()->create(['user_id' => $user->id]);
        $order = Order::factory()->status(OrderStatus::Open)->create(['customer_id' => $user->id]);

        $this->actingAs($user, 'sanctum')
            ->deleteJson('/api/v1/me')
            ->assertConflict()
            ->assertJsonPath('error.code', 'ACTIVE_ORDER_EXISTS');

        $order->forceFill(['status' => OrderStatus::Closed])->save();

        $this->actingAs($user, 'sanctum')
            ->deleteJson('/api/v1/me')
            ->assertNoContent();

        $deleted = User::withTrashed()->findOrFail($user->id);
        $this->assertSame('حساب محذوف', $deleted->name);
        $this->assertStringContainsString('@invalid.local', $deleted->email);
        $this->assertSoftDeleted($deleted);
        $this->assertSoftDeleted($address);
        $this->assertModelExists($order);
    }

    #[Test]
    public function الشروط_تعيد_آخر_نسخة_من_الخادم(): void
    {
        $this->seed(LegalPagesSeeder::class);
        $terms = LegalPage::query()->where('slug', 'terms')->sole()->versions()->sole();
        app(PublishLegalPageVersionAction::class)->execute($terms, Admin::factory()->super()->create());

        $this->getJson('/api/v1/terms/current')
            ->assertOk()
            ->assertJsonPath('data.version', 1)
            ->assertJsonStructure(['data' => ['version', 'body', 'effective_at']]);
    }
}

final class CustomerDatabaseNotification extends Notification
{
    use Queueable;

    public function __construct(
        private readonly string $code,
        private readonly string $title,
        private readonly string $body,
        private readonly string $deepLink,
    ) {}

    /** @return list<string> */
    public function via(object $notifiable): array
    {
        return ['database'];
    }

    /** @return array{code: string, title: string, body: string, deep_link: string} */
    public function toDatabase(object $notifiable): array
    {
        return [
            'code' => $this->code,
            'title' => $this->title,
            'body' => $this->body,
            'deep_link' => $this->deepLink,
        ];
    }
}
