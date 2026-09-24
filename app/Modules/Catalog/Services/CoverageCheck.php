<?php

declare(strict_types=1);

namespace App\Modules\Catalog\Services;

use App\Modules\Catalog\Models\Category;
use App\Modules\Geography\Models\Area;
use App\Modules\Providers\Enums\ProviderStatus;
use App\Modules\Providers\Models\ProviderProfile;

/**
 * DEC-053 — تحذير (بلا منع) عند تفعيل فئة أو منطقة لا يغطيها أي فني ACTIVE:
 * الطلبات فيها ستنتظر التعيين بلا مؤهلين (BR-007).
 */
final class CoverageCheck
{
    public function categoryWarning(Category $category): ?string
    {
        if (! $category->is_active) {
            return null;
        }

        $covered = ProviderProfile::query()
            ->where('status', ProviderStatus::Active->value)
            ->whereHas('categories', fn ($query) => $query->whereKey($category->getKey()))
            ->exists();

        return $covered ? null : "الفئة «{$category->name}» مفعّلة ولا يوجد فني نشط يغطيها؛ الطلبات فيها ستنتظر بلا فني مؤهل.";
    }

    public function areaWarning(Area $area): ?string
    {
        if (! $area->is_active) {
            return null;
        }

        $covered = ProviderProfile::query()
            ->where('status', ProviderStatus::Active->value)
            ->whereHas('areas', fn ($query) => $query->whereKey($area->getKey()))
            ->exists();

        return $covered ? null : "المنطقة «{$area->name}» مفعّلة ولا يوجد فني نشط يغطيها؛ الطلبات فيها ستنتظر بلا فني مؤهل.";
    }
}
