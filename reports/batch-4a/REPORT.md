# تقرير الدفعة 4أ — أثناء الزيارة

## بوابة الاعتماد

- صفحة المقارنة: [`customer-visit/comparison.html`](customer-visit/comparison.html).
- المطلوب: اعتماد 22 لقطة Android (400×800).
- لن تبدأ 4ب قبل اعتماد المالك لهذه الدفعة.

## التطابق

| الشاشة | Compose | SwiftUI | المواصفة | fixture | حالات Android | حالات Swift Core |
|---|---|---|---|---|---:|---:|
| C20 إلغاء الطلب | `C20CancellationScreen.kt` | `C20CancellationView.swift` | `SCR-C20.md` | `SCR-C20/cases.json` | 5 | 5 |
| C30 عرض التنفيذ | `ProposalDecisionScreen.kt` | `ProposalDecisionViews.swift` | `SCR-C30.md` | `SCR-C30/cases.json` | 6 | 6 |
| C31 التكلفة الإضافية | `ProposalDecisionScreen.kt` | `ProposalDecisionViews.swift` | `SCR-C31.md` | `SCR-C31/cases.json` | 7 | 7 |
| C09 حالات الزيارة | `C09TrackingScreen.kt` | `C09TrackingView.swift` | `SCR-C09.md` | `SCR-C09/cases.json` | 11 | 11 |

أضافت 4أ 20 حالة مشتركة: 18 لـ C20/C30/C31، وحالتي C09 الجديدتين `IN_PROGRESS` و`AWAITING_QUOTE_APPROVAL`. الإجمالي الحالي لرحلة العميل: 99 حالة في كل منصة.

## الانتقالات المغطاة من الواجهة

- C20 ينفّذ `cancelOrder` لـ T-04 وT-08 وT-09، مع سبب إلزامي و`expected_version`.
- C30 ينفّذ الموافقة T-14، والرفض T-15 أو T-28 حسب رسوم المعاينة.
- C31 يربط موافقة/رفض المقترح بالإجراءين المتاحين من الخادم.
- C09 يعرض نتيجة T-10 وT-11 وT-12، وحالة T-22 بإيقاف الشريط عند آخر مرحلة و`on_hold`.

## الربط والاختبار

- الأزرار التنفيذية تظهر فقط من `available_actions`.
- C20 مربوط بـ `POST /orders/{id}/cancel`، وC30/C31 بـ `GET /orders/{id}/proposals` و`POST /orders/{id}/proposals/{proposal}/decide`.
- الخادم يرجع `projected_total`، والمنصتان تنسقانه بمفتاح العملة المشترك.
- `design/parity.json` يربط المواصفات وملفات المنصتين وfixtures، وفحصه ناجح.

## الجديد في الدفعة

- نصوص: مجموعتا `cancel.*` و`proposal.*`، ومفتاحا `action.approve_proposal` و`action.reject_proposal`.
- رموز: لا يوجد رمز جديد؛ أُعيد استخدام `orders` و`info` و`check` و`image` و`warning`.
- مكونات: لا يوجد مكوّن عام جديد. أضيف دعم `actionState` لـ`AppBottomSheet`، وأصبح زر `SummaryCard` الجانبي اختياريًا في المنصتين.

## أسئلة مفتوحة

لا يوجد.

## iOS غير المتحقق منه بصريًا

- تم: بناء واختبار Swift Core على Linux، و`swift-format`، وSwiftLint على كل `iosapp`، وsyntax لملفات SwiftUI، وفحص القيم البصرية الصلبة.
- مؤجل حتى دفعة «تحقق iOS»: البناء النوعي الكامل لـSwiftUI على Xcode، الرسم الفعلي، اللقطات، والمقارنة البكسلية مع Android.
