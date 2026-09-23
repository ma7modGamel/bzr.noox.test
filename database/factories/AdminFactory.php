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
        ];
    }

    public function super(): static
    {
        return $this->state(fn (): array => ['role' => AdminRole::Super]);
    }
}
