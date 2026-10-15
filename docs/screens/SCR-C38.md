# SCR-C38 — طلب جديد: الميزانية والخامات

- النمط: A — نموذج، الخطوة 4 من 5 (DEC-071).
- المرجع البصري: Figma «Primo App» 3002:1270.
- الطرف والوضع: العميل، في وضعي الموظفين والسوق.
- الدخول من: SCR-C04 · الخروج إلى: SCR-C05.
- القواعد: BR-015، BR-016، DEC-035، DEC-040.
- الـ API: `GET /config` (وضع التشغيل).
- التخطيط (من فوق لتحت):
  1. `AppTopBar` بعنوان `request.budget.title`، و`StepIndicator` 4/5.
  2. **السوق بس:**
     - عنوان `request.budget.section` ووصفه.
     - كارت المبلغ: `AmountField` اختياري بالجنيه بخط كبير.
     - `RadioCard` لطرق التسعير (`pricing_labels`)، ومن ضمنها «استقبال عروض من غير سعر».
  3. **الموظفين:** `InfoBanner` بنص `request.staff.inspection.free` مكان التسعير.
  4. عنوان `request.materials.section` ووصفه، وتحته `RadioCard` بأيقونة لكل مسؤولية خامات.
  5. `InfoBanner` بنص `request.materials.assurance` («أي تكلفة إضافية لازم توافق عليها جوه التطبيق الأول»).
  6. شريط سفلي ثابت فيه `PrimaryButton` بالمفتاح `request.step4.action` («مراجعة الطلب»).
- **محذوف من Figma لعدم وجود مصدر بيانات** (BACKEND-NOTES B-14): شرائح المبالغ السريعة وشارة «متوسط المنطقة».
- المتغيرات: موظفين/سوق؛ بميزانية/من غير؛ مسؤوليات الخامات التلاتة.
- الحالات: ميزانية غير صالحة (`request.budget.invalid`) / اختيار ناقص / جاهز.
- fixtures: `design/fixtures/SCR-C38/cases.json`.
- أسئلة مفتوحة: لا يوجد.
