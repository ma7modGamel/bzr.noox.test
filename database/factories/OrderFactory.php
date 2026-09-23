<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Modules\Catalog\Models\Category;
use App\Modules\Catalog\Models\ProblemType;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Orders\Models\Order;
use App\Modules\Settings\Enums\OperatingMode;
use Illuminate\Database\Eloquent\Factories\Factory;
use Illuminate\Support\Facades\DB;

/** @extends Factory<Order> */
final class OrderFactory extends Factory
{
    protected $model = Order::class;

    public function definition(): array
    {
        $city = City::query()->firstOrCreate(['name' => 'القاهرة'], ['is_active' => true]);
        $area = Area::query()->firstOrCreate(
            ['city_id' => $city->id, 'name' => 'مدينة نصر'],
            ['is_active' => true],
        );
        $category = Category::query()->firstOrCreate(['name' => 'سباكة'], ['is_active' => true]);
        $problemType = ProblemType::query()->firstOrCreate(
            ['category_id' => $category->id, 'name' => 'تسريب مياه'],
            ['is_active' => true],
        );

        return [
            'number' => DB::table('order_numbers')->insertGetId([]),
            'operating_mode' => OperatingMode::Employee,
            'customer_id' => UserFactory::new(),
            'city_id' => $city->id,
            'area_id' => $area->id,
            'category_id' => $category->id,
            'problem_type_id' => $problemType->id,
            'description' => 'تسريب أسفل الحوض منذ يومين.',
            'address_text' => 'شارع الطيران، عمارة 12',
            'lat' => '30.0600000',
            'lng' => '31.3400000',
            'timing_type' => TimingType::Now,
            'pricing_mode' => PricingMode::Inspection,  // BR-008
            'materials_responsibility' => MaterialsResponsibility::Unsure,
            'status' => OrderStatus::Open,
            'selection_deadline_at' => now()->addHour(),
            'terms_version' => 1,
            'terms_accepted_at' => now(),
        ];
    }

    public function marketplace(): static
    {
        return $this->state(fn (): array => [
            'operating_mode' => OperatingMode::Marketplace,
            'offers_close_at' => now()->addMinutes(30),
        ]);
    }

    public function status(OrderStatus $status): static
    {
        return $this->state(fn (): array => ['status' => $status]);
    }

    public function scheduled(): static
    {
        return $this->state(fn (): array => [
            'timing_type' => TimingType::Scheduled,
            'slot_start' => now()->addDay()->setTime(15, 0),
            'slot_end' => now()->addDay()->setTime(17, 0),
        ]);
    }
}
