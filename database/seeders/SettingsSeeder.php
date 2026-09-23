<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Models\Setting;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Database\Seeder;

/**
 * تهيئة جدول الإعدادات بقيم 04 §الإعدادات.
 * لا يمسّ إعدادًا غيّرته الإدارة: `insertOrIgnore` على المفتاح.
 */
final class SettingsSeeder extends Seeder
{
    public function run(): void
    {
        $now = now();

        $rows = array_map(fn (Cfg $cfg) => [
            'key' => $cfg->value,
            'value' => json_encode($cfg->default(), JSON_UNESCAPED_UNICODE),
            'updated_at' => $now,
        ], Cfg::cases());

        Setting::query()->insertOrIgnore($rows);

        app(SettingsRepository::class)->flush();
    }
}
