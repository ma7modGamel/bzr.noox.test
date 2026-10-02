<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Resources;

use App\Http\Middleware\ResolveAppMode;
use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Models\Order;
use App\Modules\Orders\Services\OrderPresentation;
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

            'category' => ['id' => $this->category_id, 'name' => $this->category?->name],
            'problem_type' => ['id' => $this->problem_type_id, 'name' => $this->problemType?->name],
            'customer_address_id' => $this->customer_address_id,
            'description' => $this->description,
            'pricing_mode' => $this->pricing_mode->value,
            'pricing_mode_label' => $this->pricing_mode->getLabel(),
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
                        'currency' => 'جنيه',
                    ],
            ),
            'price_guide_review_required' => $this->when(
                $isProviderView && $this->operating_mode->value === 'EMPLOYEE',
                (bool) $this->problemType?->is_other,
            ),

            'timing' => [
                'type' => $this->timing_type->value,
                'slot_start' => $this->slot_start?->toIso8601String(),
                'slot_end' => $this->slot_end?->toIso8601String(),
            ],

            'location' => array_filter([
                'area' => $this->area?->name,           // BR-024 — اسم المنطقة يظهر دائمًا
                'city' => $this->city?->name,
                'address_text' => $showFullAddress ? $this->address_text : null,
                'building' => $showFullAddress ? $this->building : null,
                'floor' => $showFullAddress ? $this->floor : null,
                'apartment' => $showFullAddress ? $this->apartment : null,
                'landmark' => $showFullAddress ? $this->landmark : null,
                'lat' => $showFullAddress ? $this->lat : null,
                'lng' => $showFullAddress ? $this->lng : null,
            ], fn ($v) => $v !== null),

            'deadlines' => $presentation->deadlines($this->resource),
            'display_status' => $presentation->displayStatus($this->resource, $actor),
            'stepper' => $presentation->stepper($this->resource),
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
                'note' => $this->cancel_note,
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
                // BR-101 — الهاتف الحقيقي يظهر للطرفين من CONFIRMED
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

            'available_actions' => $request->user() === null
                ? []
                : $presentation->availableActions($this->resource, $request->user(), $actor),

            'created_at' => $this->created_at?->toIso8601String(),
        ];
    }

    /** BR-024 — الفني لا يرى العنوان التفصيلي قبل التأكيد. */
    private function showsFullAddress(bool $isProviderView): bool
    {
        return ! $isProviderView || $this->status->hasAssignedProvider();
    }
}
