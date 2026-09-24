<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Http\Api\V1\Resources\OrderResource;
use App\Modules\Orders\Models\Order;
use App\Modules\Payments\Actions\ChangePaymentMethodAction;
use App\Modules\Payments\Actions\CreateElectronicPaymentAction;
use App\Modules\Payments\Actions\SubmitInstapayTransferAction;
use App\Modules\Payments\Enums\PaymentChannel;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Enums\PaymentSimulation;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Illuminate\Validation\Rule;

final class PaymentController
{
    public function changeMethod(
        Request $request,
        Order $order,
        ChangePaymentMethodAction $action,
    ): OrderResource {
        Gate::authorize('view', $order);
        $data = $request->validate([
            'payment_method' => ['required', Rule::enum(PaymentMethod::class)],
        ]);

        $order = $action->execute($order, $request->user(), PaymentMethod::from($data['payment_method']));

        return new OrderResource($order->load(['latestPaymentAttempt', 'providerProfile.user']));
    }

    public function store(
        Request $request,
        Order $order,
        CreateElectronicPaymentAction $action,
    ): JsonResponse {
        Gate::authorize('view', $order);
        $data = $request->validate([
            'channel' => ['required', Rule::enum(PaymentChannel::class)],
            'simulation' => [
                Rule::prohibitedIf(fn (): bool => ! $this->simulationAllowed()),
                Rule::enum(PaymentSimulation::class),
            ],
        ]);

        $payment = $action->execute(
            $order,
            $request->user(),
            PaymentChannel::from($data['channel']),
            isset($data['simulation'])
                ? PaymentSimulation::from($data['simulation'])
                : PaymentSimulation::Pending,
        );

        return new JsonResponse(['data' => $payment->mobilePayload()], 201);
    }

    /** BR-057 — `POST /orders/{order}/instapay-transfers`. */
    public function instapayTransfer(
        Request $request,
        Order $order,
        SubmitInstapayTransferAction $action,
    ): JsonResponse {
        Gate::authorize('view', $order);
        $data = $request->validate([
            'transfer_reference' => ['required', 'string', 'min:4', 'max:64', 'regex:/^[A-Za-z0-9\-_ ]+$/'],
            'receipt_media_id' => ['nullable', 'integer'],
        ]);

        $payment = $action->execute(
            $order,
            $request->user(),
            trim($data['transfer_reference']),
            isset($data['receipt_media_id']) ? (int) $data['receipt_media_id'] : null,
        );

        return new JsonResponse([
            'data' => $payment->mobilePayload(),
            'order' => new OrderResource($order->fresh()->load(['latestPaymentAttempt', 'providerProfile.user'])),
        ], 201);
    }

    private function simulationAllowed(): bool
    {
        return config('services.fawry.driver') === 'staging'
            && app()->environment(['local', 'testing', 'staging']);
    }
}
