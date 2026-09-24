# تقرير الدفعة 4د — الحساب والدعم والشروط

## بوابة الاعتماد

- صفحة المقارنة: [`customer-account-support/comparison.html`](customer-account-support/comparison.html) (يولّدها `tools/gen-account-support-report`).
- المطلوب: اعتماد 30 لقطة Android (400×800) مبنية بـ Compose.
- **بعد قرار التحويل إلى XML Views (DEC-047):** هذه اللقطات، بعد اعتمادها، تصبح هدف المقارنة للقطات XML. أي تغيير في المحتوى بسبب قرارات هذه الجولة (صفحات قانونية من اللوحة في C34، ودمياط الجديدة) يُعاد اعتماده دفعة واحدة ضمن دفعة التحويل.

## التطابق

| الشاشة | Compose | SwiftUI | المواصفة | fixture | حالات Android | حالات Swift Core |
|---|---|---|---|---|---:|---:|
| C29 المساعدة والدعم | `C29HelpScreen.kt` | `C29HelpView.swift` | `SCR-C29.md` | `SCR-C29/cases.json` | 6 | 6 |
| C32 الإشعارات | `C32NotificationsScreen.kt` | `C32NotificationsView.swift` | `SCR-C32.md` | `SCR-C32/cases.json` | 5 | 5 |
| C33 حسابي والأمان | `C33AccountSettingsScreen.kt` | `C33AccountSettingsView.swift` | `SCR-C33.md` | `SCR-C33/cases.json` | 9 | 9 |
| C34 الشروط وسياسة الإلغاء | `C34TermsScreen.kt` | `C34TermsView.swift` | `SCR-C34.md` | `SCR-C34/cases.json` | 4 | 4 |
| C35 لا توجد عروض *(سوق)* | `C35NoOffersScreen.kt` | `C35NoOffersView.swift` | `SCR-C35.md` | `SCR-C35/cases.json` | 6 | 6 |

أضافت 4د 30 حالة مشتركة، وكل حالة لها لقطة.

## الربط

- C29: `GET /support/faqs` و`POST /support/messages` (حد 5 رسائل في الساعة).
- C32: `GET /notifications` و`POST /notifications/read`؛ فتح الإشعار يذهب لرابطه الداخلي.
- C33: `GET/PATCH /me`، و`POST /me/password`، و`DELETE /me`. الحذف ممنوع مع طلب نشط (حالة `active_order`)، والقرار من الخادم.
- C34: `GET /terms/current`. **سيتغير مصدره** إلى وحدة «الصفحات» في اللوحة (البند 4 من قرارات هذه الجولة)، بنفس الشكل في التطبيق.
- C35: إعادة النشر `POST /orders/{order}/republish` والتعديل `PATCH /orders/{order}`، والأزرار من `available_actions` (DEC-031). تظهر فقط مع `offers_enabled`.

## الجديد في الدفعة

- نصوص: مجموعات `help.*` و`notifications.*` و`account.*` و`terms.*` و`no_offers.*`.
- رموز: لا يوجد رمز جديد.
- مكونات: لا يوجد مكوّن عام جديد.

## التحقق في هذه الجلسة

| الفحص | النتيجة |
|---|---|
| اختبارات الخادم | 143 اختبارًا، 1017 تأكيدًا — ناجحة |
| `verifyPaparazziDebug` (app + core:design) | ناجح |
| `swift build` + `swift test` لـ `iosapp/Core` (Docker `swift:6.0`) | ناجح |
| `tools/check-parity` و`tools/lint-design` و`tools/gen-design --check` | ناجحة |

## أسئلة مفتوحة

لا يوجد سؤال واجهة. OD-04 (المراجعة القانونية) يظل مفتوحًا لمحتوى C34 فقط.

## iOS غير المتحقق منه بصريًا

- تم: بناء واختبار Swift Core على Linux بنفس fixtures.
- مؤجل حتى دفعة «تحقق iOS» (DEC-045).
