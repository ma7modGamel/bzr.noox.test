# SCR-P04 — الفئات والتخصصات

- النمط: A
- المرجع البصري: SCR-C03 (اختيار نوع المشكلة)
- الطرف والوضع: طالب الانضمام؛ القوائم كلها من الخادم
- الدخول من: SCR-P03 · الخروج إلى: SCR-P05 محليًا بعد التحقق
- القواعد: DEC-022 وDEC-024؛ فئة واحدة على الأقل وتخصص واحد على الأقل، ولا يقبل تخصصًا خارج الفئات المختارة
- الـ API: `GET /catalog?city_id={id}`؛ القيم المرسلة في `POST /provider/application` هي `category_ids` و`specialty_ids`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.onboarding.categories.title`
  2. نص تقدم — `provider.onboarding.step.3`
  3. `SelectableChip` لكل فئة مفعّلة من `data[].id/name`
  4. عنوان `provider.onboarding.specialties.title`
  5. `CheckRow` لكل نوع مشكلة تابع للفئات المختارة من `problem_types[].id/name`
  6. `PrimaryButton` — `common.next`
- المتغيرات: بلا اختيار؛ فئة مختارة؛ فئات متعددة؛ تحميل التخصصات؛ إعادة تقديم
- الحالات: تحميل؛ خطأ شبكة؛ كتالوج فارغ؛ أخطاء اختيار؛ محتوى
- الأخطاء: فئة مفقودة ← `provider.onboarding.categories.required`؛ تخصص مفقود/غير تابع ← `provider.onboarding.specialties.required`
- fixtures: `design/fixtures/SCR-P04/cases.json`
- أسئلة مفتوحة: لا يوجد
