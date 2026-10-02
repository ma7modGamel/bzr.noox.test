# SCR-P14 — الوصول والمعاينة
- النمط: D — متابعة حالة
- المرجع البصري: SCR-C09، وSCR-C30 لعرض التنفيذ
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: SCR-P13 بعد الوصول · الخروج إلى: SCR-P15 أو SCR-P20 أو طلب مغلق بلا مبلغ
- القواعد: BR-041..043، BR-046، BR-056، BR-071، DEC-055
- الـ API: `GET /provider/orders?scope=current`، `POST /provider/orders/{id}/start-work`، `POST /provider/orders/{id}/complete-inspection-only`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` و`Badge` من الخادم.
  2. `ProviderHeader` للعميل و`SummaryCard` للطلب والعنوان.
  3. `InfoBanner` لدليل السعر من `execution_price_guide` إن وُجد؛ «مشكلة أخرى» تعرض تنبيه المراجعة بلا إلزام نطاق.
  4. `StatusStepper` من الخادم.
  5. `PrimaryButton` للإجراء التالي (`start_work` أو `submit_execution_quote`) عند وروده.
  6. `SecondaryButton` لـ`complete_inspection_only` بالنص `action.complete_inspection_only.free` في وضع الموظفين.
  7. `DangerTextButton` لـ`report_unable` و`report_no_show` عند ورودهما، ويفتح SCR-P21.
- المتغيرات: تنفيذ مباشر / معاينة بنطاق / «مشكلة أخرى» / السماح بعد انتظار عدم الحضور.
- الحالات: تحميل / خطأ / محتوى / تنفيذ الإجراء.
- الأخطاء: أخطاء BR-041..046 تعرض رسالة الخادم؛ `CONFLICT` يعيد تحميل الطلب.
- fixtures: `design/fixtures/SCR-P14/cases.json`
- أسئلة مفتوحة: لا يوجد.
