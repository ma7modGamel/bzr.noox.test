<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Orders\Services\MediaUploadService;
use App\Modules\Orders\Services\ProviderEligibility;
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

        return Storage::disk('local')->download($media->path);
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
