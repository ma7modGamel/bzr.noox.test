<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Http\Middleware\ResolveAppMode;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\OrderStatus;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\ArrivalVerification;
use App\Modules\Orders\Services\OrderExecutionPresentation;
use App\Modules\Orders\Services\OrderPresentation;
use App\Modules\Pricing\Enums\ProposalType;
use App\Modules\Reviews\Enums\CustomerRatingTag;
use App\Modules\Reviews\Models\ReviewTag;
use App\Support\Localization;
use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

/**
 * تمثيل الطلب في الـ API.
 *
 * ما يراه الفني قبل التأكيد محدود بـ BR-024: اسم المنطقة فقط، بلا عنوان تفصيلي ولا هاتف.
 * `available_actions` تأتي من آلة الحالات لا من منطق مكرر في التطبيق (42 §المبدأ الأول).
 *
 * @mixin Order
 */
final class OrderResource extends JsonResource
{
    /** @return array<string, mixed> */
    public function toArray(Request $request): array
    {
        $isProviderView = $request->attributes->get('app_mode') === ResolveAppMode::PROVIDER;
        $actor = $isProviderView ? ActorType::Provider : ActorType::Customer;
        $showFullAddress = $this->showsFullAddress($isProviderView);
        $presentation = app(OrderPresentation::class);
        $conversation = $this->relationLoaded('conversations')
            ? ($isProviderView
                ? $this->conversations->firstWhere('provider_profile_id', $request->user()?->providerProfile?->getKey())
                : ($this->provider_profile_id === null
                    ? $this->conversations->first()
                    : $this->conversations->firstWhere('provider_profile_id', $this->provider_profile_id)))
            : null;

        return [
            'id' => $this->id,
            'number' => $this->number,
            'operating_mode' => $this->operating_mode->value,
            'status' => $this->status->value,
            'status_label' => $this->statusLabel(),
            'version' => $this->version,

            'category' => ['id' => $this->category_id, 'name' => Localization::field($this->category)],
            'problem_type' => ['id' => $this->problem_type_id, 'name' => Localization::field($this->problemType)],
            'customer_address_id' => $this->customer_address_id,
            'description' => $this->description,
            'pricing_mode' => $this->pricing_mode->value,
            'pricing_mode_label' => $this->pricing_mode->getLabel(),
            'is_warranty_visit' => $this->resource->isWarrantyVisit(),
            'warranty_coverage_text' => $this->resource->isWarrantyVisit() ? Localization::text($isProviderView ? 'copy.warranty_provider_coverage' : 'copy.warranty_coverage') : null,
            'warranty_original_order_number' => $this->resource->isWarrantyVisit() ? $this->warrantyClaim->order->number : null,
            'proposal_type_options' => $this->when($isProviderView && $this->resource->isWarrantyVisit(), fn (): array => [
                ['code' => ProposalType::Materials->value, 'label' => ProposalType::Materials->getLabel()],
            ]),
            'customer_rating_tag_options' => $this->when($isProviderView, fn (): array => array_map(
                fn (CustomerRatingTag $tag): array => ['code' => $tag->value, 'label' => $tag->getLabel()], CustomerRatingTag::cases(),
            )),
            'materials_responsibility' => $this->materials_responsibility->value,
            'materials_responsibility_label' => $this->materials_responsibility->getLabel(),
            'budget_amount' => $this->budget_amount,
            'execution_price_guide' => $this->when(
                $isProviderView
                    && $this->operating_mode->value === 'EMPLOYEE'
                    && $this->pricing_mode->value === 'INSPECTION',
                fn (): ?array => $this->problemType === null || $this->problemType->is_other
                    ? null
                    : [
                        'minimum' => $this->problemType->employee_price_min,
                        'maximum' => $this->problemType->employee_price_max,
                        'currency' => Localization::text('copy.egp'),
                    ],
            ),
            'price_guide_review_required' => $this->when(
                $isProviderView && $this->operating_mode->value === 'EMPLOYEE' && ! $this->resource->isWarrantyVisit(),
                (bool) $this->problemType?->is_other,
            ),

            'timing' => [
                'type' => $this->timing_type->value,
                'slot_start' => $this->slot_start?->toIso8601String(),
                'slot_end' => $this->slot_end?->toIso8601String(),
            ],

            'location' => array_filter([
                'area' => Localization::field($this->area),           // BR-024 — اسم المنطقة يظهر دائمًا
                'city' => Localization::field($this->city),
                'address_text' => $showFullAddress ? $this->address_text : null,
                'building' => $showFullAddress ? $this->building : null,
                'floor' => $showFullAddress ? $this->floor : null,
                'apartment' => $showFullAddress ? $this->apartment : null,
                'landmark' => $showFullAddress ? $this->landmark : null,
                'lat' => $showFullAddress ? $this->lat : null,
                'lng' => $showFullAddress ? $this->lng : null,
            ], fn ($v) => $v !== null),

            'deadlines' => $presentation->deadlines($this->resource),
            'waiting' => $presentation->waiting($this->resource),
            'display_status' => $presentation->displayStatus($this->resource, $actor),
            'stepper' => $presentation->stepper($this->resource),
            'execution_steps' => app(OrderExecutionPresentation::class)->steps($this->resource),
            'completion_estimate' => app(OrderExecutionPresentation::class)->estimate($this->resource),
            'review_tag_options' => $this->when(! $isProviderView, fn (): array => self::serviceReviewTagOptions($request)),
            'conversation_id' => $conversation?->getKey(),

            // BR-057 — يُكتب في ملاحظة تحويل إنستاباي حتى تطابقه الإدارة.
            'payment_reference' => '#'.$this->number,

            'amounts' => [
                'labor_total' => $this->labor_total,
                'materials_total' => $this->materials_total,
                'final_amount' => $this->final_amount,
                'payment_method' => $this->payment_method?->value,
                'payment_method_label' => $this->payment_method?->getLabel(),
                'payment_status' => $this->payment_status->value,
            ],

            'termination' => [
                'reason_code' => $this->cancel_reason_code?->value,
                'reason_label' => $this->cancel_reason_code?->getLabel(),
                'note' => $this->cancel_reason_code === CancelReason::AccountBlocked
                    ? $this->cancel_reason_code->getLabel()
                    : $this->cancel_note,
                'cancelled_at' => $this->cancelled_at?->toIso8601String(),
                'expired_at' => $this->expired_at?->toIso8601String(),
                'closed_at' => $this->closed_at?->toIso8601String(),
            ],

            'latest_payment' => $this->whenLoaded(
                'latestPaymentAttempt',
                fn (): ?array => $this->latestPaymentAttempt?->mobilePayload(),
            ),

            'provider' => $this->whenLoaded('providerProfile', fn (): ?array => $this->providerProfile === null ? null : [
                'id' => $this->providerProfile->id,
                'name' => $this->providerProfile->user->name,
                // قرار المالك: التواصل بين الطرفين رسائل فقط؛ الهاتف للإدارة.
                'phone' => $presentation->phoneVisible($this->resource) ? $this->providerProfile->user->phone : null,
                'rating_avg' => $this->providerProfile->rating_avg,
                'completed_orders' => $this->providerProfile->completed_orders_count,
                'is_verified' => $this->providerProfile->isVerified(),
            ]),

            'customer' => $this->when($isProviderView, fn (): array => [
                // BR-024 — قبل التأكيد: الاسم الأول والحرف الأول فقط
                'name' => $this->status->hasAssignedProvider()
                    ? $this->customer?->name
                    : $this->customer?->shortName(),
                'phone' => $presentation->phoneVisible($this->resource) ? $this->customer?->phone : null,
                'rating_avg' => $this->customer?->customer_rating_avg,
            ]),

            'arrival_code_required' => app(ArrivalVerification::class)->required($this->resource) && $this->status === OrderStatus::OnTheWay,
            'arrival_exception_pending' => $this->status === OrderStatus::OnTheWay && ($this->arrival_exception_report['provider_id'] ?? null) === $this->provider_profile_id && $this->arrival_exception_report !== null,
            'absence_report_pending' => $this->resource->hasPendingNoShowReport(),

            'work_photos' => ($isProviderView || $this->completed_at !== null)
                ? $this->resource->media->whereIn('purpose', ['REPAIR_BEFORE', 'REPAIR_AFTER'])->map(fn ($media): array => ['id' => $media->getKey(), 'purpose' => $media->purpose, 'content_url' => route('api.media.show', $media)])->values()
                : [],
            'review_photo_ids' => $this->resource->media->where('purpose', 'CUSTOMER_REVIEW')->pluck('id')->values(),
            'warranty_until' => $this->resource->warrantyExpiresAt()?->toIso8601String(),

            'available_actions' => $request->user() === null
                ? []
                : $presentation->availableActions($this->resource, $request->user(), $actor),

            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }

    /** @return list<array{code: string, label: string}> */
    private static function serviceReviewTagOptions(Request $request): array
    {
        if (! $request->attributes->has('service_review_tag_options')) {
            $request->attributes->set('service_review_tag_options', ReviewTag::options());
        }

        return $request->attributes->get('service_review_tag_options');
    }

    /** BR-024 — الفني لا يرى العنوان التفصيلي قبل التأكيد. */
    private function showsFullAddress(bool $isProviderView): bool
    {
        return ! $isProviderView || $this->status->hasAssignedProvider();
    }
}
