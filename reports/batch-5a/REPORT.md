# تقرير الدفعة 5أ — تسجيل الفني P01–P07

## بوابة الاعتماد

- صفحة المقارنة: [`provider-onboarding/comparison.html`](provider-onboarding/comparison.html).
- المطلوب: اعتماد 44 لقطة Android (400×800).
- لن تبدأ 5ب (P08) قبل اعتماد هذه الدفعة.

## التطابق

| الشاشة | Android XML | SwiftUI | المواصفة | الحالات Android | الحالات Swift Core |
|---|---|---|---|---:|---:|
| P01 دخول وضع الفني | `P01IntroView.kt` | `P01IntroView` | `SCR-P01.md` | 7 | 7 |
| P02 بيانات الفني | `P02ProfileView.kt` | `P02ProfileView` | `SCR-P02.md` | 7 | 7 |
| P03 إثبات الهوية | `P03IdentityView.kt` | `P03IdentityView` | `SCR-P03.md` | 5 | 5 |
| P04 الفئات والتخصصات | `P04CatalogView.kt` | `P04CatalogView` | `SCR-P04.md` | 7 | 7 |
| P05 مناطق العمل | `P05AreasView.kt` | `P05AreasView` | `SCR-P05.md` | 6 | 6 |
| P06 المستحقات والإرسال | `P06PayoutView.kt` | `P06PayoutView` | `SCR-P06.md` | 6 | 6 |
| P07 حالة الطلب | `P07StatusView.kt` | `P07StatusView` | `SCR-P07.md` | 6 | 6 |

الإجمالي: 44 حالة من الملفات نفسها في `design/fixtures/SCR-P01..P07`، و`design/parity.json` يربط المواصفات والمنصتين والاختبارات.

## الربط والانتقالات المغطاة

- `GET /provider/application`: يوجّه P01 إلى البداية أو P07 حسب حالة الملف و`available_actions`.
- `POST /media`: صورة الملف وصورتا الهوية، مع ملكية المستخدم وتخزين خاص للمستندات.
- `GET /catalog`، `GET /cities`، و`GET /cities/{city}/areas`: كل الفئات والتخصصات والمدن والمناطق من الخادم بلا قوائم عمل hard-coded.
- `GET /config.option_lists.provider_payout_methods`: طرق الاستلام وتسميات حقولها من الخادم.
- `POST /provider/application`: إرسال جديد أو إعادة إرسال المرفوض؛ يمنع تعديل `PENDING_REVIEW`.
- الانتقالات: P01→P02→P03→P04→P05→P06→P07، وP07→P02 عند `resubmit_provider_application`. انتقال `open_provider_home` ينتظر P08 في 5ب.
- انتقالات آلة الطلب `T-xx`: لا يوجد؛ P01–P07 رحلة انضمام/مراجعة للملف وليست انتقالات حالة طلب. أول انتقالات طلب تخص الفني تبدأ بعد P08.

## الجديد في الدفعة

- النصوص: مجموعة `provider.onboarding.*` و`provider.application.*` وإجراءات بدء/إرسال/إعادة إرسال/فتح وضع الفني.
- الرموز: لا رمز جديد؛ أعيد استخدام account/check/location/camera/orders.
- المكونات: لا مكوّن عام جديد؛ استخدمت مكتبة التصميم القائمة.
- API: `GET/POST /provider/application` وقائمة `provider_payout_methods` في `/config`.

## التحقق

| الفحص | النتيجة |
|---|---|
| API التسجيل | 10 اختبارات، 48 assertion — ناجحة |
| الحالات المشتركة | 44 Android + 44 Swift Core — ناجحة |
| Android compile/lint/Paparazzi | ناجح |
| Swift Core build/test على Linux | 10 اختبارات — ناجحة |
| swift-format + SwiftLint لكل `iosapp` | ناجح، صفر مخالفة |
| parity + design lint + generation check | ناجح؛ 544 مفتاح نص مشترك |
| OpenAPI generation/check | ناجح |

## الأسئلة المفتوحة

لا يوجد سؤال يمنع اعتماد 5أ.

## iOS غير المتحقق منه آليًا

- Swift Core، فك JSON، التحقق، الربط، والـAPI client مبنية ومختبرة على Linux.
- ملفات SwiftUI خضعت لـ swift-format وSwiftLint وفحص القيم البصرية المباشرة والتطابق.
- تجميع SwiftUI ولقطات iOS والمقارنة البكسلية لا يمكن تشغيلها على Linux، ومؤجلة لدفعة «تحقق iOS» النهائية حسب DEC-045.
