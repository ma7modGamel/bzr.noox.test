<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Reviews\Models\ReviewTag;
use Illuminate\Database\Seeder;

final class ReviewTagSeeder extends Seeder
{
    public function run(): void
    {
        $labels = [
            ['QUALITY', 'شغل متقن', 'Quality work'],
            ['PUNCTUAL', 'ملتزم بالموعد', 'On time'],
            ['RESPECTFUL', 'محترم في التعامل', 'Respectful'],
            ['CLEAN', 'ترك المكان نظيفًا', 'Left the place clean'],
            ['FAIR_PRICE', 'سعر مناسب', 'Fair price'],
        ];
        foreach ($labels as $sort => [$code, $label, $english]) {
            ReviewTag::query()->firstOrCreate(['code' => $code], ['label' => $label, 'label_en' => $english, 'sort' => $sort, 'is_active' => true]);
        }
    }
}
