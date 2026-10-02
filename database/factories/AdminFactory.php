<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Modules\Identity\Enums\AdminRole;
use App\Modules\Identity\Models\Admin;
use Illuminate\Database\Eloquent\Factories\Factory;

/** @extends Factory<Admin> */
final class AdminFactory extends Factory
{
    protected $model = Admin::class;

    public function definition(): array
    {
        return [
            'name' => fake('ar_EG')->name(),
            'email' => fake()->unique()->safeEmail(),
            'password' => 'password',
            'role' => AdminRole::Operations,
            'is_active' => true,
            // ASM-14: every admin has TOTP set up; `withoutTwoFactor()` tests the forced setup.
            'two_factor_secret' => 'JBSWY3DPEHPK3PXP',
            'two_factor_confirmed_at' => now(),
        ];
    }

    public function withoutTwoFactor(): static
    {
        return $this->state(fn (): array => ['two_factor_secret' => null, 'two_factor_confirmed_at' => null]);
    }

    public function super(): static
    {
        return $this->state(fn (): array => ['role' => AdminRole::Super]);
    }
}
