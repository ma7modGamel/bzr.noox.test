# SCR-C20 — إلغاء الطلب

- النمط: E — `AppBottomSheet` فوق C09.
- المرجع البصري: 43 §5 و§7، ومكونات الاختيار من C04.
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: إجراء `cancel` في C09 فقط · الخروج إلى: C09 بعد الرجوع، أو حالة CANCELLED بعد النجاح.
- القواعد: BR-070، 13، T-04/T-08/T-09.
- الـ API: `POST /orders/{order}/cancel` مع `reason_code` و`note` الاختيارية و`expected_version`.
- التخطيط: عنوان `cancel.title` ← `InfoBanner` (`cancel.free`) ← `RadioCard` لكل سبب عميل من 13 ← `TextAreaField` للملاحظة الاختيارية ← زر `DangerTextButton` للتأكيد وزر ثانوي للرجوع.
- السلوك: لا يُرسل الإلغاء إلا إذا احتوت `available_actions` على `cancel` واختير سبب. أثناء الإرسال تُعطّل الإجراءات. `CONFLICT` و`INVALID_TRANSITION` يعيدان تحميل C09.
- الحالات: تحميل / خطأ / دون اختيار / سبب مختار / إرسال جارٍ.
- fixtures: `design/fixtures/SCR-C20/cases.json`.
- أسئلة مفتوحة: لا يوجد.
