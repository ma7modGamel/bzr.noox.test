# SCR-P12 — الطلب المؤكد
- النمط: D — متابعة حالة
- المرجع البصري: SCR-C09 من منظور الفني
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: SCR-P08 أو إشعار التعيين · الخروج إلى: SCR-P13 بعد `start_trip` أو SCR-P21 للاعتذار
- القواعد: BR-024، BR-035، BR-036، BR-101، BR-110
- الـ API: `GET /provider/orders?scope=current`، `POST /provider/orders/{id}/start-trip`، `POST /provider/orders/{id}/back-out`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.order.title`.
  2. `Badge` من `display_status`.
  3. `ProviderHeader` للعميل: الاسم والتقييم من الاستجابة.
  4. `SummaryCard`: رقم الطلب، الخدمة، العنوان التفصيلي، الموعد.
  5. `StatusStepper` من `stepper`.
  6. أزرار `call` و`chat` و`navigate` عند ورودها في `available_actions`.
  7. `PrimaryButton` لـ`start_trip` فقط عند وروده، و`DangerTextButton` لـ`back_out` فقط عند وروده.
- المتغيرات: الآن / مجدول قبل وقت التحرك / مجدول متاح للتحرك.
- الحالات: تحميل / خطأ / محتوى / تنفيذ الإجراء.
- الأخطاء: `CONFLICT` يعيد تحميل الطلب؛ `BUSINESS_RULE_VIOLATION` و`VALIDATION_FAILED` يعرضان رسالة الخادم؛ خطأ الشبكة يعرض `ErrorState`.
- fixtures: `design/fixtures/SCR-P12/cases.json`
- أسئلة مفتوحة: لا يوجد.
