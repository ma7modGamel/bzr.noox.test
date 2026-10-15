<?php

namespace Database\Factories;

use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Support\Models\WarrantyClaim;
use Illuminate\Database\Eloquent\Factories\Factory;

/**
 * @extends Factory<WarrantyClaim>
 */
class WarrantyClaimFactory extends Factory
{
    protected $model = WarrantyClaim::class;

    /**
     * Define the model's default state.
     *
     * @return array<string, mixed>
     */
    public function definition(): array
    {
        return [
            'order_id' => Order::factory()->status(OrderStatus::Closed),
            'description' => fake()->sentence(),
            'status' => 'REQUESTED',
        ];
    }
}
