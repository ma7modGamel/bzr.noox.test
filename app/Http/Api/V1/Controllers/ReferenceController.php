<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Catalog\Models\Category;
use App\Modules\Geography\Models\City;
use App\Modules\Orders\Services\SchedulingCalendar;
use Carbon\CarbonImmutable;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** المراجع — 31 §المراجع. الكتالوج والجغرافيا والفترات المتاحة. */
final class ReferenceController
{
    public function cities(): JsonResponse
    {
        return new JsonResponse([
            'data' => City::query()->where('is_active', true)->get()
                ->map(fn (City $c): array => ['id' => $c->id, 'name' => $c->name, 'timezone' => $c->timezone]),
        ]);
    }

    public function areas(City $city): JsonResponse
    {
        return new JsonResponse([
            'data' => $city->areas()->where('is_active', true)->orderBy('sort')->get()
                ->map(fn ($a): array => ['id' => $a->id, 'name' => $a->name]),
        ]);
    }

    /** BR-011 — الفئات المفعّلة في مدينة الطلب، وأنواع المشاكل المفعّلة. */
    public function catalog(Request $request): JsonResponse
    {
        $cityId = (int) $request->query('city_id');

        $categories = Category::query()
            ->where('is_active', true)
            ->when($cityId > 0, fn ($q) => $q->whereHas(
                'cities',
                fn ($c) => $c->where('cities.id', $cityId)->where('city_categories.is_active', true),
            ))
            ->with(['problemTypes' => fn ($q) => $q->where('is_active', true)])
            ->orderBy('sort')
            ->get();

        return new JsonResponse([
            'data' => $categories->map(fn (Category $category): array => [
                'id' => $category->id,
                'name' => $category->name,
                'icon_path' => $category->icon_path,
                // OD-08 — الأيقونة من الخادم، فتتبدل بلا تحديث للتطبيق.
                'icon_url' => $category->icon_path === null ? null : asset($category->icon_path),
                'problem_types' => $category->problemTypes->map(fn ($p): array => [
                    'id' => $p->id,
                    'name' => $p->name,
                    // BR-013 — "مشكلة أخرى" تجعل الوصف إلزاميًا
                    'is_other' => $p->is_other,
                ]),
            ]),
        ]);
    }

    /** BR-017 — الفترات المتاحة فقط: ما بدأ أو اقترب دون CFG-022 لا يظهر. */
    public function slots(Request $request, SchedulingCalendar $calendar): JsonResponse
    {
        $city = City::query()->findOrFail((int) $request->query('city_id'));
        $date = CarbonImmutable::parse((string) $request->query('date', 'today'));

        return new JsonResponse([
            'data' => array_map(fn (array $slot): array => [
                'start' => $slot['start']->toIso8601String(),
                'end' => $slot['end']->toIso8601String(),
            ], $calendar->availableSlots($city, $date)),
        ]);
    }
}
