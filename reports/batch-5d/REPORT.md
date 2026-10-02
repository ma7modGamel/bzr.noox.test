# تقرير الدفعة 5د — تقييم الفني ومستحقاته وملفه

## بوابة الاعتماد

- صفحة المقارنة: [`provider-workspace/comparison.html`](provider-workspace/comparison.html).
- اعتمد المالك 38 لقطة Android (400×800) في 2026-09-25: مداخل P08، منظور الفني في C19، وكل متغيرات P17–P19.
- أُغلقت بوابة 5د، وأصبح بدء 5هـ (P09–P11 لوضع السوق) مسموحًا.

## التطابق

| الشاشة | Android XML | SwiftUI | المواصفة | حالات Android | حالات Swift Core |
|---|---|---|---|---:|---:|
| P08 مداخل مساحة الفني | `P08HomeView.kt` | `P08HomeView` | `SCR-P08.md` | 7 | 7 |
| C19 المحادثة بمنظور الفني | `C19ChatView.kt` | `C19ChatView` | `SCR-C19.md` | 10 | 10 |
| P17 تقييم العميل | `P17CustomerRatingView.kt` | `P17CustomerRatingView` | `SCR-P17.md` | 7 | 7 |
| P18 المستحقات | `P18EarningsView.kt` | `P18EarningsView` | `SCR-P18.md` | 6 | 6 |
| P19 ملف الفني ومعرضه | `P19ProfileView.kt` | `P19ProfileView` | `SCR-P19.md` | 8 | 8 |

`design/parity.json` يربط كل شاشة بالمواصفة وملفات المنصتين ومدخلات العرض والـfixtures نفسها. إجمالي صفحة 5د: 38 حالة في Android و38 حالة في Swift Core. إجمالي رحلة الفني المنفذة حتى الآن، بما فيها منظور الفني في C19: 123 حالة مشتركة في كل منصة. لم تتغير الحالات المشتركة السابقة كي تنجح الاختبارات.

## الربط والانتقالات المغطاة

- P08 يفتح الرسائل والمستحقات والملف فقط من `open_messages` و`open_earnings` و`open_provider_profile` في `available_actions`.
- C19 يعيد نفس شاشة المحادثة بدور `PROVIDER`: الطرف المقابل هو العميل، بلا شارة توثيق، والإرسال/الصورة وحالة القراءة من استجابة الخادم.
- P17 يظهر بعد وصول الطلب إلى `CLOSED` عبر T-20 أو T-21 أو T-23 أو T-25 أو T-28، ويرسل تقييم العميل مرة واحدة عند `rate_customer`. إرسال التقييم لا يغيّر حالة الطلب، لذلك لا يضيف انتقال T جديدًا.
- P18 يحسب الموظف والسوق من الطلبات المغلقة والتسويات؛ النقدي والإلكتروني يعالجان وفق وضع التشغيل. لا يوجد انتقال T جديد.
- P19 يحدّث الحقول المسموحة فقط، ويرفع الصورة والمعرض ويحذف العنصر المملوك بتأكيد. رابط الدعم يعيد استخدام C29 في المنصتين ويظهر فقط عند `contact_support`. لا يوجد انتقال T جديد.

## الجديد في الدفعة

- النصوص: `provider.rating.*` و`provider.earnings.*` و`provider.profile.*` و`action.open_messages` و`action.open_earnings` و`action.open_provider_profile` و`action.contact_support` و`stat.response_speed`.
- الرموز: لا رمز جديد؛ أعيد استخدام النجمة والطلبات والساعة والصورة والمعلومات.
- المكونات: لا مكوّن عام جديد؛ أعيد استخدام `ProviderHeader` و`StatRow` و`RatingRow` و`SummaryCard` و`MediaThumb` و`AppBottomSheet` وحقول وأزرار النظام.
- API: `GET /provider/earnings`، `GET/PATCH /provider/profile`، `POST /provider/portfolio`، `DELETE /provider/portfolio/{portfolio}`، مع جعل المحادثة تعيد العميل للطرف الفني.
- متغيرات البيئة: لا يوجد؛ لم يعدّل `.env` ولا `.env.example` في 5د.

## التحقق

| الفحص | النتيجة |
|---|---|
| API مساحة الفني | 7 اختبارات تغطي الصلاحيات، الملف، المعرض، الموظف/السوق، والمحادثة — ناجحة |
| تقييم العميل في API | اختبار النجاح ومنع التكرار في `BatchTwoApiTest` — ناجح |
| حالات 5د المعروضة | 38 Android + 38 Swift Core من fixtures واحدة — ناجحة |
| كل حالات الفني المشتركة | 123 Android + 123 Swift Core — ناجحة بلا تعديل للحالات السابقة |
| Android build/Paparazzi | البناء ناجح؛ 38/38 لقطة مرجعية ومطابقة |
| Swift Core على Linux | البناء و11 اختبارًا ناجحة، وتشمل فك JSON والمنطق المشترك |
| swift-format + SwiftLint لكل `iosapp` | ناجح، صفر مخالفة |
| parity + design lint + generation check | ناجح؛ 42 مكوّنًا، 58 شاشة، 644 مفتاح نص مشترك |
| OpenAPI generation/check | ناجح؛ المسارات مطابقة للعقد |

## الأسئلة المفتوحة

لا يوجد سؤال يمنع اعتماد 5د.

## iOS غير المتحقق منه آليًا

- Swift Core، API، فك JSON، التحويل لحالات العرض، والتحقق من الـfixtures مبنية ومختبرة على Linux.
- ملفات SwiftUI خضعت لـswift-format وSwiftLint وفحص القيم البصرية المباشرة والتطابق.
- تجميع SwiftUI ورسمه، PhotosPicker، انتقال P19 إلى C29 على جهاز، ولقطات iOS والمقارنة البكسلية مؤجلة لدفعة «تحقق iOS» النهائية حسب DEC-045.
