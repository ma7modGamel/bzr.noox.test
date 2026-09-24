# تقرير الدفعة 3ج — C14–C17

## بوابة الاعتماد

- صفحة المقارنة: `reports/batch-3c/customer-support/comparison.html`
- الاعتماد مكتمل؛ صفحة المقارنة محدّثة بقرار حذف العنوان.
- 32 لقطة Android (400×800): 28 لـ C14–C17، و1 لحالة C04 بلا عناوين، و3 لنقطة دخول C17 في C03.

## التطابق

| الشاشة | Compose | SwiftUI | المواصفة | fixture | حالات Android | حالات Swift Core |
|---|---|---|---|---|---:|---:|
| C14 العناوين | `C14AddressesScreen.kt` | `C14AddressesView.swift` | `SCR-C14.md` | `SCR-C14/cases.json` | 7 | 7 |
| C15 إضافة/تعديل عنوان | `C15AddressFormScreen.kt` | `C15AddressFormView.swift` | `SCR-C15.md` | `SCR-C15/cases.json` | 8 | 8 |
| C16 اليوم والفترة | `C16SlotPickerScreen.kt` | `C16SlotPickerView.swift` | `SCR-C16.md` | `SCR-C16/cases.json` | 6 | 6 |
| C17 الوسائط | `C17MediaScreen.kt` | `C17MediaView.swift` | `SCR-C17.md` | `SCR-C17/cases.json` | 8 | 8 |

الإجمالي: 29 حالة C14–C17 لكل منصة، مع حالة `address_missing` في C04. إجمالي رحلة العميل قبل إضافات 4أ: 79 حالة لكل منصة.

لقطات التكامل الإضافية: `SCR-C03-empty` و`SCR-C03-other_short` و`SCR-C03-with_media`. تعديل C04 لفتح C16 سلوكي فقط ولا يغيّر الرسم، وكل لقطاته المعتمدة اجتازت فحص الرجوع.

## الربط

- C14: `GET/POST/PATCH/DELETE /addresses`، والاختيار يُحدّث مسودة الطلب محليًا.
- C15: `GET /cities` و`GET /cities/{city}/areas` ثم حفظ العنوان.
- C16: `GET /slots`؛ الخادم وحده مصدر الفترات المتاحة وفق BR-017.
- C17: `POST /media` multipart و`DELETE /media/{id}`؛ حدود BR-014 متحققة في المنطق المشترك والخادم.

## الجديد في الدفعة

- نصوص جديدة: مجموعة `address.*` و`slot.*` و`media.*` ومفاتيح أيام الأسبوع وفترتي ص/م.
- رموز جديدة: لا يوجد؛ أُعيد استخدام `location` و`navigation` و`image` و`video` و`mic` و`trash`.
- مكونات جديدة: لا يوجد؛ أُعيد استخدام `SummaryCard` و`MapCard` و`SelectableChip` و`AppTextField` و`CheckRow` و`MediaThumb` والأزرار والحالات العامة.

## حسم حذف العنوان

- التأكيد يظهر في `AppBottomSheet` في المنصتين.
- الحذف ناعم؛ والخادم يعيّن أحدث عنوان متبقٍ افتراضيًا ويرجعه، أو `null` عند حذف الأخير.
- لا يتغير `address_text` المثبت في الطلبات السابقة، وC04 تمنع الاستمرار بلا عنوان.

## أسئلة مفتوحة

لا يوجد.

## iOS المؤجل بصريًا

- تحقق Linux: بناء Swift Core واختباراته، SwiftFormat، SwiftLint، syntax لملفات SwiftUI، القيم البصرية، ومطابقة المفاتيح والـfixtures.
- غير المتحقق حتى دفعة «تحقق iOS»: الرسم الفعلي لـSwiftUI، Cairo على الجهاز، قياسات 400×800، MapKit، منتقي الوسائط/الكاميرا/الصلاحيات، والتسجيل الفعلي AAC/m4a.
