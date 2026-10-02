<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settlements\Services\ProviderEarningsService;
use App\Support\Exceptions\BusinessRuleViolationException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

final class ProviderEarningsController
{
    public function __invoke(Request $request, ProviderEarningsService $earnings, FeatureGate $features): JsonResponse
    {
        $data = $request->validate(['page' => ['sometimes', 'integer', 'min:1']]);
        $page = (int) ($data['page'] ?? 1);
        $perPage = 20;
        $summary = $earnings->summary($this->profile($request));
        $transactions = $summary['transactions'];

        return new JsonResponse(['data' => [
            'operating_mode' => $features->mode()->value,
            'currency' => 'EGP',
            'summary' => [
                'balance' => number_format($summary['balance'], 2, '.', ''),
                'available' => number_format($summary['available'], 2, '.', ''),
                'pending' => number_format($summary['pending'], 2, '.', ''),
            ],
            'transactions' => $transactions->slice(($page - 1) * $perPage, $perPage)->values(),
            'meta' => [
                'current_page' => $page,
                'last_page' => max(1, (int) ceil($transactions->count() / $perPage)),
            ],
        ]]);
    }

    private function profile(Request $request): ProviderProfile
    {
        return $request->user()->providerProfile
            ?? throw BusinessRuleViolationException::rule('BR-022', 'لا يوجد ملف فني لهذا الحساب.');
    }
}
