<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Communication\Models\Message;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Orders\Services\MediaUploadService;
use App\Modules\Orders\Services\ProviderEligibility;
use App\Modules\Payments\Models\Payment;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\Facades\Storage;
use Symfony\Component\HttpFoundation\StreamedResponse;

final class MediaController
{
    public function store(Request $request, MediaUploadService $uploads): JsonResponse
    {
        $data = $request->validate(['file' => ['required', 'file']]);
        $media = $uploads->store($request->user(), $data['file']);

        return new JsonResponse(['media' => $this->serialize($media)], 201);
    }

    public function show(Request $request, OrderMedia $media, ProviderEligibility $eligibility): StreamedResponse
    {
        $this->authorize($request, $media, $eligibility);

        abort_unless(Storage::disk('local')->exists($media->path), 404);

        return Storage::disk('local')->response($media->path, headers: ['Cache-Control' => 'no-store, private', 'X-Content-Type-Options' => 'nosniff']);
    }

    public function destroy(Request $request, OrderMedia $media): JsonResponse
    {
        abort_unless($media->order_id === null && $media->uploaded_by === $request->user()->getKey(), 404);

        Storage::disk('local')->delete($media->path);
        $media->delete();

        return new JsonResponse(status: 204);
    }

    private function authorize(Request $request, OrderMedia $media, ProviderEligibility $eligibility): void
    {
        if ($media->purpose === 'PAYMENT_RECEIPT'
            || Payment::query()->where('receipt_media_id', $media->getKey())->exists()) {
            abort_unless($media->uploaded_by === $request->user()->getKey(), 404);
        }

        if (Message::query()->whereHas('media', fn ($query) => $query->whereKey($media->id))->exists()) {
            abort_unless(Message::query()->whereHas('media', fn ($query) => $query->whereKey($media->id))
                ->whereHas('conversation', fn ($query) => $query->where('customer_id', $request->user()->id)
                    ->orWhereHas('providerProfile', fn ($provider) => $provider->where('user_id', $request->user()->id)))->exists(), 404);

            return;
        }

        if (in_array($media->purpose, ['REPAIR_BEFORE', 'REPAIR_AFTER'], true)
            && $media->order?->customer_id === $request->user()->id) {
            abort_unless($media->order->completed_at !== null, 404);
        }

        if ($media->order_id === null) {
            abort_unless($media->uploaded_by === $request->user()->getKey(), 404);

            return;
        }

        if (Gate::allows('view', $media->order)) {
            return;
        }

        $profile = $request->user()->providerProfile;
        abort_unless(
            $profile !== null
                && $media->order->status->value === 'OPEN'
                && $eligibility->isEligible($media->order, $profile),
            404,
        );
    }

    /** @return array<string, mixed> */
    private function serialize(OrderMedia $media): array
    {
        return [
            'id' => $media->getKey(),
            'type' => $media->type,
            'size_bytes' => $media->size_bytes,
            'duration_sec' => $media->duration_sec,
            'expires_at' => $media->expires_at?->toIso8601String(),
        ];
    }
}
