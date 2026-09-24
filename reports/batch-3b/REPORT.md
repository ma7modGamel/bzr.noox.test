# تقرير الدفعة 3ب — شاشات العميل C01–C09

## بوابة الاعتماد

- صفحة المقارنة: [`customer/comparison.html`](customer/comparison.html).
- المطلوب اعتماد **49 لقطة Android** قبل بدء 3ج.
- كل متغير موضوع بجانب اللقطة الأصلية للشاشة؛ فروق النص المحايد وقواعد 27 §أ مقصودة.

## التطابق

| النطاق | Android | iOS | المواصفة | الحالات المشتركة |
|---|---|---|---|---:|
| C01 الرئيسية | `C01HomeScreen` | `C01HomeView` | `docs/screens/SCR-C01.md` | 5 |
| C02 الحساب | `C02AccountScreen` | `C02AccountView` | `docs/screens/SCR-C02.md` | 4 |
| C03 المشكلة | `C03ProblemScreen` | `C03ProblemView` | `docs/screens/SCR-C03.md` | 5 |
| C04 الموعد | `C04TimingScreen` | `C04TimingView` | `docs/screens/SCR-C04.md` | 6 |
| C05 المراجعة | `C05ReviewScreen` | `C05ReviewView` | `docs/screens/SCR-C05.md` | 5 |
| C06 العروض | `C06OffersScreen` | `C06OffersView` | `docs/screens/SCR-C06.md` | 6 |
| C07 ملف الفني | `C07ProviderScreen` | `C07ProviderView` | `docs/screens/SCR-C07.md` | 4 |
| C08 تفاصيل العرض | `C08OfferDetailsScreen` | `C08OfferDetailsView` | `docs/screens/SCR-C08.md` | 5 |
| C09 المتابعة | `C09TrackingScreen` | `C09TrackingView` | `docs/screens/SCR-C09.md` | 9 |
| **الإجمالي** | **49/49** | **49/49** | 9 مواصفات | **49** |

فحص `design/parity.json`: 42 مكوّنًا، 58 شاشة، و255 مفتاح نص مشترك بلا اختلاف منصة أو fixture.

## الربط والاختبارات

- Android وSwift Core يشغّلان ملفات الحالات التسعة نفسها من `design/fixtures/`.
- Android: 49 حالة منطق + 49 لقطة Paparazzi؛ البناء والتحقق من اللقطات ناجحان.
- iOS Core على Linux: 49 حالة 3ب، ضمن 8 اختبارات Swift ناجحة بلا `SwiftUI` أو `UIKit` في Core.
- الـAPI الحقيقي موصول في العميلين للصفحة الرئيسية والطلب والملف وقبول العرض، مع `available_actions` من الخادم فقط.
- أضيف `GET /providers/{provider}?order_id=...` مع 3 اختبارات/15 assertion للملكية والخصوصية.

## الجديد

- النصوص: 68 مفتاحًا عربيًا للشاشات C01–C09.
- الرموز: لا رموز جديدة؛ استُخدمت رموز المكتبة المعتمدة.
- المكونات: لا مكونات عامة جديدة؛ استُخدمت مكتبة الدفعة 1.
- منطق مشترك: `CustomerLogic` و`CustomerViewModel` وعميل API لكل منصة.

## الأسئلة المفتوحة

لا يوجد.

## iOS غير المتحقق منه آليًا الآن

- تم بناء واختبار `iosapp/Core` على Linux، ومرّ `swift-format` وSwiftLint على كل `iosapp`.
- لا يمكن بناء SwiftUI أو إنتاج لقطات iOS على Linux؛ بناء Xcode واللقطات والمقارنة البكسلية تبقى في دفعة «تحقق iOS» النهائية حسب DEC-045.
