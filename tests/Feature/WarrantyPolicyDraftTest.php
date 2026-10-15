<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Content\Actions\PublishLegalPageVersionAction;
use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Identity\Models\Admin;
use Database\Seeders\LegalPagesSeeder;
use Database\Seeders\SettingsSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class WarrantyPolicyDraftTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function preparing_the_policy_preserves_published_terms_and_does_not_duplicate_drafts(): void
    {
        $this->seed([SettingsSeeder::class, LegalPagesSeeder::class]);
        $page = LegalPage::query()->where('slug', LegalPageSlug::Terms->value)->firstOrFail();
        $version = $page->versions()->firstOrFail();
        $version->update(['body' => '<p>الشروط الحالية.</p>']);
        app(PublishLegalPageVersionAction::class)->execute($version, Admin::factory()->super()->create());
        $before = $version->refresh()->body;
        $this->artisan('content:prepare-warranty-policy')->assertSuccessful();
        $draft = $page->versions()->firstOrFail();
        $this->assertSame(LegalPageStatus::Draft, $draft->status);
        $this->assertStringContainsString('14 يومًا', $draft->body);
        $this->assertStringContainsString('فني مختلف', $draft->body);
        $this->assertStringContainsString('الزيارة والمصنعية فقط', $draft->body);
        $this->assertStringContainsString('الخامات وقطع الغيار خارج الضمان', $draft->body);
        $this->assertStringContainsString('يوافق العميل عليها', $draft->body);
        $this->assertStringContainsString($before, $draft->body);
        $this->assertSame($before, $version->refresh()->body);
        $count = $page->versions()->count();
        $this->artisan('content:prepare-warranty-policy')->assertSuccessful();
        $this->assertSame($count, $page->versions()->count());
    }
}
