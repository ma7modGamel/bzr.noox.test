# SCR-P21 — اعتذار أو تعذّر التنفيذ
- النمط: E — ورقة سفلية
- المرجع البصري: SCR-C20
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: P12/P13 للإعتذار، أو P14/P15 للتعذّر، أو P14 لعدم وجود العميل · الخروج إلى: الرئيسية أو الطلب بعد نتيجة الخادم
- القواعد: BR-036، BR-071، CFG-033
- الـ API: `GET /config`، ثم أحد `POST /provider/orders/{id}/back-out` أو `/unable` أو `/customer-no-show`
- التخطيط (من أعلى لأسفل):
  1. `AppBottomSheet` بعنوان يحدده الإجراء.
  2. `RadioCard` لأسباب `provider_cancellation_reasons` القادمة من `/config` في الاعتذار والتعذّر.
  3. `TextAreaField` للملاحظة الإلزامية في `report_unable`.
  4. `WarningBox` لعدم وجود العميل ووقت السماح من `deadlines.no_show_allowed_at`؛ لا قائمة أسباب لهذا الإجراء.
  5. زر التأكيد الموافق للإجراء، ولا يظهر إلا إذا كان الإجراء داخل `available_actions`.
- الحالات: بلا اختيار / صالح / إرسال / نجاح / قبل مهلة عدم الحضور / خطأ.
- الأخطاء: `VALIDATION_FAILED` قرب الحقل؛ `BUSINESS_RULE_VIOLATION` في الورقة؛ `CONFLICT` يعيد تحميل الطلب.
- fixtures: `design/fixtures/SCR-P21/cases.json`
- أسئلة مفتوحة: لا يوجد.
