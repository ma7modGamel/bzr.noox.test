<?php

declare(strict_types=1);

namespace Database\Seeders;

use Illuminate\Database\Seeder;

final class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $this->call([
            SettingsSeeder::class,
            AdminSeeder::class,
            CatalogSeeder::class,
            LegalPagesSeeder::class, // DEC-051 — مسودات حتى ينشرها المدير العام
            SupportReasonSeeder::class,
        ]);

        $this->call(DemoSeeder::class);
    }
}
