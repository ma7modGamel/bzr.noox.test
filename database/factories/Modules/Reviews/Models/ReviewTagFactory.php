<?php

declare(strict_types=1);

namespace Database\Factories\Modules\Reviews\Models;

use App\Modules\Reviews\Models\ReviewTag;
use Illuminate\Database\Eloquent\Factories\Factory;

/** @extends Factory<ReviewTag> */
final class ReviewTagFactory extends Factory
{
    protected $model = ReviewTag::class;

    public function definition(): array
    {
        return ['code' => strtoupper(fake()->unique()->lexify('TAG_????????')), 'label' => fake()->words(2, true),
            'label_en' => fake()->words(2, true), 'sort' => 0, 'is_active' => true];
    }
}
