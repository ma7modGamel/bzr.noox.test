# تقرير الدفعة 4ج — التواصل والسجل والدعم على الطلب

## بوابة الاعتماد

- صفحة المقارنة: [`customer-communication/comparison.html`](customer-communication/comparison.html) (يولّدها `tools/gen-communication-report`).
- المطلوب: اعتماد 51 لقطة Android (400×800) مبنية بـ Compose.
- **ملاحظة بعد قرار التحويل إلى XML Views (DEC-047):** هذه اللقطات، بعد اعتمادها، تصبح **الهدف** الذي تُقارن به لقطات XML في دفعة التحويل، ولن تُبنى أي شاشة جديدة بـ Compose.

## التطابق

| الشاشة | Compose | SwiftUI | المواصفة | fixture | حالات Android | حالات Swift Core |
|---|---|---|---|---|---:|---:|
| C01 الرئيسية (الوضعان) | `C01HomeScreen.kt` | `C01HomeView.swift` | `SCR-C01.md` | `SCR-C01/cases.json` | 5 | 5 |
| C18 الرسائل | `C18MessagesScreen.kt` | `C18MessagesView.swift` | `SCR-C18.md` | `SCR-C18/cases.json` | 6 | 6 |
| C19 المحادثة | `C19ChatScreen.kt` | `C19ChatView.swift` | `SCR-C19.md` | `SCR-C19/cases.json` | 8 | 8 |
| C25 طلباتي | `C25OrdersScreen.kt` | `C25OrdersView.swift` | `SCR-C25.md` | `SCR-C25/cases.json` | 7 | 7 |
| C26 تفاصيل طلب سابق | `C26OrderHistoryScreen.kt` | `C26OrderHistoryView.swift` | `SCR-C26.md` | `SCR-C26/cases.json` | 8 | 8 |
| C27 فتح مشكلة ومتابعتها | `C27DisputeScreen.kt` | `C27DisputeView.swift` | `SCR-C27.md` | `SCR-C27/cases.json` | 10 | 10 |
| C28 الإبلاغ عن فني | `C28ProviderReportScreen.kt` | `C28ProviderReportView.swift` | `SCR-C28.md` | `SCR-C28/cases.json` | 7 | 7 |

أضافت 4ج 51 حالة مشتركة، وكل حالة لها لقطة.

## الانتقالات والربط

- C18/C19: `GET/POST /conversations`، و`GET/POST /conversations/{id}/messages`. الحجب (أرقام وروابط) يتم على الخادم وفق DEC-013، والتطبيق يعرض النسخة المحجوبة `masked` كما هي. المحادثة للقراءة فقط بعد انتهاء نافذة 18.
- C25: `GET /orders` بتبويبين (الحالي/السابق) وترقيم صفحات.
- C26: يعرض `termination.reason_label` من الخادم، ولا يترجم أكواد الأسباب داخل التطبيق (31).
- C27: `GET/POST /orders/{order}/disputes`، ويمهد لـ T-22؛ الأسباب من `GET /config` ويديرها التشغيل من اللوحة.
- C28: `POST /providers/{provider}/reports` (ASM-11: ملف منفصل عن النزاع)، مربوط بالطلب (`order_id`).
- كل زر تنفيذي يأتي من `available_actions`.

## الجديد في الدفعة

- نصوص: مجموعات `messages.*` و`chat.*` و`orders.*` و`dispute.*`، وامتدادات `order.*` لسجل الطلب.
- رموز: لا يوجد رمز جديد.
- مكونات: لا يوجد مكوّن عام جديد؛ بُنيت الشاشات من `ChatBubble` و`ChatInput` و`OrderCard` و`RadioCard` و`TextAreaField` و`MediaThumb` و`SortChips`.
- خادم: وحدة أسباب الدعم في اللوحة (`SupportReasons`)، وعمود `order_id` في بلاغات الفنيين.

## التحقق في هذه الجلسة

| الفحص | النتيجة |
|---|---|
| اختبارات الخادم | 143 اختبارًا، 1017 تأكيدًا — ناجحة |
| `verifyPaparazziDebug` (app + core:design) | ناجح على كل اللقطات المسجلة |
| `swift build` + `swift test` لـ `iosapp/Core` (Docker `swift:6.0`) | ناجح |
| `tools/check-parity` | 42 مكوّنًا، 58 شاشة، 471 مفتاح نص |
| `tools/lint-design` و`tools/gen-design --check` | ناجحان |

## أسئلة مفتوحة

لا يوجد.

## iOS غير المتحقق منه بصريًا

- تم: بناء واختبار Swift Core على Linux بنفس fixtures.
- مؤجل حتى دفعة «تحقق iOS» (DEC-045): البناء الكامل لـ SwiftUI على Xcode، والرسم الفعلي، واللقطات، والمقارنة البكسلية مع مراجع Android.
