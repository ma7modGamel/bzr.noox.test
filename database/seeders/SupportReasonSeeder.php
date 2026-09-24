<?php

declare(strict_types=1);

namespace Database\Seeders;

use App\Modules\Support\Enums\SupportReasonType;
use App\Modules\Support\Models\SupportReason;
use Illuminate\Database\Seeder;

/** قيم بداية شائعة؛ insertOrIgnore يحافظ على أي تعديل أدخله فريق التشغيل. */
final class SupportReasonSeeder extends Seeder
{
    public function run(): void
    {
        $now = now();
        $rows = [
            [SupportReasonType::Dispute, 'WORK_QUALITY', 'جودة التنفيذ غير مرضية'],
            [SupportReasonType::Dispute, 'INCOMPLETE_WORK', 'العمل غير مكتمل'],
            [SupportReasonType::Dispute, 'UNAPPROVED_COST', 'تكلفة غير متفق عليها'],
            [SupportReasonType::Dispute, 'PROPERTY_DAMAGE', 'تلف في الممتلكات'],
            [SupportReasonType::Dispute, 'PAYMENT_ISSUE', 'مشكلة في الدفع أو المبلغ'],
            [SupportReasonType::Dispute, 'PROVIDER_CONDUCT', 'سلوك الفني'],
            [SupportReasonType::Dispute, 'OTHER', 'سبب آخر'],
            [SupportReasonType::ProviderReport, 'INAPPROPRIATE_BEHAVIOR', 'سلوك غير لائق'],
            [SupportReasonType::ProviderReport, 'HARASSMENT_OR_ABUSE', 'تحرش أو إساءة'],
            [SupportReasonType::ProviderReport, 'FRAUD_SUSPECTED', 'اشتباه احتيال'],
            [SupportReasonType::ProviderReport, 'OFF_PLATFORM_REQUEST', 'طلب تواصل أو دفع خارج التطبيق'],
            [SupportReasonType::ProviderReport, 'FALSE_INFORMATION', 'معلومات مهنية مضللة'],
            [SupportReasonType::ProviderReport, 'SAFETY_CONCERN', 'تهديد للسلامة'],
            [SupportReasonType::ProviderReport, 'OTHER', 'سبب آخر'],
        ];

        SupportReason::query()->insertOrIgnore(array_map(
            static fn (array $row, int $index): array => [
                'type' => $row[0]->value,
                'code' => $row[1],
                'label' => $row[2],
                'is_active' => true,
                'sort' => ($index % 7) + 1,
                'created_at' => $now,
                'updated_at' => $now,
            ],
            $rows,
            array_keys($rows),
        ));
    }
}
