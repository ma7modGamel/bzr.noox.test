# SCR-C15 — إضافة/تعديل عنوان
- النمط: A
- المرجع البصري: C04 (الحقول والكروت)، C09 (`MapCard`)
- الطرف والوضع: العميل — السوق والموظفين
- الدخول من: SCR-C14 زر "إضافة عنوان" أو "تعديل"   ·   الخروج إلى: SCR-C14 بعد الحفظ
- القواعد: BR-010، 20 §العنوان
- الـ API: `GET /cities`، `GET /cities/{city}/areas`، `POST /addresses`، `PATCH /addresses/{address}`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar`  title=`address.form.add_title` أو `address.form.edit_title`
  2. `MapCard`  title=`address.map.title`؛ الدبوس/موقعي من خرائط النظام ويعيدان `lat/lng` فقط للمنطق المشترك
  3. `ScreenHeading`  title=`address.area.title`
  4. `SelectableChip` لكل منطقة مفعّلة من الخادم
  5. `AppTextField` للتسمية، الشارع/التفاصيل، المبنى، الدور، الشقة، والعلامة المميزة؛ المفاتيح `address.field.*`
  6. `CheckRow`  text=`address.default`
  7. `PrimaryButton`  text=`common.save`
- المتغيرات: إضافة / تعديل. التعديل يملأ القيم الحالية ويرسل `PATCH`، والإضافة ترسل `POST`.
- التحقق: التسمية 1..50، المنطقة مطلوبة، تفاصيل العنوان 1..255، المبنى ≤50، الدور/الشقة ≤20، العلامة ≤150، و`lat/lng` مطلوبان وفي المدى المقبول. التحقق في ViewModel وSwift Core قبل الطلب، والخادم يعيد تطبيقه.
- الحالات: تحميل المراجع / خطأ شبكة / نموذج فارغ / أخطاء حقول / موقع غير محدد / نموذج صالح / حفظ جارٍ.
- الأخطاء: `422` ← إبقاء القيم وإظهار `address.validation.invalid` أو مفتاح الحقل؛ `BUSINESS_RULE_VIOLATION`/`BR-010` ← `address.validation.unserved_area`؛ خطأ شبكة ← `error.body`.
- fixtures: `design/fixtures/SCR-C15/cases.json`
- أسئلة مفتوحة: لا يوجد؛ المدينة تأتي من المدينة المختارة في بيانات المرجع، والمنطقة اختيار صريح كما تلزم وثيقة 20.

