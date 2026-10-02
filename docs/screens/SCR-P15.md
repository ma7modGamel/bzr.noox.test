# SCR-P15 — جاري التنفيذ
- النمط: D — متابعة حالة
- المرجع البصري: SCR-C09، وSCR-C31 للتكلفة الإضافية
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: SCR-P14 بعد بدء التنفيذ أو موافقة العميل على العرض · الخروج إلى: SCR-P16 بعد `complete_work` أو SCR-P20 لإضافة مقترح
- القواعد: BR-040..045، BR-071
- الـ API: `GET /provider/orders?scope=current`، `POST /provider/orders/{id}/proposals`، `POST /provider/orders/{id}/complete`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` و`Badge` و`ProviderHeader` للعميل.
  2. `SummaryCard` و`StatusStepper` من استجابة الطلب.
  3. `SecondaryButton` لـ`submit_proposal` يفتح SCR-P20.
  4. `PrimaryButton` لـ`complete_work`.
  5. `DangerTextButton` لـ`report_unable` يفتح SCR-P21.
- المتغيرات: بلا مقترح معلّق / مقترح معلّق يخفي الإكمال والمقترح وفق `available_actions`.
- الحالات: تحميل / خطأ / محتوى / تنفيذ الإجراء.
- الأخطاء: `BR-041` للمقترح المعلّق و`BR-045` قبل الإنهاء؛ `CONFLICT` يعيد التحميل.
- fixtures: `design/fixtures/SCR-P15/cases.json`
- أسئلة مفتوحة: لا يوجد.
