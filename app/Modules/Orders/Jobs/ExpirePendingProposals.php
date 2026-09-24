<?php

declare(strict_types=1);

namespace App\Modules\Orders\Jobs;

use App\Modules\Pricing\Actions\DecideProposalAction;
use App\Modules\Pricing\Enums\ProposalStatus;
use App\Modules\Pricing\Models\PriceProposal;
use App\Support\Exceptions\DomainException;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Queue\Queueable;

/**
 * انتهاء مهلة مقترح السعر (CFG-040) — والانتهاء يساوي رفضًا (BR-041، DEC-029).
 * عرض التنفيذ المنتهي ينقل الطلب إلى الدفع (T-15) أو يغلقه بلا مبلغ (T-28، BR-056).
 */
final class ExpirePendingProposals implements ShouldQueue
{
    use Queueable;

    public function handle(DecideProposalAction $decide): int
    {
        $count = 0;

        PriceProposal::query()
            ->with('order')
            ->where('status', ProposalStatus::Pending->value)
            ->where('expires_at', '<=', now())
            ->orderBy('id')
            ->chunkById(100, function ($proposals) use ($decide, &$count): void {
                foreach ($proposals as $proposal) {
                    try {
                        $decide->expire($proposal->order, $proposal);
                        $count++;
                    } catch (DomainException) {
                        // حُسم المقترح أو تغيّرت حالة الطلب بين الجلب والتنفيذ
                    }
                }
            });

        return $count;
    }
}
