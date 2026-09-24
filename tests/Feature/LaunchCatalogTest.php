<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Services\CoverageCheck;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Database\Seeders\CatalogSeeder;
use Illuminate\Foundation\Testing\RefreshDatabase;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

/** DEC-052 / DEC-053 — دمياط الجديدة وكتالوج الإطلاق. */
final class LaunchCatalogTest extends TestCase
{
    use RefreshDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        $this->seed(CatalogSeeder::class);
    }

    #[Test]
    public function المدينة_والمناطق_حسب_القرار(): void
    {
        $city = City::query()->sole();
        $this->assertSame('دمياط الجديدة', $city->name);
        $this->assertTrue((bool) $city->is_active);
        $this->assertSame(8, (int) $city->radius_km);
        $this->assertEqualsWithDelta(31.4368, (float) $city->center_lat, 0.00001);

        $this->assertSame(
            ['الحي الأول', 'الحي الثاني', 'الحي الثالث', 'الحي الرابع', 'الحي الخامس', 'المنطقة المركزية'],
            Area::query()->where('is_active', true)->orderBy('sort')->pluck('name')->all(),
        );
        $this->assertSame(['منطقة الشاليهات', 'المنطقة الصناعية'], Area::query()->where('is_active', false)->orderBy('sort')->pluck('name')->all());
    }

    #[Test]
    public function ثماني_فئات_مفعلة_بأيقونات_وكل_فئة_آخرها_مشكلة_أخرى(): void
    {
        $this->assertSame(13, Category::query()->count());
        $this->assertSame(8, Category::query()->where('is_active', true)->count());

        foreach (Category::query()->with('problemTypes')->get() as $category) {
            $last = $category->problemTypes->sortBy('sort')->last();
            $this->assertTrue((bool) $last->is_other, $category->name);
            $this->assertNotNull($category->icon_path, $category->name);
            $this->assertFileExists(public_path($category->icon_path));
        }

        $catalog = $this->getJson('/api/v1/catalog?city_id='.City::query()->sole()->id)->assertOk();
        $this->assertCount(8, $catalog->json('data'));
        $this->assertSame('سباكة', $catalog->json('data.0.name'));
        $this->assertStringEndsWith('design/categories/plumbing.svg', (string) $catalog->json('data.0.icon_url'));
        $this->assertSame([], array_values(array_filter(
            array_merge(...array_map(fn (array $category): array => array_column($category['problem_types'], 'name'), $catalog->json('data'))),
            fn (string $name): bool => str_contains($name, 'غاز') || str_contains($name, 'ماس'),
        )));
    }

    #[Test]
    public function التحذير_عند_فئة_أو_منطقة_بلا_فني_نشط(): void
    {
        $category = Category::query()->where('name', 'سباكة')->sole();
        $area = Area::query()->where('name', 'الحي الأول')->sole();
        $check = app(CoverageCheck::class);

        $this->assertNotNull($check->categoryWarning($category));
        $this->assertNotNull($check->areaWarning($area));

        $provider = ProviderProfile::factory()->create(['status' => ProviderStatus::Active]);
        $provider->categories()->attach($category->id);
        $provider->areas()->attach($area->id);

        $this->assertNull($check->categoryWarning($category));
        $this->assertNull($check->areaWarning($area));
        $this->assertNull($check->categoryWarning(Category::query()->where('name', 'حدادة')->sole())); // مخفية: لا تحذير
    }
}
