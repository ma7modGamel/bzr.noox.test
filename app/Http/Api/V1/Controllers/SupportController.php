<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Offers\Models\Offer;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Models\OrderMedia;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Actions\CreateProviderReportAction;
use App\Modules\Support\Actions\OpenDisputeAction;
use App\Modules\Support\Enums\SupportReasonType;
use App\Modules\Support\Models\Dispute;
use App\Modules\Support\Models\DisputeAttachment;
use App\Modules\Support\Models\SupportReason;
use Illuminate\Database\Query\Builder;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Gate;
use Illuminate\Validation\Rule;
use Illuminate\Validation\ValidationException;

/** واجهات C27 وC28؛ أكواد الأسباب لا تُقبل إلا إن كانت فعالة في لوحة الإدارة. */
final class SupportController
{
    public function disputes(Request $request, Order $order): JsonResponse
    {
        Gate::authorize('view', $order);

        $disputes = $order->disputes()
            ->with('attachments')
            ->latest('id')
            ->get();
        $labels = SupportReason::query()
            ->forType(SupportReasonType::Dispute)
            ->whereIn('code', $disputes->pluck('reason_code'))
            ->pluck('label', 'code');

        return new JsonResponse([
            'data' => $disputes->map(
                fn (Dispute $dispute): array => $this->serializeDispute($dispute, $labels->get($dispute->reason_code)),
            )->values(),
        ]);
    }

    public function openDispute(Request $request, Order $order, OpenDisputeAction $action): JsonResponse
    {
        Gate::authorize('openDispute', $order);

        $data = $request->validate([
            'reason_code' => [
                'required',
                'string',
                Rule::exists('support_reasons', 'code')->where(
                    fn (Builder $query): Builder => $query
                        ->where('type', SupportReasonType::Dispute->value)
                        ->where('is_active', true),
                ),
            ],
            'description' => ['required', 'string', 'max:1000'],
            'media_ids' => ['array', 'max:5'],
            'media_ids.*' => ['integer', 'distinct'],
        ]);

        $mediaIds = array_map('intval', $data['media_ids'] ?? []);

        $dispute = DB::transaction(function () use ($request, $order, $action, $data, $mediaIds): Dispute {
            $media = OrderMedia::query()
                ->whereIn('id', $mediaIds)
                ->where('uploaded_by', $request->user()->getKey())
                ->whereNull('order_id')
                ->where('type', 'IMAGE')
                ->lockForUpdate()
                ->get();

            if ($media->count() !== count($mediaIds)) {
                throw ValidationException::withMessages([
                    'media_ids' => 'تحتوي الصور على ملف غير صالح أو غير مملوك للحساب.',
                ]);
            }

            $dispute = $action->execute(
                $order,
                ActorType::Customer,
                $request->user()->getKey(),
                $data['reason_code'],
                $data['description'],
            );

            foreach ($media as $item) {
                $item->forceFill(['order_id' => $order->getKey(), 'expires_at' => null])->save();
                $dispute->attachments()->create([
                    'order_media_id' => $item->getKey(),
                    'path' => $item->path,
                    'uploaded_by_type' => ActorType::Customer,
                    'uploaded_by_id' => $request->user()->getKey(),
                ]);
            }

            return $dispute->load('attachments');
        });

        $label = SupportReason::query()
            ->forType(SupportReasonType::Dispute)
            ->where('code', $dispute->reason_code)
            ->value('label');

        return new JsonResponse(['data' => $this->serializeDispute($dispute, $label)], 201);
    }

    public function reportProvider(
        Request $request,
        ProviderProfile $provider,
        CreateProviderReportAction $action,
    ): JsonResponse {
        $data = $request->validate([
            'order_id' => ['required', 'integer'],
            'reason_code' => [
                'required',
                'string',
                Rule::exists('support_reasons', 'code')->where(
                    fn (Builder $query): Builder => $query
                        ->where('type', SupportReasonType::ProviderReport->value)
                        ->where('is_active', true),
                ),
            ],
            'description' => ['nullable', 'string', 'max:1000'],
        ]);

        $order = Order::query()
            ->whereKey($data['order_id'])
            ->where('customer_id', $request->user()->getKey())
            ->firstOrFail();

        $belongsToOrder = $order->provider_profile_id === $provider->getKey()
            || Offer::query()
                ->where('order_id', $order->getKey())
                ->where('provider_profile_id', $provider->getKey())
                ->exists();
        abort_unless($belongsToOrder, 404);

        $report = $action->execute(
            $request->user(),
            $order,
            $provider,
            $data['reason_code'],
            $data['description'] ?? null,
        );

        return new JsonResponse([
            'data' => [
                'id' => $report->getKey(),
                'status' => $report->status->value,
            ],
        ], 201);
    }

    /** @return array<string, mixed> */
    private function serializeDispute(Dispute $dispute, ?string $reasonLabel): array
    {
        return [
            'id' => $dispute->getKey(),
            'reason_code' => $dispute->reason_code,
            'reason_label' => $reasonLabel ?? $dispute->reason_code,
            'description' => $dispute->description,
            'status' => $dispute->status->value,
            'status_label' => $dispute->status->getLabel(),
            'resolution' => $dispute->resolution?->value,
            'resolution_label' => $dispute->resolution?->getLabel(),
            'resolution_note' => $dispute->resolution_note,
            'is_post_close' => $dispute->is_post_close,
            'attachments' => $dispute->attachments->map(fn (DisputeAttachment $attachment): array => [
                'media_id' => $attachment->order_media_id,
                'url' => $attachment->order_media_id === null
                    ? null
                    : route('api.media.show', ['media' => $attachment->order_media_id]),
            ])->values(),
            'created_at' => $dispute->created_at?->toIso8601String(),
            'resolved_at' => $dispute->resolved_at?->toIso8601String(),
        ];
    }
}
