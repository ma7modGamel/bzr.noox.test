<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use Illuminate\Database\Seeder;

/**
 * الكتالوج والجغرافيا — 20، BR-012.
 * مناطق التجربة قرار مفتوح (OD-05)؛ المذكور هنا بذرة تطوير تُبدَّل من اللوحة.
 */
final class CatalogSeeder extends Seeder
{
    public function run(): void
    {
        $cairo = City::query()->firstOrCreate(
            ['name' => 'القاهرة'],
            ['is_active' => true, 'timezone' => 'Africa/Cairo', 'center_lat' => 30.0444, 'center_lng' => 31.2357],
        );

        foreach (['مدينة نصر', 'مصر الجديدة', 'المعادي', 'الزمالك', 'المقطم'] as $i => $name) {
            Area::query()->firstOrCreate(
                ['city_id' => $cairo->id, 'name' => $name],
                ['is_active' => true, 'sort' => $i],
            );
        }

        $catalog = [
            'سباكة' => ['تسريب مياه', 'انسداد', 'تركيب سخان', 'تركيب أدوات صحية'],
            'كهرباء' => ['انقطاع جزئي', 'تركيب إنارة', 'لوحة كهرباء', 'تمديدات'],
            'تكييفات' => ['تنظيف', 'شحن فريون', 'تركيب', 'لا يبرّد'],
            'نجارة' => ['إصلاح باب', 'تركيب مطبخ', 'إصلاح أثاث'],
            'أجهزة منزلية' => ['غسالة', 'ثلاجة', 'بوتاجاز'],
            'دهانات' => ['دهان غرفة', 'معالجة رطوبة'],
        ];

        $sort = 0;

        foreach ($catalog as $categoryName => $problems) {
            $category = Category::query()->firstOrCreate(
                ['name' => $categoryName],
                ['is_active' => true, 'sort' => $sort++],
            );

            $cairo->categories()->syncWithoutDetaching([$category->id => ['is_active' => true]]);

            foreach ($problems as $p => $problem) {
                ProblemType::query()->firstOrCreate(
                    ['category_id' => $category->id, 'name' => $problem],
                    ['is_active' => true, 'sort' => $p, 'is_other' => false],
                );
            }

            // BR-012 — لكل فئة نوع "مشكلة أخرى" يجعل الوصف إلزاميًا (BR-013)
            ProblemType::query()->firstOrCreate(
                ['category_id' => $category->id, 'name' => 'مشكلة أخرى'],
                ['is_active' => true, 'sort' => 99, 'is_other' => true],
            );
        }
    }
}
