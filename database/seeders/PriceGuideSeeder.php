<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Catalog\Actions\ImportPriceGuideAction;
use Illuminate\Database\Seeder;

final class PriceGuideSeeder extends Seeder
{
    public function run(ImportPriceGuideAction $import): void
    {
        $rows = $import->execute(base_path('design/data/price-guide-OD-13.xlsx'));

        $this->command?->info('تم استيراد '.count($rows).' صفًا من دليل الأسعار.');
    }
}
