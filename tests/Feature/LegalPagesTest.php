<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Content\Actions\PublishLegalPageVersionAction;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Content\Models\LegalPageVersion;
use App\Modules\Content\Services\TermsService;
use App\Modules\Identity\Models\Admin;
use App\Modules\Identity\Models\User;
use Database\Seeders\LegalPagesSeeder;
use Illuminate\Auth\Access\AuthorizationException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** DEC-051 / BR-018 — الصفحات القانونية من اللوحة. */
final class LegalPagesTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(LegalPagesSeeder::class);
    }

    #[Test]
    public function المسودات_لا_تظهر_في_التطبيق_ولا_على_الويب(): void
    {
        $this->assertSame(4, LegalPage::query()->count());
        $this->assertSame(4, LegalPageVersion::query()->where('status', LegalPageStatus::Draft->value)->where('requires_legal_review', true)->count());

        $this->getJson('/api/v1/pages/privacy')->assertNotFound()->assertJsonPath('error.code', 'PAGE_NOT_PUBLISHED');
        $this->getJson('/api/v1/terms/current')->assertNotFound();
        $this->get('/privacy')->assertNotFound()->assertSee('قيد الإعداد');
    }

    #[Test]
    public function النشر_يظهر_النسخة_على_الويب_والتطبيق_بعد_تنقية_المحتوى(): void
    {
        $version = $this->version('privacy');
        $version->forceFill(['body' => '<p>نص</p><script>alert(1)</script><p onclick="x()">آمن</p>'])->save();
        app(PublishLegalPageVersionAction::class)->execute($version, Admin::factory()->super()->create());

        $api = $this->getJson('/api/v1/pages/privacy')->assertOk();
        $api->assertJsonPath('data.version', 1)->assertJsonPath('data.slug', 'privacy');
        $this->assertStringNotContainsString('<script', $api->json('data.body_html'));
        $this->assertStringNotContainsString('onclick', $api->json('data.body_html'));

        $this->get('/privacy')->assertOk()->assertSee('dir="rtl"', false)->assertSee('آمن');
    }

    #[Test]
    public function مدير_التشغيل_لا_ينشر(): void
    {
        $this->expectException(AuthorizationException::class);
        app(PublishLegalPageVersionAction::class)->execute($this->version('terms'), Admin::factory()->create());
    }

    #[Test]
    public function نشر_شروط_جديدة_يتطلب_موافقة_عند_أول_طلب_بعدها(): void
    {
        $terms = app(TermsService::class);
        $admin = Admin::factory()->super()->create();
        app(PublishLegalPageVersionAction::class)->execute($this->version('terms'), $admin);
        $this->assertSame(1, $terms->currentVersion());

        $customer = User::factory()->create();
        $this->assertTrue($terms->acceptanceRequired($customer));
        $terms->recordAcceptance($customer, 1);
        $this->assertFalse($terms->acceptanceRequired($customer->refresh()));

        $page = LegalPage::query()->where('slug', 'terms')->sole();
        $next = $page->versions()->create(['version' => 2, 'title' => 'الشروط', 'body' => '<p>v2</p>', 'status' => LegalPageStatus::Draft]);
        app(PublishLegalPageVersionAction::class)->execute($next, $admin);

        $this->assertSame(2, $terms->currentVersion());
        $this->assertTrue($terms->acceptanceRequired($customer->refresh()));
        $this->actingAs($customer, 'sanctum')->getJson('/api/v1/me')
            ->assertOk()
            ->assertJsonPath('user.terms.current_version', 2)
            ->assertJsonPath('user.terms.accepted_version', 1)
            ->assertJsonPath('user.terms.acceptance_required', true);
    }

    #[Test]
    public function نسخة_بتاريخ_سريان_لاحق_لا_تسري_قبل_موعدها(): void
    {
        $version = $this->version('terms');
        $version->forceFill(['effective_at' => now()->addDay()])->save();
        app(PublishLegalPageVersionAction::class)->execute($version, Admin::factory()->super()->create());

        $this->getJson('/api/v1/terms/current')->assertNotFound();
        $this->travel(2)->days();
        $this->getJson('/api/v1/terms/current')->assertOk()->assertJsonPath('data.version', 1);
    }

    #[Test]
    public function الأسئلة_الشائعة_المنشورة_تصل_كقائمة_أسئلة(): void
    {
        app(PublishLegalPageVersionAction::class)->execute($this->version('faq'), Admin::factory()->super()->create());

        $faqs = $this->getJson('/api/v1/support/faqs')->assertOk();
        $faqs->assertJsonPath('data.0.title', 'كيف أتابع حالة طلبي؟');
        $this->assertCount(5, $faqs->json('data'));
    }

    private function version(string $slug): LegalPageVersion
    {
        return LegalPage::query()->where('slug', $slug)->sole()->versions()->sole();
    }
}
