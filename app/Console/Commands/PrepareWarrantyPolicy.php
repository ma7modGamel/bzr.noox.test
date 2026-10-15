<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Modules\Content\Enums\LegalPageSlug;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

final class PrepareWarrantyPolicy extends Command
{
    protected $signature = 'content:prepare-warranty-policy';

    protected $description = 'Prepare a draft of the 14-day replacement technician warranty while preserving published versions';

    public function handle(): int
    {
        $sections = [
            'ar' => '<h2>ضمان الإصلاح لمدة 14 يومًا</h2><p>للإصلاح المنفّذ ضمان لمدة 14 يومًا من تاريخ إنهاء الإصلاح. عند عودة العطل يرسل العميل طلب ضمان مرتبطًا بالطلب الأصلي من التطبيق. تراجع الإدارة البلاغ وتتواصل مع العميل لتنسيق زيارة فني مختلف عن الفني الذي نفّذ الإصلاح الأول. الضمان يشمل الزيارة والمصنعية فقط، وهما مجانًا. الخامات وقطع الغيار خارج الضمان؛ يعرض الفني تكلفتها ويوافق العميل عليها داخل التطبيق قبل التنفيذ. عند عدم الحاجة إلى خامات مدفوعة تُغلق الزيارة بلا دفع، وعند وجود خامات موافق عليها يدفع العميل قيمتها فقط. يبقى سجل الطلب والمدفوعات الأصلية محفوظًا، ولا يبدأ ضمان جديد من زيارة الضمان. نافذة النزاع مستقلة عن مدة الضمان.</p>',
            'en' => '<h2>14-day repair warranty</h2><p>Completed repairs have a 14-day warranty from repair completion. If the fault returns, the customer submits a warranty request linked to the original order in the app. Administration reviews the report and contacts the customer to arrange a visit by a different technician. The warranty covers the visit and labor only, both free of charge. Materials and spare parts are excluded; the technician submits their price for customer approval in the app before use. A visit with no payable materials closes without payment; otherwise the customer pays only for approved materials. The original order and payment history are preserved, and a warranty visit does not restart the warranty period. The dispute window is separate from the warranty period.</p>',
        ];
        $created = DB::transaction(function () use ($sections): int {
            $page = LegalPage::query()->where('slug', LegalPageSlug::Terms->value)->lockForUpdate()->first();
            if ($page === null) {
                return 0;
            }
            $count = 0;
            foreach ($sections as $locale => $section) {
                $latest = $page->versions()->where('locale', $locale)->first();
                if ($latest === null) {
                    continue;
                }
                $pattern = $locale === 'ar'
                    ? '/<h2>ضمان الإصلاح(?: لمدة 14 يومًا)?<\/h2>\s*<p>.*?<\/p>/us'
                    : '/<h2>14-day repair warranty<\/h2>\s*<p>.*?<\/p>/s';
                $body = preg_replace($pattern, $section, $latest->body, count: $replacements);
                $body = $replacements > 0 ? $body : $latest->body.$section;
                $body = str_replace('المحادثة داخل التطبيق، ويظهر رقم الهاتف للطرفين بعد تأكيد الطلب ولمدة محددة بعد إغلاقه.', 'التواصل بين العميل والفني بالرسائل داخل التطبيق فقط. تتولى الإدارة الاتصال بالعميل عند الحاجة ومتابعة حالات الطلب.', $body);
                $body = str_replace('الرقم المرجعي للتحويل وصورة الإيصال إن أُرفقت', 'الرقم المرجعي للتحويل وصورة الإيصال الإلزامية', $body);
                if ($body === $latest->body) {
                    continue;
                }
                $page->versions()->create([
                    'version' => ((int) $page->versions()->max('version')) + 1,
                    'locale' => $locale, 'title' => $latest->title,
                    'body' => $body, 'status' => LegalPageStatus::Draft,
                    'requires_legal_review' => true,
                ]);
                $count++;
            }

            return $count;
        });
        $this->info("Prepared {$created} policy draft(s).");

        return self::SUCCESS;
    }
}
