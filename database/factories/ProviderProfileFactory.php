<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Modules\Identity\Models\User;
use App\Modules\Providers\Enums\EmploymentType;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;
use Illuminate\Database\Eloquent\Factories\Factory;

/** @extends Factory<ProviderProfile> */
final class ProviderProfileFactory extends Factory
{
    protected $model = ProviderProfile::class;

    public function definition(): array
    {
        return [
            'user_id' => UserFactory::new(),
            'status' => ProviderStatus::Active,
            'employment_type' => EmploymentType::Employee,
            'experience_years' => fake()->numberBetween(1, 20),
            'available_now' => true,
            'phone_verified_at' => now(),
            'submitted_at' => now(),
            'reviewed_at' => now(),
            'payout_method' => 'INSTAPAY',
        ];
    }

    public function independent(): static
    {
        return $this->state(fn (): array => ['employment_type' => EmploymentType::Independent]);
    }

    public function suspended(): static
    {
        return $this->state(fn (): array => ['status' => ProviderStatus::Suspended]);
    }

    /** يُستدعى بعد الإنشاء لربط الفئة والمنطقة (شرطا BR-022). */
    public function servingFor(int $categoryId, int $areaId): static
    {
        return $this->afterCreating(function (ProviderProfile $profile) use ($categoryId, $areaId): void {
            $profile->categories()->syncWithoutDetaching([$categoryId]);
            $profile->areas()->syncWithoutDetaching([$areaId]);
        });
    }
}
