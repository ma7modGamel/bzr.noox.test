<?php

declare(strict_types=1);

namespace App\Http\Controllers;

use App\Modules\Payments\Actions\ProcessPaymentWebhookAction;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

final class FawryWebhookController
{
    public function __invoke(Request $request, ProcessPaymentWebhookAction $action): JsonResponse
    {
        $payload = $request->json()->all();
        $accepted = $action->execute($payload);
        $merchantRef = $payload['merchantRefNumber'] ?? $payload['merchantRefNum'] ?? '';

        Log::notice('Fawry webhook received.', [
            'accepted' => $accepted,
            'merchant_ref_hash' => is_string($merchantRef) && $merchantRef !== ''
                ? hash('sha256', $merchantRef)
                : null,
        ]);

        return new JsonResponse(['received' => true]);
    }
}
