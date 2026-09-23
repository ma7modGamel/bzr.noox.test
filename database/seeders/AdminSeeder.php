<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Identity\Enums\AdminRole;
use App\Modules\Identity\Models\Admin;
use Illuminate\Database\Seeder;

/** حسابا الإدارة — DEC-021، 23. كلمات المرور للتطوير فقط. */
final class AdminSeeder extends Seeder
{
    public function run(): void
    {
        Admin::query()->firstOrCreate(
            ['email' => 'super@bzr.test'],
            [
                'name' => 'المدير العام',
                'password' => 'password',
                'role' => AdminRole::Super,
                'is_active' => true,
            ],
        );

        Admin::query()->firstOrCreate(
            ['email' => 'ops@bzr.test'],
            [
                'name' => 'مدير التشغيل',
                'password' => 'password',
                'role' => AdminRole::Operations,
                'is_active' => true,
            ],
        );
    }
}
