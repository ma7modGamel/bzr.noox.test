# تقرير الدفعة 5ب — رئيسية الفني P08

## بوابة الاعتماد

- صفحة المقارنة: [`provider-home/comparison.html`](provider-home/comparison.html).
- المطلوب: اعتماد 7 لقطات Android (400×800).
- لن تبدأ 5ج (P12–P16 + P20–P21) قبل اعتماد هذه الدفعة.

## التطابق

| الشاشة | Android XML | SwiftUI | المواصفة | حالات Android | حالات Swift Core |
|---|---|---|---|---:|---:|
| P08 رئيسية الفني | `P08HomeView.kt` | `P08HomeView` | `SCR-P08.md` | 7 | 7 |

`design/parity.json` يربط الشاشة بالمواصفة، الملفين، المنطق المشترك، والـfixture نفسه. أسماء مدخلات الشاشة متطابقة: `state` و`onAvailabilityChange` و`onAction` و`onBack`.

## الربط والانتقالات المغطاة

- `GET /provider/home`: مصدر واحد لوضع التشغيل، `available_now`، الطلب المعيّن، و`available_actions`.
- `PUT /provider/availability`: حفظ «متاح الآن» للفني النشط فقط وإعادة حمولة الرئيسية المحدثة.
- وضع الموظفين يخفي طلبات السوق وحظر المستحقات حتى لو كان للفني رصيد معلّق؛ التعيين يظل من الإدارة فقط وبلا ورديات.
- زر تفاصيل الطلب يظهر فقط عند `open_assigned_order`. وجهته P12، وسيُربط الانتقال النهائي عند بناء P12 في 5ج بعد اعتماد هذه البوابة.
- انتقالات آلة الطلب: P08 لا تنفذ انتقال `T-xx`؛ تعرض نتيجة التعيين الإداري T-27 وتفتح الطلب المعيّن فقط.

## الجديد في الدفعة

- النصوص: `provider.home.*` و`action.open_assigned_order`.
- الرموز: لا رمز جديد؛ أُعيد استخدام check وempty وchevron وshield.
- المكونات: لا مكوّن عام جديد؛ أُعيد استخدام `CheckRow` و`InfoBanner` و`OrderCard` و`EmptyState` و`PrimaryButton`.
- API: `GET /provider/home` و`PUT /provider/availability`.
- متغيرات البيئة: لا يوجد، ولم يُعدّل `.env` أو `.env.example`.

## التحقق

| الفحص | النتيجة |
|---|---|
| API الرئيسية والإتاحة | 9 اختبارات، 38 assertion — ناجحة |
| حالات P08 المشتركة | 7 Android + 7 Swift Core — ناجحة من fixture واحدة |
| كل حالات الفني المشتركة P01–P08 | 51 Android + 51 Swift Core — ناجحة |
| Android compile/lint/Paparazzi | ناجح؛ 7/7 لقطات مرجعية |
| Swift Core build/test على Linux | 11 اختبارًا — ناجحة، ومنها PUT وفك JSON والمنطق المشترك |
| swift-format + SwiftLint لكل `iosapp` | ناجح، صفر مخالفة |
| parity + design lint + generation check | ناجح؛ 42 مكوّنًا، 58 شاشة، 569 مفتاح نص مشترك |
| OpenAPI generation/check | ناجح؛ المساران موجودان في العقد المولّد |

## الأسئلة المفتوحة

لا يوجد سؤال يمنع اعتماد 5ب.

## iOS غير المتحقق منه آليًا

- Swift Core، نموذج P08، فك JSON، PUT، تحويل الاستجابة لحالة العرض، والfixture مبنية ومختبرة على Linux.
- ملف SwiftUI خضع لـswift-format وSwiftLint وفحص القيم البصرية المباشرة والتطابق.
- تجميع SwiftUI ورسمه ولقطات iOS والمقارنة البكسلية مؤجلة لدفعة «تحقق iOS» النهائية حسب DEC-045.
