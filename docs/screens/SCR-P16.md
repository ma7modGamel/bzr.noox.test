# SCR-P16 — بانتظار الدفع
- النمط: D — متابعة حالة
- المرجع البصري: SCR-C09 وSCR-C21
- الطرف والوضع: الفني — الموظفين والسوق
- الدخول من: SCR-P15 بعد إنهاء العمل · الخروج إلى: SCR-P17 بعد إغلاق الطلب
- القواعد: BR-050..055، BR-065
- الـ API: `GET /provider/orders?scope=current`، `POST /provider/orders/{id}/cash-received`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` و`Badge`.
  2. `ProviderHeader` للعميل.
  3. `SummaryCard` للمصنعية والخامات والإجمالي وطريقة الدفع من `amounts`.
  4. `StatusStepper` من الخادم.
  5. `PrimaryButton` لـ`confirm_cash` فقط عند وروده؛ في الدفع الإلكتروني يظهر `InfoBanner` للانتظار بلا زر.
- المتغيرات: نقدي / دفع إلكتروني أو تحويل قيد التحقق.
- الحالات: تحميل / خطأ / محتوى / تأكيد الاستلام.
- الأخطاء: اختلاف المبلغ يعرض خطأ الخادم؛ `CONFLICT` يعيد التحميل.
- fixtures: `design/fixtures/SCR-P16/cases.json`
- أسئلة مفتوحة: لا يوجد.
