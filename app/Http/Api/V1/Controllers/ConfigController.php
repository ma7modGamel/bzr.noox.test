<?php

declare(strict_types=1);

namespace App\Http\Api\V1\Controllers;

use App\Modules\Orders\Enums\ActorType;
use App\Modules\Orders\Enums\CancelReason;
use App\Modules\Orders\Enums\MaterialsResponsibility;
use App\Modules\Orders\Enums\PricingMode;
use App\Modules\Orders\Enums\TimingType;
use App\Modules\Payments\Enums\PaymentChannel;
use App\Modules\Payments\Enums\PaymentMethod;
use App\Modules\Payments\Services\PaymentChannels;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\FeatureGate;
use App\Modules\Settings\Services\SettingsRepository;
use App\Modules\Support\Enums\SupportReasonType;
use App\Modules\Support\Models\SupportReason;
use BackedEnum;
use Filament\Support\Contracts\HasLabel;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\DB;

/**
 * `GET /config` — 31، DEC-041.
 *
 * التطبيقان يقرآنه عند الإقلاع وعند العودة للمقدمة، فيخفيان شاشات العروض ورسوم المعاينة
 * **بمفتاح لا بإصدار**. هذا ما يجعل تفعيل CFG-090 إعدادًا لا مراجعة متجر (39).
 */
final class ConfigController
{
    public function __invoke(FeatureGate $features, SettingsRepository $settings, PaymentChannels $channels): JsonResponse
    {
        $supportReasons = SupportReason::query()
            ->active()
            ->orderBy('sort')
            ->orderBy('id')
            ->get()
            ->groupBy(fn (SupportReason $reason): string => $reason->type->value);

        return new JsonResponse([
            'tokens_version' => config('mobile.tokens_version'),
            'operating_mode' => $features->mode()->value,
            'offers_enabled' => $features->offersEnabled(),
            'inspection_fee_enabled' => $features->inspectionFeeEnabled(),
            'service_hours' => [
                'from' => $settings->string(Cfg::ServiceHoursFrom),
                'to' => $settings->string(Cfg::ServiceHoursTo),
            ],
            'slots' => [
                'duration_minutes' => 120,
                'minimum_lead_minutes' => $settings->int(Cfg::MinLeadMinutesBeforeSlot),
                'booking_horizon_days' => 7,
            ],
            'service_slots' => $settings->array(Cfg::ScheduledSlots),
            'limits' => [
                'max_offers' => $settings->int(Cfg::MaxOffersPerOrder),
                'min_offer_amount' => $settings->decimal(Cfg::MinOfferAmount),
                'offer_window_minutes' => $settings->int(Cfg::OffersWindowNowMinutes),
                'max_open_orders' => $settings->int(Cfg::MaxOpenOrdersPerCustomer),
            ],
            'proposal_timeout_minutes' => $settings->int(Cfg::ProposalTimeoutMinutes),
            'location_update_seconds' => $settings->int(Cfg::LocationUpdateSeconds),
            'rating_window_days' => $settings->int(Cfg::RatingWindowDays),
            'media_limits' => [
                'images' => 5,
                'image_mb' => 10,
                'video_seconds' => 60,
                'video_mb' => 50,
                'audio_seconds' => 120,
                'audio_mb' => 5,
                'audio_mime' => 'audio/mp4',
                'audio_channels' => 1,
                'audio_bitrate_bps' => 64_000,
            ],
            'option_lists' => [
                'customer_cancellation_reasons' => $this->enumOptions(CancelReason::forActor(ActorType::Customer)),
                'dispute_reasons' => $supportReasons
                    ->get(SupportReasonType::Dispute->value, collect())
                    ->map(fn (SupportReason $reason): array => $reason->toOption())
                    ->values(),
                'provider_report_reasons' => $supportReasons
                    ->get(SupportReasonType::ProviderReport->value, collect())
                    ->map(fn (SupportReason $reason): array => $reason->toOption())
                    ->values(),
                'timing_types' => $this->enumOptions(TimingType::cases()),
                'materials_responsibilities' => $this->enumOptions(MaterialsResponsibility::cases()),
                'pricing_modes' => $this->enumOptions(PricingMode::cases()),
                'payment_methods' => $this->enumOptions(PaymentMethod::cases()),
                'payment_channels' => $this->enumOptions(PaymentChannel::cases()),
                // DEC-050 — القنوات المفعّلة من اللوحة فقط؛ التطبيق لا يعرض غيرها.
                'payment_gateways' => $this->enumOptions($channels->enabled()),
            ],
            'option_defaults' => [
                'timing_type' => TimingType::Now->value,
                'materials_responsibility' => MaterialsResponsibility::Unsure->value,
                'payment_method' => PaymentMethod::Cash->value,
            ],
            'instapay' => $channels->instapay(),
            'terms_version' => (int) (DB::table('terms_versions')->max('version') ?? 1),
            'minimum_supported_app_version' => config('mobile.minimum_supported_app_version'),
        ]);
    }

    /**
     * @param  list<BackedEnum&HasLabel>  $cases
     * @return list<array{code: string, label: string}>
     */
    private function enumOptions(array $cases): array
    {
        return array_map(
            static fn (BackedEnum&HasLabel $case): array => [
                'code' => (string) $case->value,
                'label' => $case->getLabel(),
            ],
            $cases,
        );
    }
}
