<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Customers\Models\CustomerAddress;
use App\Modules\Geography\Models\Area;
use App\Modules\Identity\Models\User;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
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
            'data' => $this->addressesPayload($request->user()),
        ]);
    }

    public function store(Request $request): JsonResponse
    {
        $data = $this->validated($request);
        $this->assertAreaIsServed((int) $data['area_id']);

        $address = DB::transaction(function () use ($request, $data): CustomerAddress {
            User::query()->whereKey($request->user()->getKey())->lockForUpdate()->firstOrFail();
            if (! $request->user()->addresses()->exists()) {
                $data['is_default'] = true;
            }
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
            User::query()->whereKey($request->user()->getKey())->lockForUpdate()->firstOrFail();
            if ($data['is_default'] ?? false) {
                $request->user()->addresses()->whereKeyNot($address->getKey())->update(['is_default' => false]);
            }

            $address->update($data);
        });

        return new JsonResponse(['data' => $this->payload($address->fresh(['city', 'area']))]);
    }

    public function makeDefault(Request $request, CustomerAddress $address): JsonResponse
    {
        Gate::authorize('update', $address);
        $payload = DB::transaction(function () use ($request, $address): array {
            User::query()->whereKey($request->user()->getKey())->lockForUpdate()->firstOrFail();
            $address->refresh();
            abort_if($address->trashed(), 404);
            $request->user()->addresses()->whereKeyNot($address->getKey())->update(['is_default' => false]);
            $address->update(['is_default' => true]);

            return [
                'data' => $this->payload($address->fresh(['city', 'area'])),
                'addresses' => $this->addressesPayload($request->user()),
            ];
        });

        return new JsonResponse($payload);
    }

    public function destroy(Request $request, CustomerAddress $address): JsonResponse
    {
        Gate::authorize('delete', $address);

        $defaultAddress = DB::transaction(function () use ($request, $address): ?CustomerAddress {
            User::query()->whereKey($request->user()->getKey())->lockForUpdate()->firstOrFail();
            $address->refresh();
            abort_if($address->trashed(), 404);
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

    /** @return list<array<string, mixed>> */
    private function addressesPayload(User $user): array
    {
        return $user->addresses()->with(['city', 'area'])->orderByDesc('is_default')->orderByDesc('id')->get()
            ->map(fn (CustomerAddress $address): array => $this->payload($address))->all();
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
            throw BusinessRuleViolationException::rule('BR-010', Localization::text('copy.this_service_is_unavailable_in_this_area'));
        }
    }

    private function displayAddress(CustomerAddress $address): string
    {
        $parts = [Localization::field($address->city), Localization::field($address->area), $address->address_text];
        foreach (['building', 'floor', 'apartment'] as $field) {
            $value = $address->{$field};
            if ($value !== null && trim($value) !== '') {
                $parts[] = str_replace('{value}', $value, Localization::design('address.detail.'.$field));
            }
        }
        $parts[] = $address->landmark;

        return implode(' · ', array_filter($parts, fn (?string $part): bool => $part !== null && trim($part) !== ''));
    }

    /** @return array<string, mixed> */
    private function payload(CustomerAddress $address): array
    {
        return [
            'id' => $address->id,
            'label' => $address->label,
            'city' => ['id' => $address->city_id, 'name' => Localization::field($address->city)],
            'area' => ['id' => $address->area_id, 'name' => Localization::field($address->area)],
            'address_text' => $address->address_text,
            'display_address' => $this->displayAddress($address),
            'building' => $address->building,
            'floor' => $address->floor,
            'apartment' => $address->apartment,
            'landmark' => $address->landmark,
            'lat' => $address->lat,
            'lng' => $address->lng,
            'is_default' => $address->is_default,
            'actions' => array_values(array_filter([
                ['code' => 'edit', 'label' => Localization::design('common.edit')],
                $address->is_default ? null : ['code' => 'set_default', 'label' => Localization::design('address.default')],
                ['code' => 'delete', 'label' => Localization::design('address.delete')],
            ])),
        ];
    }
}
