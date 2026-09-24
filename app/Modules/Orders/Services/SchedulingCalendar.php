<?php

declare(strict_types=1);

namespace App\Modules\Orders\Services;

use App\Modules\Geography\Models\City;
use App\Modules\Settings\Enums\Cfg;
use App\Modules\Settings\Services\SettingsRepository;
use App\Support\Exceptions\BusinessRuleViolationException;
use Carbon\CarbonImmutable;

/**
 * ساعات الخدمة والفترات المجدولة — BR-017، CFG-020..CFG-022.
 * الأوقات تُخزَّن UTC وتُحسب بتوقيت المدينة (29، 20).
 */
final readonly class SchedulingCalendar
{
    public function __construct(private SettingsRepository $settings) {}

    /** BR-017 — `NOW` مسموح فقط داخل ساعات الخدمة. */
    public function isWithinServiceHours(City $city, ?CarbonImmutable $at = null): bool
    {
        $local = ($at ?? CarbonImmutable::now())->setTimezone($city->timezone);
        $from = $this->timeOf($local, Cfg::ServiceHoursFrom);
        $to = $this->timeOf($local, Cfg::ServiceHoursTo);

        return $local->betweenIncluded($from, $to);
    }

    public function assertNowIsAllowed(City $city, ?CarbonImmutable $at = null): void
    {
        if (! $this->isWithinServiceHours($city, $at)) {
            throw BusinessRuleViolationException::rule(
                'BR-017',
                'طلبات "الآن" متاحة داخل ساعات الخدمة فقط ('
                    .$this->settings->string(Cfg::ServiceHoursFrom).' – '
                    .$this->settings->string(Cfg::ServiceHoursTo).').',
            );
        }
    }

    /**
     * الفترات المتاحة ليوم بعينه بعد استبعاد ما بدأ أو اقترب (CFG-022).
     *
     * @return list<array{start: CarbonImmutable, end: CarbonImmutable}> بتوقيت UTC
     */
    public function availableSlots(City $city, CarbonImmutable $date, ?CarbonImmutable $now = null): array
    {
        $now ??= CarbonImmutable::now();
        $earliest = $now->addMinutes($this->settings->int(Cfg::MinLeadMinutesBeforeSlot));
        $localDate = $date->setTimezone($city->timezone)->startOfDay();

        $slots = [];

        foreach ($this->settings->array(Cfg::ScheduledSlots) as $slot) {
            $start = $this->applyTime($localDate, (string) ($slot['from'] ?? ''));
            $end = $this->applyTime($localDate, (string) ($slot['to'] ?? ''));

            if ($start === null || $end === null || $start->lt($earliest)) {
                continue;
            }

            $slots[] = ['start' => $start->utc(), 'end' => $end->utc()];
        }

        return $slots;
    }

    /** أقصى مدى للجدولة: اليوم حتى +7 أيام (BR-017، ASM-03). */
    public function schedulingHorizonDays(): int
    {
        return 7;
    }

    /**
     * يتحقق أن الفترة المطلوبة واحدة من فترات CFG-021 وتستوفي CFG-022 والمدى.
     *
     * @return array{start: CarbonImmutable, end: CarbonImmutable}
     */
    public function assertValidSlot(City $city, CarbonImmutable $slotStart, ?CarbonImmutable $now = null): array
    {
        $now ??= CarbonImmutable::now();

        if ($slotStart->gt($now->addDays($this->schedulingHorizonDays()))) {
            throw BusinessRuleViolationException::rule(
                'BR-017',
                'الجدولة متاحة حتى '.$this->schedulingHorizonDays().' أيام من اليوم.',
            );
        }

        foreach ($this->availableSlots($city, $slotStart, $now) as $slot) {
            if ($slot['start']->equalTo($slotStart)) {
                return $slot;
            }
        }

        throw BusinessRuleViolationException::rule(
            'BR-017',
            'الفترة المختارة غير متاحة: يجب أن تكون من الفترات المعتمدة وتبدأ بعد '
                .$this->settings->int(Cfg::MinLeadMinutesBeforeSlot).' دقيقة على الأقل.',
        );
    }

    private function timeOf(CarbonImmutable $local, Cfg $key): CarbonImmutable
    {
        return $this->applyTime($local->startOfDay(), (string) $this->settings->string($key))
            ?? $local->startOfDay();
    }

    private function applyTime(CarbonImmutable $localDay, string $time): ?CarbonImmutable
    {
        if (preg_match('/^(\d{1,2}):(\d{2})$/', $time, $m) !== 1) {
            return null;
        }

        return $localDay->setTime((int) $m[1], (int) $m[2]);
    }
}
