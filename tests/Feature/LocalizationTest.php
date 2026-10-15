<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Catalog\Models\Category;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Identity\Models\User;
use App\Modules\Notifications\Contracts\PushSender;
use App\Modules\Notifications\Data\LocalizedMessage;
use App\Modules\Notifications\Data\NotificationContent;
use App\Modules\Notifications\Jobs\SendPushNotification;
use App\Modules\Notifications\Models\DeviceToken;
use App\Modules\Notifications\Push\PushMessage;
use App\Modules\Notifications\Push\PushResult;
use App\Modules\Notifications\Services\OrderNotifications;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Localization;
use App\Support\SafeHtml;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Database\Seeders\SupportReasonSeeder;
use Illuminate\Auth\Notifications\ResetPassword;
use Illuminate\Auth\Notifications\VerifyEmail;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Arr;
use Illuminate\Support\Facades\Queue;
use PHPUnit\Framework\Attributes\Test;
use PHPUnit\Framework\Attributes\TestWith;
use Tests\TestCase;

final class LocalizationTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function config_switches_all_options_without_changing_machine_codes(): void
    {
        $this->seed([SettingsSeeder::class, SupportReasonSeeder::class]);

        $english = $this->getJson('/api/v1/config', ['Accept-Language' => 'en-GB,en;q=0.9,ar;q=0.5'])
            ->assertOk()->assertHeader('Content-Language', 'en')
            ->assertJsonPath('option_lists.materials_responsibilities.0.label', 'I have the materials')
            ->assertJsonPath('option_lists.materials_responsibilities.0.code', 'CUSTOMER_HAS');
        $this->assertDoesNotMatchRegularExpression('/\p{Arabic}/u', json_encode($english->json('option_lists'), JSON_UNESCAPED_UNICODE));

        $this->getJson('/api/v1/config', ['Accept-Language' => 'ar-EG'])
            ->assertOk()->assertHeader('Content-Language', 'ar')
            ->assertJsonPath('option_lists.materials_responsibilities.0.label', 'عندي الخامات');
    }

    #[Test]
    public function catalog_uses_admin_translation_and_fails_explicitly_when_missing(): void
    {
        $category = Category::query()->create(['name' => 'خدمة تجريبية', 'name_en' => 'Test service', 'is_active' => true]);

        $this->getJson('/api/v1/catalog', ['Accept-Language' => 'en'])
            ->assertOk()->assertJsonPath('data.0.name', 'Test service');

        $category->update(['name_en' => null]);

        $this->getJson('/api/v1/catalog', ['Accept-Language' => 'en'])
            ->assertStatus(409)->assertJsonPath('error.code', 'LOCALIZATION_UNAVAILABLE')
            ->assertJsonPath('error.message', 'This content is not available in this language yet.');
        $this->getJson('/api/v1/catalog', ['Accept-Language' => 'ar'])
            ->assertOk()->assertJsonPath('data.0.name', 'خدمة تجريبية');
    }

    #[Test]
    public function launch_catalog_has_english_names_for_every_category_and_problem(): void
    {
        $this->seed(CatalogSeeder::class);

        $response = $this->getJson('/api/v1/catalog', ['Accept-Language' => 'en'])->assertOk();

        $this->assertNotEmpty($response->json('data'));
        $this->assertDoesNotMatchRegularExpression('/\p{Arabic}/u', json_encode($response->json('data'), JSON_UNESCAPED_UNICODE));
    }

    #[Test]
    public function user_names_are_preserved_and_locale_preference_is_validated(): void
    {
        $user = User::factory()->create(['name' => 'أحمد محمود']);

        $this->actingAs($user, 'sanctum')->patchJson('/api/v1/me', ['locale' => 'en'], ['Accept-Language' => 'en'])
            ->assertOk()->assertJsonPath('user.name', 'أحمد محمود')->assertJsonPath('user.locale', 'en');
        $this->assertSame('en', $user->refresh()->locale);
        $this->patchJson('/api/v1/me', ['locale' => 'fr'], ['Accept-Language' => 'ar'])
            ->assertUnprocessable()->assertJsonPath('error.message', 'بيانات غير صالحة.');
    }

    #[Test]
    public function validation_messages_and_fields_use_arabic_or_english(): void
    {
        $arabic = $this->postJson('/api/v1/auth/login', [], ['Accept-Language' => 'ar'])->assertUnprocessable();
        $this->assertSame('حقل البريد الإلكتروني مطلوب.', $arabic->json('error.fields.email.0'));
        $english = $this->postJson('/api/v1/auth/login', [], ['Accept-Language' => 'en'])->assertUnprocessable()->assertHeader('Content-Language', 'en');
        $this->assertSame('The email address field is required.', $english->json('error.fields.email.0'));
        $this->assertDoesNotMatchRegularExpression('/\p{Arabic}/u', $english->getContent());
    }

    #[Test]
    public function legal_versions_are_language_specific_and_unapproved_drafts_never_leak(): void
    {
        $page = LegalPage::query()->create(['slug' => 'terms', 'title' => 'الشروط']);
        $page->versions()->create(['version' => 1, 'locale' => 'ar', 'title' => 'الشروط', 'body' => '<h2>العنوان</h2><p>النص الأصلي</p>', 'status' => LegalPageStatus::Published, 'effective_at' => now()->subDay()]);
        $draft = $page->versions()->create(['version' => 2, 'locale' => 'en', 'title' => 'Terms', 'body' => '<h2>Heading</h2><ol><li>First</li></ol>', 'status' => LegalPageStatus::Draft, 'requires_legal_review' => true]);

        $this->getJson('/api/v1/terms/current', ['Accept-Language' => 'en'])->assertNotFound()->assertJsonPath('error.message', 'This page has not been published yet.');
        $this->getJson('/api/v1/terms/current', ['Accept-Language' => 'ar'])->assertOk()->assertJsonPath('data.title', 'الشروط');
        $draft->forceFill(['status' => LegalPageStatus::Published, 'requires_legal_review' => false, 'effective_at' => now()->subMinute()])->save();
        $this->getJson('/api/v1/terms/current', ['Accept-Language' => 'en'])->assertOk()->assertJsonPath('data.title', 'Terms')->assertJsonPath('data.body', '<h2>Heading</h2><ol><li>First</li></ol>');
    }

    #[Test]
    public function html_keeps_structure_and_removes_active_content_and_unsafe_urls(): void
    {
        $safe = SafeHtml::sanitize('<h2>Heading</h2><ol><li><strong>First</strong></li></ol><script>alert(1)</script><a href="jav&#x61;script:alert(1)" onclick="x()">link</a><a href="https://example.com" style="color:red">safe</a><img src="https://tracker.test/">');

        $this->assertStringContainsString('<ol><li><strong>First</strong></li></ol>', $safe);
        $this->assertStringContainsString('href="https://example.com"', $safe);
        foreach (['javascript', 'onclick', '<script', 'alert(1)', '<img', 'style='] as $unsafe) {
            $this->assertStringNotContainsString($unsafe, $safe);
        }
    }

    #[Test]
    public function public_legal_pages_use_the_requested_direction_and_keep_locale_in_links(): void
    {
        $page = LegalPage::query()->create(['slug' => 'privacy', 'title' => 'سياسة الخصوصية']);
        $page->versions()->create(['version' => 1, 'locale' => 'en', 'title' => 'Privacy Policy', 'body' => '<h2>Privacy</h2><ol><li>First</li></ol>', 'status' => LegalPageStatus::Published, 'effective_at' => now()->subDay()]);

        $this->get('/privacy?locale=en')->assertOk()->assertHeader('Content-Language', 'en')
            ->assertSee('lang="en" dir="ltr"', false)->assertSee('<ol><li>First</li></ol>', false)
            ->assertSee('locale=en', false)->assertDontSee('سياسة الخصوصية');
        $this->get('/privacy?locale=ar')->assertNotFound()->assertSee('lang="ar" dir="rtl"', false);
    }

    #[Test]
    public function unpublished_english_faq_does_not_fall_back_to_arabic(): void
    {
        $this->getJson('/api/v1/support/faqs', ['Accept-Language' => 'en'])
            ->assertOk()->assertJsonPath('data', []);
    }

    #[Test]
    public function authentication_mail_uses_the_recipient_preference(): void
    {
        $user = User::factory()->create(['locale' => 'en']);
        $verification = (new VerifyEmail)->toMail($user);
        $reset = (new ResetPassword('test-token'))->toMail($user);
        $this->assertSame('Verify email', $verification->actionText);
        $this->assertSame('Set a new password', $reset->actionText);
        $user->locale = 'ar';
        $this->assertSame('تفعيل البريد', (new VerifyEmail)->toMail($user)->actionText);
    }

    #[Test]
    public function notifications_render_current_locale_and_push_uses_recipient_device_language(): void
    {
        Queue::fake([SendPushNotification::class]);
        $recipient = User::factory()->create(['locale' => 'ar']);
        $device = DeviceToken::query()->create(['user_id' => $recipient->id, 'token' => str_repeat('x', 40), 'platform' => 'ANDROID', 'app_mode' => 'CUSTOMER', 'locale' => 'en']);
        $order = Order::factory()->create();
        app(OrderNotifications::class)->newMessage($order, $recipient, false);
        $notification = $recipient->notifications()->sole();

        $this->actingAs($recipient, 'sanctum')->getJson('/api/v1/notifications', ['Accept-Language' => 'en'])
            ->assertOk()->assertJsonPath('data.0.title', 'New message');
        $this->getJson('/api/v1/notifications', ['Accept-Language' => 'ar'])
            ->assertOk()->assertJsonPath('data.0.title', 'رسالة جديدة');

        $sender = $this->mock(PushSender::class);
        $sender->shouldReceive('send')->once()->with($device->token, \Mockery::on(fn (PushMessage $message): bool => $message->title === 'New message' && ! preg_match('/\p{Arabic}/u', $message->body)))->andReturn(PushResult::Sent);
        (new SendPushNotification($notification->id, $device->id))->handle($sender);
        Queue::assertPushed(SendPushNotification::class, 1);
    }

    #[Test]
    public function legacy_notification_history_does_not_invent_a_translation(): void
    {
        $original = ['title' => 'عنوان قديم', 'body' => 'نص قديم', 'deep_link' => 'bremo://orders/1'];
        $english = NotificationContent::localized($original, 'en');

        $this->assertSame('Update from Bremo', $english['title']);
        $this->assertTrue($english['legacy_content_unavailable']);
        $this->assertSame('bremo://orders/1', $english['deep_link']);
        $this->assertSame('نص قديم', NotificationContent::localized($original, 'ar')['body']);
    }

    #[Test]
    #[TestWith([1, 'وصل عرض جديد لطلب #1.', 'A new offer was received for request #1.'])]
    #[TestWith([0, 'لا توجد عروض جديدة لطلب #1.', 'No new offers were received for request #1.'])]
    #[TestWith([2, 'وصل عرضان جديدان لطلب #1.', '2 new offers were received for request #1.'])]
    #[TestWith([3, 'وصلت 3 عروض جديدة لطلب #1.', '3 new offers were received for request #1.'])]
    #[TestWith([11, 'وصل 11 عرضًا جديدًا لطلب #1.', '11 new offers were received for request #1.'])]
    #[TestWith([100, 'عدد العروض الجديدة لطلب #1: 100.', '100 new offers were received for request #1.'])]
    public function offer_counts_have_correct_plural_forms(int $count, string $arabic, string $english): void
    {
        $message = new LocalizedMessage('notifications.offers_count', ['count' => $count, 'order' => '#1']);

        $this->assertSame($arabic, $message->render('ar'));
        $this->assertSame($english, $message->render('en'));
    }

    #[Test]
    public function backend_catalog_keys_are_complete_with_matching_placeholders(): void
    {
        $arabic = Arr::dot(config('localization.ar'));
        $english = Arr::dot(config('localization.en'));
        $this->assertSame(array_keys($arabic), array_keys($english));
        foreach ($arabic as $key => $value) {
            $this->assertNotSame('', trim($value), $key);
            $this->assertNotSame('', trim($english[$key]), $key);
            preg_match_all('/:[a-z_]+/', $value, $arParameters);
            preg_match_all('/:[a-z_]+/', $english[$key], $enParameters);
            sort($arParameters[0]);
            sort($enParameters[0]);
            $this->assertSame($arParameters[0], $enParameters[0], $key);
            $this->assertDoesNotMatchRegularExpression('/\p{Arabic}/u', $english[$key], $key);
        }
    }

    #[Test]
    public function missing_system_keys_fail_instead_of_using_another_language(): void
    {
        $this->expectException(\RuntimeException::class);
        Localization::text('missing.key', locale: 'en');
    }

    #[Test]
    public function unmatched_api_routes_still_return_the_requested_language(): void
    {
        $this->getJson('/api/v1/nonexistent', ['Accept-Language' => 'en'])
            ->assertNotFound()->assertHeader('Content-Language', 'en')
            ->assertJsonPath('error.message', 'Not found.');
    }

    #[Test]
    public function an_absent_language_preference_uses_the_authenticated_user(): void
    {
        $this->seed([SettingsSeeder::class, SupportReasonSeeder::class]);
        $this->actingAs(User::factory()->create(['locale' => 'en']), 'sanctum')
            ->getJson('/api/v1/config', ['Accept-Language' => ''])
            ->assertOk()->assertHeader('Content-Language', 'en');
    }

    #[Test]
    public function unexpected_api_failures_return_a_localized_message_without_internal_details(): void
    {
        $this->app->bind(SettingsRepository::class, static function (): never {
            throw new \RuntimeException('Internal implementation details');
        });

        $this->getJson('/api/v1/config', ['Accept-Language' => 'en'])
            ->assertStatus(500)->assertHeader('Content-Language', 'en')
            ->assertExactJson(['error' => ['code' => 'SERVER_ERROR', 'message' => 'We could not complete your request. Please try again later.']]);
    }

    #[Test]
    public function changing_language_does_not_replay_another_language_or_repeat_a_mutation(): void
    {
        $user = User::factory()->create();
        $payload = ['token' => str_repeat('y', 40), 'platform' => 'ANDROID'];
        $this->actingAs($user, 'sanctum')->postJson('/api/v1/me/devices', $payload, ['Accept-Language' => 'ar', 'Idempotency-Key' => 'locale-switch'])->assertNoContent();
        $this->postJson('/api/v1/me/devices', $payload, ['Accept-Language' => 'en', 'Idempotency-Key' => 'locale-switch'])
            ->assertConflict()->assertJsonPath('error.code', 'IDEMPOTENCY_LOCALE_MISMATCH')
            ->assertJsonPath('error.message', 'This action was completed in another language. Refresh to view the result.');
        $this->assertSame(1, DeviceToken::query()->where('user_id', $user->id)->count());
        $this->assertSame('ar', DeviceToken::query()->where('user_id', $user->id)->sole()->locale);
    }
}
