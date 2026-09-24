<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Orders\Enums\TimingType;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Enums\OperatingMode;
use App\Modules\Settings\Services\SettingsRepository;
use Carbon\CarbonImmutable;

/**
 * مهلات الطلب — تختلف جذريًا بين الوضعين (39):
 *
 * - وضع السوق: نافذة عروض (CFG-010 / CFG-010b) ثم مهلة اختيار (CFG-013 / CFG-013b).
 * - وضع الموظفين: لا نافذة عروض إطلاقًا؛ `selection_deadline_at` تصير **مهلة التعيين** (CFG-093).
 */
final readonly class OrderDeadlines
{
    public function __construct(private SettingsRepository $settings) {}

    /** @return array{offers_close_at: ?CarbonImmutable, selection_deadline_at: CarbonImmutable} */
    public function for(
        OperatingMode $mode,
        TimingType $timing,
        ?CarbonImmutable $slotStart,
        ?CarbonImmutable $publishedAt = null,
    ): array {
        $publishedAt ??= CarbonImmutable::now();

        return $mode === OperatingMode::Employee
            ? $this->assignmentDeadlines($timing, $slotStart, $publishedAt)
            : $this->offerDeadlines($timing, $slotStart, $publishedAt);
    }

    /** وضع الموظفين — CFG-093: الآن = ساعة من النشر، المجدول = بداية الفترة. */
    private function assignmentDeadlines(TimingType $timing, ?CarbonImmutable $slotStart, CarbonImmutable $publishedAt): array
    {
        $deadline = $timing === TimingType::Now
            ? $publishedAt->addMinutes($this->settings->int(Cfg::AssignmentDeadlineNowMinutes))
            : ($slotStart ?? $publishedAt);

        return ['offers_close_at' => null, 'selection_deadline_at' => $deadline];
    }

    /** وضع السوق — DEC-003، DEC-028. */
    private function offerDeadlines(TimingType $timing, ?CarbonImmutable $slotStart, CarbonImmutable $publishedAt): array
    {
        if ($timing === TimingType::Now) {
            $closeAt = $publishedAt->addMinutes($this->settings->int(Cfg::OffersWindowNowMinutes));

            return [
                'offers_close_at' => $closeAt,
                'selection_deadline_at' => $closeAt->addMinutes($this->settings->int(Cfg::SelectionGraceNowMinutes)),
            ];
        }

        $slotStart ??= $publishedAt;

        // حتى ساعة قبل الفترة، بحد أقصى 24 ساعة من النشر (CFG-010b)
        $beforeSlot = $slotStart->subMinutes($this->settings->int(Cfg::OffersWindowScheduledLeadMinutes));
        $maxWindow = $publishedAt->addHours($this->settings->int(Cfg::OffersWindowScheduledMaxHours));
        $closeAt = $beforeSlot->lt($maxWindow) ? $beforeSlot : $maxWindow;

        if ($closeAt->lt($publishedAt)) {
            $closeAt = $publishedAt;
        }

        return [
            'offers_close_at' => $closeAt,
            'selection_deadline_at' => $slotStart->subMinutes($this->settings->int(Cfg::SelectionLeadScheduledMinutes)),
        ];
    }

    /** لحظة تنبيه الإدارة لطلب بلا تعيين — CFG-092 (وضع الموظفين). */
    public function assignmentAlertAt(TimingType $timing, ?CarbonImmutable $slotStart, CarbonImmutable $publishedAt): CarbonImmutable
    {
        return $timing === TimingType::Now
            ? $publishedAt->addMinutes($this->settings->int(Cfg::AssignmentAlertNowMinutes))
            : ($slotStart ?? $publishedAt)->subMinutes($this->settings->int(Cfg::AssignmentAlertScheduledLeadMinutes));
    }
}
