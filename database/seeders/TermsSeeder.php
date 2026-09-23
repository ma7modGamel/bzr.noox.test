<?php

declare(strict_types=1);

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

/** BR-018 — يُحفظ رقم نسخة الشروط ووقت الموافقة على كل طلب. النص النهائي: OD-04. */
final class TermsSeeder extends Seeder
{
    public function run(): void
    {
        DB::table('terms_versions')->insertOrIgnore([
            'version' => 1,
            'body' => 'مسودة الشروط وسياسة الإلغاء — بانتظار المراجعة القانونية (OD-04).',
            'effective_at' => now(),
            'created_at' => now(),
            'updated_at' => now(),
        ]);
    }
}
