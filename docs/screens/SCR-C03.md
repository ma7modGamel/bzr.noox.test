# SCR-C03 — طلب جديد: الخدمة والمشكلة

- النمط: A — نموذج، الخطوة 1 من 3.
- المرجع البصري: `design/reference/C03-request-step1-service.jpeg` بعد تعديلات 27 §أ.
- الطرف والوضع: العميل، في وضعي الموظفين والسوق.
- الدخول من: SCR-C01 · الخروج إلى: SCR-C04، وSCR-C17 لإضافة الوسائط.
- القواعد: BR-011، BR-012، BR-013، BR-014، DEC-024، DEC-035.
- الـ API: `GET /catalog?city_id={city_id}`؛ رفع الوسائط في SCR-C17 عبر `POST /media`.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `request.new.title` ثم `StepIndicator` 1/3.
  2. عنوان `request.category.title` وشبكة `SelectableTile` من الفئات.
  3. عنوان `request.problem.title` و`SelectableChip` من أنواع مشكلة الفئة المختارة.
  4. `TextAreaField`: `request.description.label` و`request.description.placeholder`، مع عداد `{count}/1000`.
  5. عند `is_other=true` يظهر `common.required` والتحقق الأدنى 10 أحرف.
  6. معاينات `MediaThumb` للوسائط المضافة مع حذف، وزر يفتح SCR-C17.
  7. `PrimaryButton` بالمفتاح `request.step1.action`.
- المتغيرات: فئة/مشكلة غير مختارة؛ مشكلة عادية؛ مشكلة أخرى؛ وسائط فارغة/مضافة.
- الحالات: تحميل الكتالوج / خطأ وإعادة المحاولة / عدم اتصال / تحقق الحقول / جاهز للمتابعة.
- الإجراءات: زر المتابعة انتقال داخل نموذج محلي؛ لا يوجد طلب منشور بعد كي يحمل `available_actions`.
- عقد 6ب: أنواع المشكلة تُرشّح حسب `category_id` المختار، واختيارها يحفظ `problem_type_id` و`is_other` الفعليين؛ تغيير الفئة يمسح مشكلة لم تعد تنتمي إليها، ولا يُستبدل أي اختيار بأول عنصر في الكتالوج.
- الأخطاء: BR-011/VALIDATION_FAILED ← الحقل المقابل؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C03/cases.json`.
- أسئلة مفتوحة: لا يوجد.
