<?php

declare(strict_types=1);

namespace App\Console\Commands;

use App\Modules\Content\Actions\PublishLegalPageVersionAction;
use App\Modules\Content\Enums\LegalPageStatus;
use App\Modules\Content\Models\LegalPage;
use App\Modules\Identity\Models\Admin;
use Illuminate\Console\Command;
use Illuminate\Support\Facades\DB;

final class PublishStaffPolicies extends Command
{
    protected $signature = 'content:publish-staff-policies {--admin= : Existing active super administrator ID} {--preview : Show the final text without writing}';

    protected $description = 'Publish Arabic staff-mode policies with the approved warranty, messaging, and arrival workflow';

    public function handle(PublishLegalPageVersionAction $publish): int
    {
        $admin = Admin::query()->find((int) $this->option('admin'));
        if (! $this->option('preview') && ($admin === null || ! $admin->isSuper() || ! $admin->is_active)) {
            $this->error('An existing active super administrator is required.');

            return self::FAILURE;
        }
        if (! $this->option('preview')) {
            $this->call('content:prepare-warranty-policy');
        }
        foreach (['terms', 'privacy', 'cancellation-policy'] as $slug) {
            $page = LegalPage::query()->where('slug', $slug)->firstOrFail();
            $latest = $page->versions()->where('locale', 'ar')->firstOrFail();
            $body = $latest->body;
            $body = str_replace('الرقم المرجعي للتحويل وصورة الإيصال إن أُرفقت', 'الرقم المرجعي للتحويل وصورة الإيصال الإلزامية', $body);
            $body = str_replace('يرى الفني المعيَّن العنوان التفصيلي، ويظهر رقم الهاتف للطرفين حتى 24 ساعة بعد الإغلاق.', 'يرى الفني المعيَّن العنوان التفصيلي اللازم للزيارة، ولا يعرض التطبيق رقم هاتف أي طرف للطرف الآخر. التواصل بين العميل والفني بالرسائل فقط، والإدارة تتولى الاتصال التشغيلي بالعميل.', $body);
            $body = str_replace('في المحادثة قبل التأكيد تُحجب الأرقام والروابط والبريد تلقائيًا', 'في المحادثة تُحجب الأرقام والروابط والبريد تلقائيًا طوال رحلة الطلب', $body);
            $body = str_replace('يعرض اسم الفني الأول وحالة الطلب فقط، بلا عنوان ولا خريطة ولا هاتف ولا سعر، وينتهي بانتهاء الطلب.', 'يعرض رقم الطلب وحالة الزيارة واسم الفني الأول والمنطقة فقط، بلا اسم العميل أو عنوان تفصيلي أو موقع حي أو هاتف أو سعر. ينتهي خلال 24 ساعة كحد أقصى، ويتوقف بانتهاء الزيارة أو سحب الرابط.', $body);
            $body = str_replace('بعد الوصول يمكن للفني الإبلاغ عن تعذّر التنفيذ، أو عن عدم وجود العميل بعد مدة الانتظار المحددة، ويُلغى الطلب دون أي مبلغ.', 'بعد الوصول يمكن للفني الإبلاغ عن تعذّر التنفيذ وفق المسار المتاح. غياب العميل يُبلّغ للإدارة بعد مدة الانتظار؛ البلاغ وحده لا يلغي الطلب. تتولى الإدارة الاتصال بالعميل وتسجيل النتيجة ثم تأكيد استمرار الزيارة أو إلغاء الطلب بلا مبلغ.', $body);
            if ($slug === 'terms' && ! str_contains($body, '<h2>تأكيد وصول الفني</h2>')) {
                $body .= '<h2>تأكيد وصول الفني</h2><p>أثناء تحرك الفني يظهر للعميل كود وصول خاص بالزيارة. يعطيه العميل للفني عند وصوله للتحقق داخل التطبيق. الكود محدود الصلاحية والمحاولات، ويمكن تحديثه من التطبيق. عند غياب العميل أو تعذّر إظهار الكود يبلّغ الفني الإدارة؛ تتحقق الإدارة وتتولى الاتصال وتسجل سبب أي استثناء قبل تأكيد الوصول. هذا الإجراء لا يمثل موافقة على تكلفة الإصلاح ولا إثبات انتهائه.</p>';
            }
            if ($this->option('preview')) {
                $this->line($slug.': '.$body);

                continue;
            }
            DB::transaction(function () use ($page, $latest, $body, $publish, $admin, $slug): void {
                $page->newQuery()->whereKey($page->id)->lockForUpdate()->firstOrFail();
                if ($latest->status === LegalPageStatus::Published && $body === $latest->body) {
                    $this->info($slug.': already published, version '.$latest->version);

                    return;
                }
                $version = $latest;
                if ($body !== $latest->body) {
                    $version = $page->versions()->create(['version' => (int) $page->versions()->max('version') + 1,
                        'locale' => 'ar', 'title' => $latest->title, 'body' => $body, 'status' => LegalPageStatus::Draft,
                        'requires_legal_review' => true, 'created_by' => $admin->id]);
                }
                $published = $publish->execute($version, $admin);
                $this->info($slug.': published version '.$published->version);
            });
        }

        return self::SUCCESS;
    }
}
