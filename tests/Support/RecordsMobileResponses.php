<?php

declare(strict_types=1);

namespace Tests\Support;

use Illuminate\Testing\TestResponse;

/**
 * DEC-061 — يكتب ردود الخادم الحقيقية (مع RECORD_API_RESPONSES=1) إلى
 * androidapp/app/src/test/resources/api/{area}/ بمعرّفات ثابتة، لتقرأها اختبارات القراءة في المنصتين.
 */
trait RecordsMobileResponses
{
    /** @var array<string, int> */
    private array $recordedIds = [];

    protected function recordResponse(string $area, string $name, TestResponse $response): void
    {
        if (! env('RECORD_API_RESPONSES')) {
            return;
        }

        $directory = base_path("androidapp/app/src/test/resources/api/{$area}");
        if (! is_dir($directory)) {
            mkdir($directory, 0755, true);
        }
        file_put_contents(
            "{$directory}/{$name}.json",
            json_encode($this->stableIds($response->json()), JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES).PHP_EOL,
        );
    }

    private function stableIds(mixed $value, ?string $key = null): mixed
    {
        if (is_array($value)) {
            $result = [];
            foreach ($value as $childKey => $child) {
                $result[$childKey] = $this->stableIds($child, is_string($childKey) ? $childKey : $key);
            }

            return $result;
        }
        if (is_int($value) && $key !== null && ($key === 'id' || str_ends_with($key, '_id'))) {
            return $this->recordedIds["{$key}:{$value}"] ??= 1000 + count($this->recordedIds);
        }

        return $value;
    }
}
