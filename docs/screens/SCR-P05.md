# SCR-P05 — مناطق العمل

- النمط: A
- المرجع البصري: SCR-C15 (المنطقة)، SCR-C16 (الاختيارات)
- الطرف والوضع: طالب الانضمام؛ المدن والمناطق المفعلة من الخادم فقط
- الدخول من: SCR-P04 · الخروج إلى: SCR-P06 محليًا بعد اختيار منطقة واحدة على الأقل
- القواعد: DEC-022؛ منطقة واحدة على الأقل، والمنطقة يجب أن تتبع مدينة مفعلة؛ لا ورديات أو جداول في المرحلة الأولى (DEC-057)
- الـ API: `GET /cities` ثم `GET /cities/{city}/areas`؛ الإرسال النهائي يرسل `area_ids`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.onboarding.areas.title`
  2. نص تقدم — `provider.onboarding.step.4`
  3. محدد مدينة من بيانات `/cities`
  4. `CheckRow` لكل منطقة من `/cities/{city}/areas`
  5. `InfoBanner` — `provider.onboarding.availability.note`
  6. `PrimaryButton` — `common.next`
- المتغيرات: تحميل المدن؛ مدينة بلا مناطق؛ بلا اختيار؛ منطقة/مناطق مختارة؛ إعادة تقديم
- الحالات: تحميل؛ خطأ شبكة؛ فراغ؛ خطأ اختيار؛ محتوى
- الأخطاء: منطقة مفقودة/غير مفعلة/لا تتبع المدينة ← `provider.onboarding.areas.required`
- fixtures: `design/fixtures/SCR-P05/cases.json`
- أسئلة مفتوحة: لا يوجد
