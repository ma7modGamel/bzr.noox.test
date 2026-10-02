<?php

declare(strict_types=1);

namespace App\Modules\Catalog\Services;

use App\Support\Exceptions\PriceGuideImportException;
use OpenSpout\Reader\XLSX\Reader;
use Throwable;

final class PriceGuideWorkbookReader
{
    private const SHEET_NAME = 'دليل الأسعار';

    /** @var array<string, list<string>> */
    private const HEADERS = [
        'category' => ['الفئة'],
        'problem_type' => ['نوع المشكلة'],
        'minimum' => ['أقل سعر', 'أقل سعر (جنيه)'],
        'maximum' => ['أعلى سعر', 'أعلى سعر (جنيه)'],
        'notes' => ['ملاحظات'],
    ];

    /** @return list<array{row_number: int, category: mixed, problem_type: mixed, minimum: mixed, maximum: mixed, notes: mixed}> */
    public function read(string $path): array
    {
        if (! is_file($path)) {
            throw new PriceGuideImportException(['ملف دليل الأسعار غير موجود.']);
        }

        if (strtolower(pathinfo($path, PATHINFO_EXTENSION)) !== 'xlsx') {
            throw new PriceGuideImportException(['الملف يجب أن يكون بصيغة XLSX.']);
        }

        $reader = new Reader;

        try {
            $reader->open($path);

            foreach ($reader->getSheetIterator() as $sheet) {
                if ($this->normalize((string) $sheet->getName()) !== self::SHEET_NAME) {
                    continue;
                }

                $rows = [];
                $columns = null;
                $rowNumber = 0;

                foreach ($sheet->getRowIterator() as $row) {
                    $rowNumber++;
                    $values = $row->toArray();

                    if ($columns === null) {
                        $columns = $this->mapHeaders($values);

                        continue;
                    }

                    $candidate = [
                        'row_number' => $rowNumber,
                        'category' => $values[$columns['category']] ?? null,
                        'problem_type' => $values[$columns['problem_type']] ?? null,
                        'minimum' => $values[$columns['minimum']] ?? null,
                        'maximum' => $values[$columns['maximum']] ?? null,
                        'notes' => $values[$columns['notes']] ?? null,
                    ];

                    if (! $this->isEmptyRow($candidate)) {
                        $rows[] = $candidate;
                    }
                }

                if ($columns === null) {
                    throw new PriceGuideImportException(['ورقة «'.self::SHEET_NAME.'» فارغة.']);
                }

                return $rows;
            }
        } catch (PriceGuideImportException $exception) {
            throw $exception;
        } catch (Throwable $exception) {
            throw new PriceGuideImportException(['تعذر قراءة ملف XLSX: '.$exception->getMessage()]);
        } finally {
            $reader->close();
        }

        throw new PriceGuideImportException(['ورقة «'.self::SHEET_NAME.'» غير موجودة في الملف.']);
    }

    /**
     * @param  list<mixed>  $values
     * @return array{category: int, problem_type: int, minimum: int, maximum: int, notes: int}
     */
    private function mapHeaders(array $values): array
    {
        $normalized = array_map(fn (mixed $value): string => $this->normalize($this->stringValue($value)), $values);
        $columns = [];
        $missing = [];

        foreach (self::HEADERS as $key => $acceptedHeaders) {
            $index = false;

            foreach ($acceptedHeaders as $header) {
                $index = array_search($header, $normalized, true);

                if ($index !== false) {
                    break;
                }
            }

            if ($index === false) {
                $missing[] = $acceptedHeaders[0];

                continue;
            }

            $columns[$key] = $index;
        }

        if ($missing !== []) {
            throw new PriceGuideImportException(['عناوين الأعمدة الناقصة: '.implode('، ', $missing).'.']);
        }

        /** @var array{category: int, problem_type: int, minimum: int, maximum: int, notes: int} $columns */
        return $columns;
    }

    /** @param array{row_number: int, category: mixed, problem_type: mixed, minimum: mixed, maximum: mixed, notes: mixed} $row */
    private function isEmptyRow(array $row): bool
    {
        foreach (['category', 'problem_type', 'minimum', 'maximum', 'notes'] as $key) {
            if ($this->stringValue($row[$key]) !== '') {
                return false;
            }
        }

        return true;
    }

    private function stringValue(mixed $value): string
    {
        return is_scalar($value) ? trim((string) $value) : '';
    }

    private function normalize(string $value): string
    {
        return preg_replace('/\s+/u', ' ', trim($value)) ?? trim($value);
    }
}
