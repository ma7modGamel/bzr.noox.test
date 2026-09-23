<?php

declare(strict_types=1);

namespace App\Modules\Pricing\Enums;

use Filament\Support\Contracts\HasLabel;

/** BR-042 — DEC-007. */
enum ProposalType: string implements HasLabel
{
    case ExecutionQuote = 'EXECUTION_QUOTE';
    case Materials = 'MATERIALS';
    case ExtraWork = 'EXTRA_WORK';

    public function getLabel(): string
    {
        return match ($this) {
            self::ExecutionQuote => 'عرض تنفيذ',
            self::Materials => 'خامات',
            self::ExtraWork => 'أعمال إضافية',
        };
    }

    /** المقترحات القابلة للسحب من الفني (12). */
    public function isWithdrawable(): bool
    {
        return $this !== self::ExecutionQuote;
    }
}
