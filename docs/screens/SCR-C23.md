# SCR-C23 — تأكيد إنهاء الطلب

- النمط: C — تفاصيل.
- المرجع البصري: C08، و43 §4 (`SummaryCard` + شريط إجراءات ثابت).
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: C09 في AWAITING_CONFIRMATION · الخروج إلى: C24 بعد T-21، أو C27 عبر `open_dispute`.
- القواعد: BR-052، BR-080، 14، T-21/T-22.
- الـ API: `POST /orders/{order}/confirm-completion`؛ فتح المشكلة ينتقل إلى C27 ولا يرسل طلبًا من C23.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `completion.confirm.title`.
  2. `ScreenHeading` بالمفتاح `completion.confirm.heading`.
  3. `SummaryCard` لاسم الفني المختصر، والمصنعية، والخامات، والإجمالي وطريقة الدفع النقدي، كلها من استجابة الطلب.
  4. `InfoBanner` بالمفتاح `completion.confirm.help`.
  5. `PrimaryButton` بالمفتاح `action.confirm_completion` فقط مع `confirm_completion`.
  6. `SecondaryButton` بالمفتاح `completion.report_problem` فقط مع `open_dispute`.
- السلوك: التأكيد يرسل `expected_version`; النجاح يفتح C24 إذا احتوت الاستجابة الجديدة على `rate`. لا تستنتج الشاشة إمكان التأكيد أو المشكلة من الحالة.
- الحالات: تحميل / خطأ / جاهز / إرسال التأكيد / إجراء غير متاح.
- الأخطاء: `CONFLICT` يعيد تحميل الطلب؛ `INVALID_TRANSITION` يعيد C09 بالحالة الحديثة.
- fixtures: `design/fixtures/SCR-C23/cases.json`.
- أسئلة مفتوحة: لا يوجد.
