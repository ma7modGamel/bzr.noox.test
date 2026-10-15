<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settlements\Enums\EarningsTransactionType;
use App\Modules\Settlements\Services\DuesBlocking;
use App\Modules\Settlements\Services\ProviderEarningsService;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Localization;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Symfony\Component\HttpFoundation\StreamedResponse;

final class ProviderEarningsController
{
    public function __invoke(Request $request, ProviderEarningsService $earnings, FeatureGate $features, DuesBlocking $dues): JsonResponse
    {
        $data = $this->filters($request);
        $page = (int) ($data['page'] ?? 1);
        $perPage = 20;
        $profile = $this->profile($request);
        $summary = $earnings->summary($profile);
        $transactions = $earnings->filter($summary['transactions'], $data);

        return new JsonResponse(['data' => [
            'operating_mode' => $features->mode()->value,
            'currency' => 'EGP',
            'summary' => [
                'balance' => number_format($summary['balance'], 2, '.', ''),
                'available' => number_format($summary['available'], 2, '.', ''),
                'pending' => number_format($summary['pending'], 2, '.', ''),
            ],
            // DEC-068 — شريط المنع في P18 (BR-063)؛ limit = null في وضع الموظفين أو قبل OD-02.
            'dues' => [
                'blocked' => $profile->isBlockedByDues(),
                'debt' => number_format(max(0.0, -$summary['balance']), 2, '.', ''),
                'limit' => $dues->limit(),
            ],
            'filter_options' => array_map(fn (EarningsTransactionType $type): array => ['code' => $type->value, 'label' => $type->getLabel()], EarningsTransactionType::cases()),
            'filters' => ['type' => $data['type'] ?? 'ALL', 'date_from' => $data['date_from'] ?? null, 'date_to' => $data['date_to'] ?? null, 'timezone' => config('app.timezone')],
            'transactions' => $transactions->slice(($page - 1) * $perPage, $perPage)->values(),
            'meta' => [
                'total' => $transactions->count(),
                'current_page' => $page,
                'last_page' => max(1, (int) ceil($transactions->count() / $perPage)),
            ],
        ]]);
    }

    public function export(Request $request, ProviderEarningsService $earnings): StreamedResponse
    {
        $data = $this->filters($request);
        $profile = $this->profile($request);
        $transactions = $earnings->filter($earnings->summary($profile)['transactions'], $data);

        return response()->streamDownload(function () use ($transactions): void {
            $file = fopen('php://output', 'wb');
            fwrite($file, "\xEF\xBB\xBF");
            fputcsv($file, ['ID', 'Date (ISO 8601)', 'Type', 'Description', 'Details', 'Amount (EGP)', 'Available'], ',', '"', '');
            foreach ($transactions as $row) {
                fputcsv($file, [$row['id'], $row['occurred_at'], $row['type'], $this->csvText($row['title']), $this->csvText($row['detail']), $row['amount'], $row['available'] ? '1' : '0'], ',', '"', '');
            }
            fclose($file);
        }, 'bremo-statement-'.now()->format('Y-m-d').'.csv', ['Content-Type' => 'text/csv; charset=UTF-8', 'Cache-Control' => 'private, no-store']);
    }

    private function csvText(string $value): string
    {
        return preg_match('/^[\s]*[=+@-]/u', $value) === 1 ? "'".$value : $value;
    }

    /** @return array{page?: int, type?: string, date_from?: string|null, date_to?: string|null} */
    private function filters(Request $request): array
    {
        return $request->validate([
            'page' => ['sometimes', 'integer', 'min:1'],
            'type' => ['sometimes', Rule::enum(EarningsTransactionType::class)],
            'date_from' => ['nullable', 'date_format:Y-m-d'],
            'date_to' => ['nullable', 'date_format:Y-m-d', Rule::when($request->filled('date_from'), ['after_or_equal:date_from'])],
        ]);
    }

    private function profile(Request $request): ProviderProfile
    {
        return $request->user()->providerProfile
            ?? throw BusinessRuleViolationException::rule('BR-022', Localization::text('copy.there_is_no_technician_profile_for_this_account'));
    }
}
