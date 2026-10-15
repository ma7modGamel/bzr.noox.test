<?php

declare(strict_types=1);

use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

/** DEC-069 — OD-02: حد دين الفني 5000 جنيه، يُضبط فقط إن كان فارغًا فلا يكتب فوق قرار المدير العام. */
return new class extends Migration
{
    public function up(): void
    {
        $key = Cfg::ProviderDebtLimit->value;
        $current = DB::table('settings')->where('key', $key)->value('value');

        if ($current === null || json_decode((string) $current, true) === null) {
            DB::table('settings')->updateOrInsert(['key' => $key], ['value' => json_encode('5000.00'), 'updated_at' => now()]);
            app(SettingsRepository::class)->flush();
        }
    }

    public function down(): void
    {
        // قرار تشغيلي؛ لا يُعاد لقيمة فارغة تلقائيًا.
    }
};
