<?php

declare(strict_types=1);

namespace App\Modules\Support\Actions;

use App\Modules\Identity\Models\User;
use App\Modules\Orders\Models\Order;
use App\Modules\Providers\Models\ProviderProfile;
use App\Modules\Support\Enums\ReportStatus;
use App\Modules\Support\Models\ProviderReport;

/** BR-121 — ينشئ ملفًا إداريًا مستقلًا بلا تعديل حالة الطلب أو الفني. */
final readonly class CreateProviderReportAction
{
    public function execute(
        User $reporter,
        Order $order,
        ProviderProfile $provider,
        string $reasonCode,
        ?string $description,
    ): ProviderReport {
        return ProviderReport::query()->create([
            'reporter_user_id' => $reporter->getKey(),
            'order_id' => $order->getKey(),
            'provider_profile_id' => $provider->getKey(),
            'reason_code' => $reasonCode,
            'description' => $description,
            'status' => ReportStatus::New,
        ]);
    }
}
