# تقرير الدفعة 5ج — تنفيذ طلب الفني

## بوابة الاعتماد

- صفحة المقارنة: [`provider-execution/comparison.html`](provider-execution/comparison.html).
- المطلوب: اعتماد 41 لقطة Android (400×800) للشاشات P12–P16 وP20–P21.
- لن تبدأ 5د (P17–P19) قبل اعتماد هذه الدفعة.

## التطابق

| الشاشة | Android XML | SwiftUI | المواصفة | حالات Android | حالات Swift Core |
|---|---|---|---|---:|---:|
| P12 الطلب المؤكد | `P12ConfirmedOrderView.kt` | `P12ConfirmedOrderView` | `SCR-P12.md` | 5 | 5 |
| P13 في الطريق | `P13OnTheWayView.kt` | `P13OnTheWayView` | `SCR-P13.md` | 5 | 5 |
| P14 في الموقع | `P14ArrivedView.kt` | `P14ArrivedView` | `SCR-P14.md` | 6 | 6 |
| P15 جاري التنفيذ | `P15InProgressView.kt` | `P15InProgressView` | `SCR-P15.md` | 5 | 5 |
| P16 انتظار التحصيل | `P16AwaitingPaymentView.kt` | `P16AwaitingPaymentView` | `SCR-P16.md` | 5 | 5 |
| P20 العرض/الإضافة | `P20ProposalView.kt` | `P20ProposalView` | `SCR-P20.md` | 8 | 8 |
| P21 الاعتذار/التعذر | `P21UnableView.kt` | `P21UnableView` | `SCR-P21.md` | 7 | 7 |

`design/parity.json` يربط الشاشات السبع بالمواصفات، ملفات المنصتين، ومدخلات العرض والـfixtures نفسها. الإجمالي في 5ج: 41 حالة في Android و41 حالة في Swift Core؛ وإجمالي حالات الفني P01–P21 المنفذة حتى الآن: 92 حالة في كل منصة.

## الربط والانتقالات المغطاة

- P12: بدء التحرك T-05، والاعتذار قبل التحرك T-06/T-07.
- P13: إثبات الوصول T-10، مع موقع فعلي على الجهاز وفتح الملاحة الخارجية.
- P14: بدء التنفيذ T-11، إرسال عرض التنفيذ T-12، إغلاق المعاينة المجانية T-28، والتعذر/عدم حضور العميل T-16.
- P15: الإنهاء T-17، والتعذر T-18، وإضافة تكلفة أثناء التنفيذ عبر P20.
- P16: تأكيد التحصيل النقدي T-19، مع منع الإجراء عندما تكون قناة الدفع إلكترونية.
- P20: الأنواع ودليل الأسعار من الخادم، والتحقق من النطاق، ورفع صورة حقيقية وربطها بالعرض ذريًا.
- P21: أسباب الاعتذار/التعذر من `/config` بلا قوائم مكتوبة في التطبيق، مع تحقق السبب والملاحظة ووقت عدم الحضور.
- كل إجراء mutating يستخدم `available_actions` و`expected_version`، والشاشة تعاد بناؤها من استجابة الخادم.

## الجديد في الدفعة

- النصوص: مفاتيح `provider.order.*` و`provider.price_guide.*` و`provider.proposal.*` و`provider.unable.*` و`provider.no_show.*` ومفاتيح الأفعال المقابلة.
- الرموز: لا رمز جديد؛ أعيد استخدام رموز الطلب والموقع والتحذير والصورة والهاتف والمحادثة.
- المكونات: لا مكوّن عام جديد؛ أعيد استخدام `ProviderHeader` و`StatusStepper` و`InfoBanner` و`AppTextField` و`SelectableChip` والأزرار المعيارية.
- API: `provider_cancellation_reasons` و`proposal_types` في `/config`، سياق التنفيذ ودليل السعر في مورد الطلب، و`photo_media_id` في إرسال العرض.
- متغيرات البيئة: لا يوجد؛ لم يعدّل `.env` أو `.env.example` في 5ج.

## التحقق

| الفحص | النتيجة |
|---|---|
| API تنفيذ الفني | 6 اختبارات، 36 assertion — ناجحة |
| حالات 5ج المشتركة | 41 Android + 41 Swift Core — ناجحة من fixtures واحدة |
| كل حالات الفني المشتركة | 92 Android + 92 Swift Core — ناجحة |
| Android compile/lint/Paparazzi | ناجح؛ 41/41 لقطة مرجعية ومطابقة |
| Swift Core build/test على Linux | 11 اختبارًا — ناجحة، وتشمل فك JSON والمنطق المشترك |
| swift-format + SwiftLint لكل `iosapp` | ناجح، صفر مخالفة |
| parity + design lint + generation check | ناجح؛ 42 مكوّنًا، 58 شاشة، 610 مفاتيح نص مشتركة |
| OpenAPI generation/check | ناجح؛ العقد مطابق للمسارات |

## الأسئلة المفتوحة

- عقد الشاشات لا يعرّف شاشة محادثة خاصة بالفني رغم ظهور `chat` في `available_actions`. الزر ظاهر من الخادم، لكن لم أربطه تخمينًا: هل يعيد الفني استخدام SCR-C19 بمنظور العميل، أم تريد شاشة فني مستقلة برقم جديد؟ الاتصال والملاحة يعملان الآن؛ ربط المحادثة ينتظر هذا القرار.

## iOS غير المتحقق منه آليًا

- Swift Core، API، فك JSON، التحويل لحالات العرض، والتحقق من الـfixtures مبنية ومختبرة على Linux.
- ملفات SwiftUI خضعت لـswift-format وSwiftLint وفحص القيم البصرية المباشرة والتطابق.
- تجميع SwiftUI ورسمه، CoreLocation/MapKit/PhotosPicker على جهاز، ولقطات iOS والمقارنة البكسلية مؤجلة لدفعة «تحقق iOS» النهائية حسب DEC-045.
