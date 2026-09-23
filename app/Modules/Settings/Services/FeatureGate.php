<?php

declare(strict_types=1);

namespace App\Modules\Settings\Services;

use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Support\Exceptions\BusinessRuleViolationException;
use App\Support\Exceptions\FeatureDisabledException;

/**
 * بوابة مفاتيح المرحلة — 39-OPERATING-MODES، BR-006..BR-009.
 * كل إجراء يخص ميزة معطّلة يُرفض هنا قبل أي قفل أو كتابة (30).
 */
final class FeatureGate
{
    public function __construct(private readonly SettingsRepository $settings) {}

    public function mode(): OperatingMode
    {
        return $this->offersEnabled()
            ? OperatingMode::Marketplace
            : OperatingMode::Employee;
    }

    /** CFG-090 */
    public function offersEnabled(): bool
    {
        return $this->settings->bool(Cfg::OffersEnabled);
    }

    /** CFG-091 — رسوم المعاينة. معطّل ⇒ المعاينة مجانية (BR-008). */
    public function inspectionFeeEnabled(): bool
    {
        return $this->settings->bool(Cfg::InspectionFeeEnabled);
    }

    public function inspectionIsFree(): bool
    {
        return ! $this->inspectionFeeEnabled();
    }

    /** يُستخدم في كل إجراء عرض (submitOffer / withdrawOffer / acceptOffer). */
    public function requireOffers(string $action): void
    {
        if (! $this->offersEnabled()) {
            throw FeatureDisabledException::for(Cfg::OffersEnabled, $action);
        }
    }

    /** يُستخدم في التعيين الإداري (T-27 / T-29) — متاح في وضع الموظفين فقط. */
    public function requireEmployeeMode(string $action): void
    {
        if ($this->offersEnabled()) {
            throw FeatureDisabledException::for(Cfg::OffersEnabled, $action);
        }
    }

    /**
     * BR-009: لا يُفعَّل CFG-091 إلا مع CFG-090، لأن رسوم المعاينة تأتي من عرض مقدم الخدمة.
     */
    public function assertFlagCombination(bool $offersEnabled, bool $inspectionFeeEnabled): void
    {
        if ($inspectionFeeEnabled && ! $offersEnabled) {
            throw BusinessRuleViolationException::rule(
                'BR-009',
                'لا يمكن تفعيل رسوم المعاينة (CFG-091) قبل تفعيل عروض السعر (CFG-090).',
            );
        }
    }
}
