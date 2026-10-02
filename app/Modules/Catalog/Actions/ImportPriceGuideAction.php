<?php

declare(strict_types=1);

namespace App\Modules\Catalog\Actions;

use App\Modules\Catalog\Data\PriceGuideRow;
use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Catalog\Services\PriceGuideWorkbookReader;
use App\Support\Exceptions\PriceGuideImportException;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;

final readonly class ImportPriceGuideAction
{
    public function __construct(private PriceGuideWorkbookReader $reader) {}

    /** @return list<PriceGuideRow> */
    public function preview(string $path): array
    {
        $sourceRows = $this->reader->read($path);
        $catalog = $this->catalogIndex(Category::query()->with('problemTypes')->get());
        $errors = [];
        $seen = [];
        $rows = [];

        foreach ($sourceRows as $sourceRow) {
            $rowNumber = $sourceRow['row_number'];
            $categoryName = $this->normalizeText($sourceRow['category']);
            $problemTypeName = $this->normalizeText($sourceRow['problem_type']);

            if ($categoryName === '') {
                $errors[] = "الصف {$rowNumber}: الفئة مطلوبة.";
            }

            if ($problemTypeName === '') {
                $errors[] = "الصف {$rowNumber}: نوع المشكلة مطلوب.";
            }

            if ($categoryName === '' || $problemTypeName === '') {
                continue;
            }

            $problemType = $catalog[$this->key($categoryName, $problemTypeName)] ?? null;

            if ($problemType === null) {
                $errors[] = "الصف {$rowNumber}: النوع «{$categoryName} / {$problemTypeName}» غير موجود في الكتالوج.";

                continue;
            }

            if (isset($seen[$problemType->getKey()])) {
                $errors[] = "الصف {$rowNumber}: النوع «{$categoryName} / {$problemTypeName}» مكرر (ظهر أولًا في الصف {$seen[$problemType->getKey()]}).";

                continue;
            }

            $seen[$problemType->getKey()] = $rowNumber;
            $minimum = $this->decimal($sourceRow['minimum'], $rowNumber, 'أقل سعر', $errors);
            $maximum = $this->decimal($sourceRow['maximum'], $rowNumber, 'أعلى سعر', $errors);
            $requiresRange = ! $problemType->is_other;

            if ($requiresRange && ($minimum === null || $maximum === null)) {
                $errors[] = "الصف {$rowNumber}: الحد الأدنى والأقصى مطلوبان لنوع المشكلة العادي.";
            } elseif (($minimum === null) !== ($maximum === null)) {
                $errors[] = "الصف {$rowNumber}: يجب إدخال الحد الأدنى والأقصى معًا أو تركهما معًا.";
            }

            if ($minimum !== null && $maximum !== null && bccomp($minimum, $maximum, 2) > 0) {
                $errors[] = "الصف {$rowNumber}: أقل سعر يجب ألا يزيد على أعلى سعر.";
            }

            $rows[] = new PriceGuideRow(
                rowNumber: $rowNumber,
                problemTypeId: $problemType->getKey(),
                categoryName: $categoryName,
                problemTypeName: $problemTypeName,
                minimum: $minimum,
                maximum: $maximum,
                notes: $this->nullableText($sourceRow['notes']),
            );
        }

        foreach ($catalog as $problemType) {
            if ($problemType->is_other || ! $problemType->is_active || ! $problemType->category->is_active) {
                continue;
            }

            if (! isset($seen[$problemType->getKey()])) {
                $errors[] = "نوع المشكلة المفعّل «{$problemType->category->name} / {$problemType->name}» غير موجود في الملف.";
            }
        }

        if ($errors !== []) {
            throw new PriceGuideImportException(array_values(array_unique($errors)));
        }

        return $rows;
    }

    /** @return list<PriceGuideRow> */
    public function execute(string $path): array
    {
        $rows = $this->preview($path);

        DB::transaction(function () use ($rows): void {
            foreach ($rows as $row) {
                ProblemType::query()->whereKey($row->problemTypeId)->update([
                    'employee_price_min' => $row->minimum,
                    'employee_price_max' => $row->maximum,
                    'employee_price_notes' => $row->notes,
                ]);
            }
        });

        return $rows;
    }

    /**
     * @param  Collection<int, Category>  $categories
     * @return array<string, ProblemType>
     */
    private function catalogIndex(Collection $categories): array
    {
        $catalog = [];

        foreach ($categories as $category) {
            foreach ($category->problemTypes as $problemType) {
                $problemType->setRelation('category', $category);
                $catalog[$this->key($category->name, $problemType->name)] = $problemType;
            }
        }

        return $catalog;
    }

    private function key(string $categoryName, string $problemTypeName): string
    {
        return $this->normalizeText($categoryName).'|'.$this->normalizeText($problemTypeName);
    }

    /** @param list<string> $errors */
    private function decimal(mixed $value, int $rowNumber, string $label, array &$errors): ?string
    {
        $normalized = str_replace([',', '٬', ' '], '', $this->normalizeText($value));

        if ($normalized === '') {
            return null;
        }

        if (! is_numeric($normalized)) {
            $errors[] = "الصف {$rowNumber}: {$label} يجب أن يكون رقمًا.";

            return null;
        }

        $decimal = number_format((float) $normalized, 2, '.', '');

        if (bccomp($decimal, '0.00', 2) <= 0) {
            $errors[] = "الصف {$rowNumber}: {$label} يجب أن يكون أكبر من صفر.";

            return null;
        }

        return $decimal;
    }

    private function normalizeText(mixed $value): string
    {
        if (! is_scalar($value)) {
            return '';
        }

        $text = trim((string) $value);

        return preg_replace('/\s+/u', ' ', $text) ?? $text;
    }

    private function nullableText(mixed $value): ?string
    {
        $text = $this->normalizeText($value);

        return $text === '' ? null : $text;
    }
}
