# SCR-P13 — الفني في الطريق
- النمط: D — متابعة حالة
- المرجع البصري: SCR-C09 مع كارت العميل بدل كارت الفني
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: SCR-P12 بعد بدء التحرك · الخروج إلى: SCR-P14 بعد `mark_arrived` أو SCR-P21 للاعتذار
- القواعد: BR-036، BR-101، BR-110، BR-111، CFG-080، CFG-081
- الـ API: `GET /provider/orders?scope=current`، `POST /provider/orders/{id}/location`، `POST /provider/orders/{id}/arrived`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` و`Badge` من الخادم.
  2. `ProviderHeader` للعميل.
  3. `MapCard` للوجهة مع `navigate` عند وروده.
  4. `SummaryCard` للعناوين وبيانات الطلب.
  5. `StatusStepper` من الخادم.
  6. `PrimaryButton` لـ`mark_arrived`، وأزرار الاتصال والمحادثة، و`DangerTextButton` لـ`back_out`؛ كلها من `available_actions`.
- المتغيرات: موقع متاح / صلاحية مرفوضة / وصول بعيد يحتاج تأكيد الخادم.
- الحالات: تحميل / خطأ / محتوى / إرسال الوصول.
- الأخطاء: `EC-21` يطلب صلاحية الموقع؛ `BR-111` يعرض تأكيد الوصول البعيد؛ `CONFLICT` يعيد التحميل.
- fixtures: `design/fixtures/SCR-P13/cases.json`
- أسئلة مفتوحة: لا يوجد.
