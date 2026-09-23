<?php

declare(strict_types=1);

namespace App\Modules\Settings\Services;

use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\CfgType;
use App\Modules\Settings\Models\Setting;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\DB;

/**
 * المصدر الوحيد لقراءة الإعدادات (30 §آلة الحالات: خدمة واحدة بتخزين مؤقت).
 * لا يقرأ أي كود آخر جدول `settings` مباشرة.
 */
final class SettingsRepository
{
    private const CACHE_KEY = 'bzr:settings';

    /** @var array<string, mixed>|null */
    private ?array $memo = null;

    public function get(Cfg $key): mixed
    {
        $value = $this->all()[$key->value] ?? $key->default();

        return $this->cast($key, $value);
    }

    public function bool(Cfg $key): bool
    {
        return (bool) $this->get($key);
    }

    public function int(Cfg $key): int
    {
        return (int) $this->get($key);
    }

    public function decimal(Cfg $key): ?string
    {
        $value = $this->get($key);

        return $value === null ? null : (string) $value;
    }

    public function string(Cfg $key): ?string
    {
        $value = $this->get($key);

        return $value === null ? null : (string) $value;
    }

    /** @return array<int, mixed> */
    public function array(Cfg $key): array
    {
        return (array) ($this->get($key) ?? []);
    }

    public function set(Cfg $key, mixed $value, ?int $adminId = null): void
    {
        Setting::query()->updateOrCreate(
            ['key' => $key->value],
            ['value' => $value, 'updated_by' => $adminId],
        );

        $this->flush();
    }

    /** @param array<string, mixed> $values مفاتيح Cfg->value */
    public function setMany(array $values, ?int $adminId = null): void
    {
        DB::transaction(function () use ($values, $adminId): void {
            foreach ($values as $key => $value) {
                Setting::query()->updateOrCreate(
                    ['key' => $key],
                    ['value' => $value, 'updated_by' => $adminId],
                );
            }
        });

        $this->flush();
    }

    /** @return array<string, mixed> */
    public function all(): array
    {
        return $this->memo ??= Cache::rememberForever(
            self::CACHE_KEY,
            fn (): array => Setting::query()->pluck('value', 'key')->all(),
        );
    }

    public function flush(): void
    {
        $this->memo = null;
        Cache::forget(self::CACHE_KEY);
    }

    private function cast(Cfg $key, mixed $value): mixed
    {
        if ($value === null) {
            return null;
        }

        return match ($key->type()) {
            CfgType::Bool => (bool) $value,
            CfgType::Int => (int) $value,
            CfgType::Decimal, CfgType::Time, CfgType::String => (string) $value,
            CfgType::Json => (array) $value,
        };
    }
}
