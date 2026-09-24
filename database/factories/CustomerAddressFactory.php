<?php

declare(strict_types=1);

namespace Database\Factories;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Geography\Models\Area;
use App\Modules\Geography\Models\City;
use Illuminate\Database\Eloquent\Factories\Factory;

/** @extends Factory<CustomerAddress> */
final class CustomerAddressFactory extends Factory
{
    protected $model = CustomerAddress::class;

    public function definition(): array
    {
        $city = City::query()->firstOrCreate(['name' => 'دمياط الجديدة'], ['is_active' => true, 'timezone' => 'Africa/Cairo']); // DEC-052
        $area = Area::query()->firstOrCreate(['city_id' => $city->id, 'name' => 'الحي الأول'], ['is_active' => true]);

        return [
            'user_id' => UserFactory::new(),
            'label' => 'المنزل',
            'city_id' => $city->id,
            'area_id' => $area->id,
            'address_text' => 'شارع الطيران، عمارة 12',
            'building' => '12',
            'floor' => '3',
            'apartment' => '9',
            'lat' => '30.0600000',
            'lng' => '31.3400000',
            'is_default' => true,
        ];
    }
}
