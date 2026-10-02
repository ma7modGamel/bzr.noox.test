<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Providers\Actions\AddPortfolioItemAction;
use App\Modules\Providers\Models\PortfolioItem;
use App\Modules\Providers\Models\ProviderProfile;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;

final class ProviderPortfolioController
{
    public function store(Request $request, AddPortfolioItemAction $action): JsonResponse
    {
        $data = $request->validate([
            'media_id' => ['required', 'integer'],
            'caption' => ['nullable', 'string', 'max:150'],
        ]);
        $item = $action->execute($this->profile($request), (int) $data['media_id'], $data['caption'] ?? null);

        return new JsonResponse(['data' => $item->only(['id', 'image_path', 'caption'])], 201);
    }

    public function destroy(Request $request, PortfolioItem $portfolio): JsonResponse
    {
        abort_unless($portfolio->provider_profile_id === $this->profile($request)->getKey(), 404);
        $path = $portfolio->image_path;
        $portfolio->delete();
        Storage::disk('local')->delete($path);

        return new JsonResponse(status: 204);
    }

    private function profile(Request $request): ProviderProfile
    {
        return $request->user()->providerProfile
            ?? throw BusinessRuleViolationException::rule('BR-022', 'لا يوجد ملف فني لهذا الحساب.');
    }
}
