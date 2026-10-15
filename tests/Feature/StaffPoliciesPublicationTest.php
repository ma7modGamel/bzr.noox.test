<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Content\Models\LegalPageVersion;
use App\Modules\Identity\Models\Admin;
use Database\Seeders\LegalPagesSeeder;
use Illuminate\Foundation\Testing\LazilyRefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class StaffPoliciesPublicationTest extends TestCase
{
    use LazilyRefreshDatabase;

    #[Test]
    public function publication_updates_approved_workflow_and_exposes_current_safe_pages_without_duplicate_versions(): void
    {
        $this->seed(LegalPagesSeeder::class);
        $admin = Admin::factory()->super()->create();
        $before = LegalPageVersion::query()->count();
        $this->artisan('content:publish-staff-policies', ['--preview' => true])->assertSuccessful();
        $this->assertSame($before, LegalPageVersion::query()->count());
        $this->artisan('content:publish-staff-policies', ['--admin' => $admin->id])->assertSuccessful();
        $terms = $this->getJson('/api/v1/terms/current')->assertOk();
        $this->assertStringContainsString('الزيارة والمصنعية فقط', $terms->json('data.body'));
        $this->assertStringContainsString('فني مختلف', $terms->json('data.body'));
        $this->assertStringContainsString('تأكيد وصول الفني', $terms->json('data.body'));
        $this->assertNotNull($terms->json('data.cancellation_policy'));
        $this->assertSame('privacy', $terms->json('data.privacy_policy.slug'));
        $this->assertSame('ar', $terms->json('data.privacy_policy.locale'));
        $privacy = $this->getJson('/api/v1/pages/privacy')->assertOk();
        $this->assertStringContainsString('بالرسائل فقط', $privacy->json('data.body_html'));
        $this->assertStringNotContainsString('يظهر رقم الهاتف للطرفين', $privacy->json('data.body_html'));
        $this->assertStringContainsString('البلاغ وحده لا يلغي', $terms->json('data.cancellation_policy.body_html'));
        $count = LegalPageVersion::query()->count();
        $this->artisan('content:publish-staff-policies', ['--admin' => $admin->id])->assertSuccessful();
        $this->assertSame($count, LegalPageVersion::query()->count());
    }

    #[Test]
    public function publication_rejects_operations_and_inactive_super_admins(): void
    {
        $this->seed(LegalPagesSeeder::class);
        $this->artisan('content:publish-staff-policies', ['--admin' => Admin::factory()->create()->id])->assertFailed();
        $this->artisan('content:publish-staff-policies', ['--admin' => Admin::factory()->super()->create(['is_active' => false])->id])->assertFailed();
        $this->assertSame(0, LegalPageVersion::query()->where('status', 'PUBLISHED')->count());
    }
}
