<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use Illuminate\Database\Seeder;

/**
 * منطقة التجربة (DEC-052) وكتالوج الإطلاق (DEC-053). قائمة مبدئية يراجعها المالك من اللوحة:
 * كل مدينة ومنطقة وفئة ونوع مشكلة قابل للتعديل والإخفاء وإعادة الترتيب.
 * لا أنواع مشاكل خطرة (غاز، ماس كهربائي) لأن الطوارئ مؤجلة (DEC-012).
 */
final class CatalogSeeder extends Seeder
{
    /** @var array<string, array{key: string, active: bool, problems: list<string>}> */
    private const CATALOG = [
        'سباكة' => ['key' => 'plumbing', 'active' => true, 'problems' => [
            'تسريب مياه', 'انسداد صرف', 'تركيب أو تغيير خلاط', 'تركيب أو صيانة سخان',
            'تركيب أدوات صحية', 'صيانة سيفون أو صندوق طرد', 'ضعف ضغط المياه', 'تركيب فلتر مياه',
        ]],
        'كهرباء' => ['key' => 'electricity', 'active' => true, 'problems' => [
            'انقطاع الكهرباء عن جزء من الشقة', 'فصل القاطع باستمرار', 'تركيب إضاءة أو نجف',
            'تركيب أو تغيير فيش ومفاتيح', 'تمديد خط كهرباء جديد', 'تركيب مروحة سقف',
            'تركيب أو صيانة لوحة كهرباء', 'تركيب جرس أو إنتركم',
        ]],
        'تكييفات' => ['key' => 'air-conditioning', 'active' => true, 'problems' => [
            'التكييف لا يبرد', 'تسريب مياه من التكييف', 'صوت عالي', 'شحن فريون',
            'تنظيف وصيانة دورية', 'تركيب تكييف جديد', 'فك ونقل تكييف',
        ]],
        'أجهزة منزلية' => ['key' => 'appliances', 'active' => true, 'problems' => [
            'صيانة غسالة', 'صيانة ثلاجة أو ديب فريزر', 'صيانة بوتاجاز أو فرن',
            'صيانة ميكروويف', 'صيانة غسالة أطباق', 'تركيب جهاز جديد',
        ]],
        'نجارة' => ['key' => 'carpentry', 'active' => true, 'problems' => [
            'إصلاح أو ضبط باب', 'تغيير كالون أو مفصلات', 'فتح باب مقفول',
            'إصلاح دولاب أو مطبخ', 'فك وتركيب أثاث', 'تركيب أرفف',
        ]],
        'دهانات' => ['key' => 'painting', 'active' => true, 'problems' => [
            'دهان غرفة', 'دهان شقة كاملة', 'معالجة رطوبة وتقشير', 'ترميم شروخ ومعجون', 'ورق حائط',
        ]],
        'تركيبات منزلية متفرقة' => ['key' => 'home-installations', 'active' => true, 'problems' => [
            'تركيب ستائر وجرارات', 'تعليق شاشة على الحائط', 'تعليق لوحات ومرايات',
            'تركيب إكسسوارات حمام', 'تجميع أثاث جاهز',
        ]],
        'ألوميتال وزجاج' => ['key' => 'aluminum-glass', 'active' => true, 'problems' => [
            'إصلاح شباك أو باب ألوميتال', 'تغيير زجاج مكسور', 'تركيب سلك ناموسية', 'تغيير عجل أو كوالين',
        ]],
        // مخفية حتى يتوفر فنيون (DEC-053)
        'حدادة' => ['key' => 'ironwork', 'active' => false, 'problems' => [
            'إصلاح باب أو شباك حديد', 'لحام', 'تركيب حماية شبابيك',
        ]],
        'سيراميك وأرضيات' => ['key' => 'tiles-flooring', 'active' => false, 'problems' => [
            'تغيير بلاطات مكسورة', 'ترويب', 'تركيب لمساحة صغيرة',
        ]],
        'دش وستالايت' => ['key' => 'satellite', 'active' => false, 'problems' => [
            'ضبط الدش', 'تركيب دش جديد', 'مشكلة إشارة أو ريسيفر',
        ]],
        'تنظيف خزانات المياه' => ['key' => 'water-tanks', 'active' => false, 'problems' => [
            'تنظيف وتعقيم خزان',
        ]],
        'مكافحة حشرات' => ['key' => 'pest-control', 'active' => false, 'problems' => [
            'رش شقة', 'مكافحة نوع معين',
        ]],
    ];

    /** @var array<string, bool> المنطقة ← مفعّلة؟ */
    private const AREAS = [
        'الحي الأول' => true,
        'الحي الثاني' => true,
        'الحي الثالث' => true,
        'الحي الرابع' => true,
        'الحي الخامس' => true,
        'المنطقة المركزية' => true,
        'منطقة الشاليهات' => false,
        'المنطقة الصناعية' => false,
    ];

    public function run(): void
    {
        $city = City::query()->firstOrCreate(
            ['name' => 'دمياط الجديدة'],
            ['is_active' => true, 'timezone' => 'Africa/Cairo', 'center_lat' => 31.4368, 'center_lng' => 31.6670, 'radius_km' => 8],
        );

        $sort = 0;
        foreach (self::AREAS as $name => $active) {
            Area::query()->firstOrCreate(
                ['city_id' => $city->id, 'name' => $name],
                ['is_active' => $active, 'sort' => $sort++],
            );
        }

        $categorySort = 0;
        foreach (self::CATALOG as $name => $entry) {
            $category = Category::query()->firstOrCreate(
                ['name' => $name],
                [
                    'is_active' => $entry['active'],
                    'sort' => $categorySort,
                    'icon_path' => 'design/categories/'.$entry['key'].'.svg', // OD-08
                ],
            );
            $categorySort++;

            $city->categories()->syncWithoutDetaching([$category->id => ['is_active' => $entry['active']]]);

            foreach ($entry['problems'] as $index => $problem) {
                ProblemType::query()->firstOrCreate(
                    ['category_id' => $category->id, 'name' => $problem],
                    ['is_active' => true, 'sort' => $index, 'is_other' => false],
                );
            }

            // BR-012 — لكل فئة «مشكلة أخرى» آخر القائمة، وتجعل الوصف إلزاميًا (BR-013).
            ProblemType::query()->firstOrCreate(
                ['category_id' => $category->id, 'name' => 'مشكلة أخرى'],
                ['is_active' => true, 'sort' => 99, 'is_other' => true],
            );
        }
    }
}
