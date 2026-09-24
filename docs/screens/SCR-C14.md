# SCR-C14 — العناوين المحفوظة / اختيار عنوان
- النمط: B
- المرجع البصري: C06 (قائمة الكروت)، C04 (`SummaryCard` للعناوين)
- الطرف والوضع: العميل — السوق والموظفين
- الدخول من: SCR-C04 زر "تغيير" أو SCR-C02 "العناوين المحفوظة"   ·   الخروج إلى: SCR-C04 بعد الاختيار، أو SCR-C15 للإضافة/التعديل
- القواعد: BR-010، 07 §الخطوة 2، 20 §العنوان
- الـ API: `GET /addresses`، `DELETE /addresses/{address}`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar`  title=`address.list.title`
  2. `ScreenHeading`  title=`address.list.heading`  detail=`address.list.body`
  3. حالة فارغة: `EmptyState`  title=`address.empty.title`  body=`address.empty.body`
  4. لكل عنوان: `SummaryCard`  title=`address.label`  rows=المنطقة ثم `address_text`  editText=`common.edit`، ومعه اختيار `SelectableChip` وحذف `DangerTextButton`.
  5. الضغط على الحذف يفتح `AppBottomSheet` للتأكيد (43 §7)، ولا يُستخدم حوار النظام. الإلغاء يغلق الورقة، والتأكيد وحده يرسل الطلب.
  6. `PrimaryButton`  text=`address.add` → SCR-C15
- المتغيرات: إدارة من الحساب / اختيار من إنشاء الطلب؛ في وضع الاختيار يظهر `address.select` ويعود العنوان المختار إلى مسودة الطلب.
- الحالات: تحميل / خطأ شبكة / فراغ / عنوان واحد افتراضي / عدة عناوين وعنوان مختار / تأكيد الحذف / حذف جارٍ.
- الحذف ناعم. إذا كان العنوان المحذوف افتراضيًا، يعيّن الخادم آخر عنوان أُضيف أو عُدّل (`updated_at DESC, id DESC`) ويرجعه في `data.default_address`. إذا لم يبق عنوان تكون القيمة `null` وتُلزم C04 بإضافة عنوان قبل المتابعة. الطلبات السابقة لا تتأثر لأن عنوانها نسخة ثابتة.
- الأخطاء: `404` عند عنوان غير مملوك أو محذوف ← إعادة تحميل القائمة مع `error.body`؛ خطأ شبكة ← حالة الخطأ العامة.
- fixtures: `design/fixtures/SCR-C14/cases.json`
- أسئلة مفتوحة: لا يوجد.
