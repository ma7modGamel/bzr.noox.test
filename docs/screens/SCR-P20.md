# SCR-P20 — مقترح سعر
- النمط: E — ورقة سفلية
- المرجع البصري: SCR-C30/SCR-C31 ومكونات الحقول في C04
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: SCR-P14 (`submit_execution_quote`) أو SCR-P15 (`submit_proposal`) · الخروج إلى شاشة الطلب النشط
- القواعد: BR-040..046، DEC-055
- الـ API: `POST /provider/orders/{id}/proposals`، `POST /media`
- التخطيط (من أعلى لأسفل):
  1. `AppBottomSheet` بعنوان `provider.proposal.title`.
  2. `RadioCard` للأنواع التي يرسلها السياق/الخادم: عرض تنفيذ، خامات، عمل إضافي.
  3. `InfoBanner` لنطاق دليل السعر في عرض التنفيذ إن وُجد.
  4. `AmountField` للمبلغ و`TextAreaField` لسبب المقترح.
  5. `TextAreaField` لسبب الخروج عن النطاق، يظهر فقط إذا أثبت منطق الشاشة أن المبلغ خارجه.
  6. `MediaThumb` اختياري لصورة مرفوعة حقيقيًا.
  7. `PrimaryButton` لـ`submit_execution_quote` أو `submit_proposal` فقط من `available_actions`.
- التحقق: المبلغ أكبر من صفر؛ السبب 5..300؛ سبب الخروج 10..300 عند تجاوز الدليل؛ «مشكلة أخرى» معفاة منه وتُعلّم للمراجعة.
- الحالات: فارغ / صالح داخل النطاق / خارج النطاق / مشكلة أخرى / رفع صورة / إرسال / نجاح / خطأ.
- الأخطاء: `VALIDATION_FAILED` قرب الحقل؛ BR-041..046 في `WarningBox`؛ فشل الوسائط يسمح بإعادة المحاولة.
- fixtures: `design/fixtures/SCR-P20/cases.json`
- أسئلة مفتوحة: لا يوجد.
