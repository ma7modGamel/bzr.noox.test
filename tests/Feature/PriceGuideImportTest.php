<?php

declare(strict_types=1);

namespace Tests\Feature;

use App\Modules\Catalog\Actions\ImportPriceGuideAction;
use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Support\Exceptions\PriceGuideImportException;
use Illuminate\Foundation\Testing\RefreshDatabase;
use OpenSpout\Common\Entity\Row;
use OpenSpout\Writer\XLSX\Writer;
use PHPUnit\Framework\Attributes\Test;
use Tests\TestCase;

final class PriceGuideImportTest extends TestCase
{
    use RefreshDatabase;

    #[Test]
    public function يستورد_النطاقات_ذريا_ويقبل_مشكلة_أخرى_بلا_نطاق(): void
    {
        [$problem, $other] = $this->catalog();
        $path = base_path('tests/fixtures/price-guide-valid.xlsx');

        $rows = app(ImportPriceGuideAction::class)->execute($path);

        $this->assertCount(2, $rows);
        $this->assertSame('150.00', $problem->refresh()->employee_price_min);
        $this->assertSame('450.00', $problem->employee_price_max);
        $this->assertSame('أسعار اختبارية فقط', $problem->employee_price_notes);
        $this->assertNull($other->refresh()->employee_price_min);
        $this->assertNull($other->employee_price_max);
    }

    #[Test]
    public function يرفض_الصف_الناقص_بسبب_واضح_ولا_يحفظ_أي_تغيير(): void
    {
        [$problem] = $this->catalog();
        $problem->update(['employee_price_min' => '90.00', 'employee_price_max' => '120.00']);
        $path = $this->workbook([
            ['سباكة', 'تسريب مياه', 150, null, null],
            ['سباكة', 'مشكلة أخرى', null, null, null],
        ]);

        try {
            app(ImportPriceGuideAction::class)->execute($path);
            $this->fail('كان يجب رفض الملف الناقص.');
        } catch (PriceGuideImportException $exception) {
            $this->assertStringContainsString('الصف 2', $exception->getMessage());
            $this->assertStringContainsString('الحد الأدنى والأقصى مطلوبان', $exception->getMessage());
        }

        $this->assertSame('90.00', $problem->refresh()->employee_price_min);
        $this->assertSame('120.00', $problem->employee_price_max);
    }

    #[Test]
    public function يرفض_نوعا_مفعلا_غير_موجود_في_الملف(): void
    {
        $this->catalog();
        $path = $this->workbook([
            ['سباكة', 'مشكلة أخرى', null, null, null],
        ]);

        $this->expectException(PriceGuideImportException::class);
        $this->expectExceptionMessage('نوع المشكلة المفعّل «سباكة / تسريب مياه» غير موجود في الملف.');

        app(ImportPriceGuideAction::class)->preview($path);
    }

    #[Test]
    public function يرفض_أي_صف_لنوع_عادي_بلا_سعر_حتى_لو_كان_النوع_غير_مفعل(): void
    {
        [$problem] = $this->catalog();
        $problem->update(['is_active' => false]);
        $path = $this->workbook([
            ['سباكة', 'تسريب مياه', null, null, null],
            ['سباكة', 'مشكلة أخرى', null, null, null],
        ]);

        $this->expectException(PriceGuideImportException::class);
        $this->expectExceptionMessage('الحد الأدنى والأقصى مطلوبان لنوع المشكلة العادي');

        app(ImportPriceGuideAction::class)->preview($path);
    }

    #[Test]
    public function يرفض_ملف_الاختبار_الخاطئ_بكل_أرقام_الصفوف_ولا_يحفظ_جزئيا(): void
    {
        [$problem, , $category] = $this->catalog();
        $problem->update(['employee_price_min' => '90.00', 'employee_price_max' => '120.00']);
        foreach (['انسداد صرف', 'تركيب خلاط', 'صيانة سخان'] as $sort => $name) {
            $category->problemTypes()->create([
                'name' => $name,
                'is_other' => false,
                'sort' => $sort + 1,
                'is_active' => true,
            ]);
        }

        try {
            app(ImportPriceGuideAction::class)->execute(base_path('tests/fixtures/price-guide-invalid.xlsx'));
            $this->fail('كان يجب رفض ملف الاختبار ذي الصفوف الخاطئة.');
        } catch (PriceGuideImportException $exception) {
            $this->assertStringContainsString('الصف 3: الحد الأدنى والأقصى مطلوبان', $exception->getMessage());
            $this->assertStringContainsString('الصف 4: أقل سعر يجب أن يكون رقمًا', $exception->getMessage());
            $this->assertStringContainsString('الصف 5: أقل سعر يجب ألا يزيد على أعلى سعر', $exception->getMessage());
            $this->assertStringContainsString('الصف 7: النوع «سباكة / نوع غير موجود» غير موجود', $exception->getMessage());
        }

        $this->assertSame('90.00', $problem->refresh()->employee_price_min);
        $this->assertSame('120.00', $problem->employee_price_max);
        $this->assertNull($category->problemTypes()->where('name', 'انسداد صرف')->value('employee_price_min'));
    }

    /** @return array{ProblemType, ProblemType, Category} */
    private function catalog(): array
    {
        $category = Category::query()->create([
            'name' => 'سباكة',
            'sort' => 0,
            'is_active' => true,
        ]);
        $problem = $category->problemTypes()->create([
            'name' => 'تسريب مياه',
            'is_other' => false,
            'sort' => 0,
            'is_active' => true,
        ]);
        $other = $category->problemTypes()->create([
            'name' => 'مشكلة أخرى',
            'is_other' => true,
            'sort' => 99,
            'is_active' => true,
        ]);

        return [$problem, $other, $category];
    }

    /** @param list<array{mixed, mixed, mixed, mixed, mixed}> $rows */
    private function workbook(array $rows): string
    {
        $path = sys_get_temp_dir().'/bremo-price-guide-'.bin2hex(random_bytes(8)).'.xlsx';
        $writer = new Writer;
        $writer->openToFile($path);
        $writer->getCurrentSheet()->setName('دليل الأسعار');
        $writer->addRow(Row::fromValues([
            'الفئة',
            'نوع المشكلة',
            'أقل سعر (جنيه)',
            'أعلى سعر (جنيه)',
            'ملاحظات',
        ]));

        foreach ($rows as $row) {
            $writer->addRow(Row::fromValues($row));
        }

        $writer->close();

        $this->beforeApplicationDestroyed(static function () use ($path): void {
            @unlink($path);
        });

        return $path;
    }
}
