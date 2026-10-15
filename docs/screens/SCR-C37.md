# SCR-C37 — طلب جديد: وصف المشكلة

- النمط: A — نموذج، الخطوة 2 من 5 (DEC-071).
- المرجع البصري: Figma «Primo App» 3002:144.
- الطرف والوضع: العميل، في وضعي الموظفين والسوق.
- الدخول من: SCR-C03 · الخروج إلى: SCR-C04، وSCR-C17 لإضافة الوسائط، وSCR-C03 من «تعديل».
- القواعد: BR-012، BR-013، BR-014، DEC-035.
- الـ API: رفع الوسائط في SCR-C17 عن طريق `POST /media`.
- التخطيط (من فوق لتحت):
  1. `AppTopBar` بعنوان `request.problem.title`، و`StepIndicator` 2/5.
  2. كارت «الخدمة المختارة: {name}» وفيه زر `common.edit` بيرجع لـ SCR-C03.
  3. عنوان `request.problem.type` و`SelectableChip` لأنواع مشكلة الفئة. ده إضافة على Figma، لأن الخادم بيطلب `problem_type_id`.
  4. `TextAreaField` بعنوان `request.description.label` مع عداد `{count}/1000` وتلميح `request.description.hint`. عند `is_other=true` بيظهر `common.required`، وأقل طول 10 أحرف.
  5. «صورة، فيديو أو رسالة صوتية»: 3 أزرار (صورة / فيديو / تسجيل صوتي) كلها بتفتح SCR-C17، وتحتها شريط المرفقات الحالية مع خانة «إضافة مرفق».
  6. سطر طمأنة `request.media.privacy` (مرفقاتك بتظهر للفنيين الموثّقين بس).
  7. شريط سفلي ثابت فيه `PrimaryButton` بالمفتاح `request.step2.action` («التالي: المكان والموعد»).
- المتغيرات: مشكلة مش مختارة؛ مشكلة عادية؛ مشكلة أخرى؛ وسائط فاضية أو مضافة.
- الحالات: تحقق الحقول / جاهز للمتابعة.
- الأخطاء: BR-011/VALIDATION_FAILED ← الحقل المقابل.
- fixtures: `design/fixtures/SCR-C37/cases.json`.
- أسئلة مفتوحة: لا يوجد.
