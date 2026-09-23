<?php

declare(strict_types=1);

namespace Tests\Feature\Admin;

use App\Modules\Identity\Models\Admin;
use Database\Seeders\CatalogSeeder;
use Database\Seeders\SettingsSeeder;
use Filament\Facades\Filament;
use Filament\Resources\Resource;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** كل مورد مسجّل في اللوحة يفتح بلا خطأ — حارس ضد موارد تُضاف وتنكسر بصمت. */
final class PanelResourcesTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed([SettingsSeeder::class, CatalogSeeder::class]);
    }

    #[Test]
    public function كل_صفحات_الفهرس_تفتح_لمدير_التشغيل(): void
    {
        $this->actingAs(Admin::factory()->super()->create(), 'admin');

        $resources = Filament::getPanel('admin')->getResources();

        $this->assertNotEmpty($resources, 'لا مورد مسجّل في اللوحة.');

        foreach ($resources as $resource) {
            /** @var class-string<Resource> $resource */
            $this->get($resource::getUrl('index'))
                ->assertOk();
        }
    }
}
