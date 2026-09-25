# تغييرات مطلوبة أو تمّت في ملفات مشتركة: تحويل أندرويد إلى XML

المالك يقرر في كل بند. وكيل أندرويد لا يعدّل الملفات المشتركة إلا في الجزء الذي يملكه (عمود أندرويد في `parity.json`، وجزء أندرويد في `tools/gen-design`).

## تمّت في جزء أندرويد المسموح له (للعلم)
| الملف | التغيير | السبب |
|---|---|---|
| `tools/gen-design` | حذف سطر واحد: `syncText(... generated/DesignTokens.kt ...)` واستبداله بتعليق. كود بناء `$kotlin` بقي كما هو (غير مستخدم) لتقليل التعديل على ملف مشترك | الخطوة 4: `DesignTokens.kt` كان توكنات Compose (Color/dp/sp)، ومحذوف مع Compose |
| `design/generated/manifest.json` (مولّد) | اختفى منه سطر `DesignTokens.kt` فقط، بعد تشغيل `tools/gen-design`. تأكدت بـ `diff` إن ده التغيير الوحيد | نتيجة البند السابق؛ `gen-design --check` ✅ |
| `design/parity.json` | عمود `android` للشاشات SCR-C01..C35 فقط: `file` و`symbol` بقوا بيشاوروا على الـ XML Views (`C01HomeView`…، `CustomerLoginScreen`…، `C30ExecutionQuoteView`/`C31AdditionalCostView`). أعمدة iOS والمكونات متلمستش | حذف ملفات Compose؛ `check-parity` ✅ |

## تمّت قبل قاعدة الملكية (الخطوتان 1 و2): للمراجعة
| الملف | التغيير | السبب | الحالة |
|---|---|---|---|
| `tools/gen-design` (الجزء المشترك، مخرج CSS) | لون `scrim` صار يُكتب RRGGBBAA بدل ARGB | خطأ في ترتيب القنوات في مخرج الويب | تمّ، ويحتاج موافقتك أو إرجاعه |
| `design/reference/android-compose/gallery/` | تجميد لقطات Compose المعتمدة للمعرض هدفًا للمقارنة | عشان تفضل موجودة بعد حذف Compose | تمّ، مجلد جديد |
| `design/reference/android-compose/screens/` (212 ملفًا) | **خطأ مني ومحتاج يتحذف:** نسخة أولى من لقطات الشاشات بأسماء غلط | كتبتها قبل ما أنتبه إن `design/` مشترك، والحذف اتمنع عليّ. المرجع الصحيح في `reports/xml-conversion/reference/compose-screens/` (233 لقطة) | **مطلوب منك:** `rm -r design/reference/android-compose/screens` |
| `iosapp/` (`bundle id` و`InfoPlist.xcstrings`) | `com.bremo.app` والاسم «بريمو» | DEC-048 (دفعة منفصلة بأمر المالك، مش جزء من التحويل) | تمّ، للعلم |

## مطلوبة (لم تُنفَّذ)
| الملف | التغيير | السبب |
|---|---|---|
| `design/fixtures/` | «مدينة نصر، القاهرة» ← «الحي الأول، دمياط الجديدة» | كانت ضمن خطة الخطوة 3، وبقت ملفًا مشتركًا. تُنفَّذ بقرارك أو بوكيل iOS، وبعدها يُعاد اعتماد اللقطات المتأثرة في المنصتين مرة واحدة |
| `docs/43-UI-BUILD-CONTRACT.md` | توثيق الانحرافين المعتمدين: الحقول الأربعة `EditText` بإطار مولّد بدل `TextInputLayout`، و`SelectableChip` بدل `Chip`. وأيضًا: مرجع لقطات الشاشات صار `reports/xml-conversion/reference/compose-screens/` | البرومبت يشترط `TextInputLayout` و`Chip`؛ السبب في REPORT.md |
| `docs/41-DELIVERY-PLAN.md` | حالة الدفعة ب: التحويل اكتمل (249/249، Compose محذوف) | متابعة الخطة |

## اختلاف أسماء أو مدخلات عن SwiftUI
لا يوجد. المكونات الـ 42 بنفس الأسماء والمدخلات والترتيب. الشاشات: `C01HomeView`… تقابل `C01HomeView` في iOS، والدخول `CustomerLoginScreen`/`CustomerRegisterScreen`/`CustomerEmailVerificationScreen`/`CustomerPasswordRecoveryScreen`، وC30/C31 `C30ExecutionQuoteView`/`C31AdditionalCostView`، كلها بنفس أسماء SwiftUI (`tools/check-parity` ✅: 42 مكوّنًا، 58 شاشة، 471 مفتاح نص).
