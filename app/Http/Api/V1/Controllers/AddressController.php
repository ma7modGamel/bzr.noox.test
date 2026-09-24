<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Geography\Models\Area;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;

/** العناوين المحفوظة — 20، BR-010. */
final class AddressController
{
    public function index(Request $request): JsonResponse
    {
        return new JsonResponse([
            'data' => $request->user()->addresses()->with(['city', 'area'])->get()
                ->map(fn (CustomerAddress $a): array => $this->payload($a)),
        ]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $this->validated($request);
        $this->assertAreaIsServed((int) $data['area_id']);

        $address = DB::transaction(function () use ($request, $data): CustomerAddress {
            if ($data['is_default'] ?? false) {
                $request->user()->addresses()->update(['is_default' => false]);
            }

            return $request->user()->addresses()->create($data);
        });

        return new JsonResponse(['data' => $this->payload($address->load(['city', 'area']))], 201);
    }

    public function update(Request $request, CustomerAddress $address): JsonResponse
    {
        Gate::authorize('update', $address);

        $data = $this->validated($request);
        $this->assertAreaIsServed((int) $data['area_id']);

        DB::transaction(function () use ($request, $address, $data): void {
            if ($data['is_default'] ?? false) {
                $request->user()->addresses()->whereKeyNot($address->getKey())->update(['is_default' => false]);
            }

            $address->update($data);
        });

        return new JsonResponse(['data' => $this->payload($address->fresh(['city', 'area']))]);
    }

    public function destroy(Request $request, CustomerAddress $address): JsonResponse
    {
        Gate::authorize('delete', $address);

        $defaultAddress = DB::transaction(function () use ($request, $address): ?CustomerAddress {
            $wasDefault = $address->is_default;
            $address->delete();

            if (! $wasDefault) {
                return $request->user()->addresses()
                    ->where('is_default', true)
                    ->with(['city', 'area'])
                    ->first();
            }

            $replacement = $request->user()->addresses()
                ->orderByDesc('updated_at')
                ->orderByDesc('id')
                ->lockForUpdate()
                ->first();

            if ($replacement !== null) {
                $replacement->update(['is_default' => true]);
                $replacement->load(['city', 'area']);
            }

            return $replacement;
        });

        return new JsonResponse([
            'data' => [
                'deleted_id' => $address->getKey(),
                'default_address' => $defaultAddress === null ? null : $this->payload($defaultAddress),
            ],
        ]);
    }

    /** @return array<string, mixed> */
    private function validated(Request $request): array
    {
        return $request->validate([
            'label' => ['required', 'string', 'max:50'],
            'city_id' => ['required', 'integer'],
            'area_id' => ['required', 'integer'],
            'address_text' => ['required', 'string', 'max:255'],
            'building' => ['nullable', 'string', 'max:50'],
            'floor' => ['nullable', 'string', 'max:20'],
            'apartment' => ['nullable', 'string', 'max:20'],
            'landmark' => ['nullable', 'string', 'max:150'],
            'lat' => ['required', 'numeric', 'between:-90,90'],
            'lng' => ['required', 'numeric', 'between:-180,180'],
            'is_default' => ['boolean'],
        ]);
    }

    /** BR-010 — العنوان يجب أن يقع في منطقة مفعّلة في مدينة مفعّلة. */
    private function assertAreaIsServed(int $areaId): void
    {
        $served = Area::query()
            ->where('id', $areaId)
            ->where('is_active', true)
            ->whereHas('city', fn ($q) => $q->where('is_active', true))
            ->exists();

        if (! $served) {
            throw BusinessRuleViolationException::rule('BR-010', 'الخدمة غير متاحة في هذه المنطقة.');
        }
    }

    /** @return array<string, mixed> */
    private function payload(CustomerAddress $address): array
    {
        return [
            'id' => $address->id,
            'label' => $address->label,
            'city' => ['id' => $address->city_id, 'name' => $address->city?->name],
            'area' => ['id' => $address->area_id, 'name' => $address->area?->name],
            'address_text' => $address->address_text,
            'building' => $address->building,
            'floor' => $address->floor,
            'apartment' => $address->apartment,
            'landmark' => $address->landmark,
            'lat' => $address->lat,
            'lng' => $address->lng,
            'is_default' => $address->is_default,
        ];
    }
}
