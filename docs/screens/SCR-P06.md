# SCR-P06 — استلام المستحقات وإرسال الطلب

- النمط: A
- المرجع البصري: SCR-C22 (طرق الدفع)، SCR-C05 (المراجعة)
- الطرف والوضع: طالب الانضمام؛ نوع التعاقد لا يظهر، والبيانات مشفرة في الخادم
- الدخول من: SCR-P05 · الخروج إلى: SCR-P07 بعد نجاح `submit_provider_application`
- القواعد: DEC-022، BR-065؛ طريقة واحدة إلزامية من الخادم (`INSTAPAY`/`WALLET`/`BANK`) والبيانات المناسبة إلزامية، وتُستخدم للموظف أيضًا في رد الخامات
- الـ API: طرق الاستلام من `GET /config.option_lists.provider_payout_methods`؛ `POST /provider/application` يرسل الحقول ومعرّفات الصور الخاصة المرفوعة مسبقًا، والإجراء يظهر فقط من `available_actions`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.onboarding.payout.title`
  2. نص تقدم — `provider.onboarding.step.5`
  3. `RadioCard` لكل طريقة قادمة من الخادم
  4. `AppTextField` ديناميكي — رقم إنستاباي/المحفظة أو IBAN، بمفتاح التسمية القادم مع الخيار
  5. `SummaryCard` — أعداد الفئات والتخصصات والمناطق والمستندات
  6. `InfoBanner` — `provider.onboarding.review.note`
  7. `PrimaryButton` — `action.submit_provider_application`، من `available_actions`
- المتغيرات: بلا طريقة؛ كل طريقة من الخادم؛ إرسال؛ نجاح؛ إعادة تقديم
- الحالات: تحميل؛ أخطاء تحقق؛ رفع/إرسال؛ خطأ شبكة قابل لإعادة المحاولة؛ محتوى
- الأخطاء: طريقة/بيانات ناقصة أو غير صالحة ← مفاتيح `provider.onboarding.payout.*`؛ `EMAIL_NOT_VERIFIED` ← SCR-C12؛ `APPLICATION_NOT_EDITABLE` ← إعادة تحميل SCR-P07؛ `422` يعاد إلى الخطوة المرتبطة بالحقل
- fixtures: `design/fixtures/SCR-P06/cases.json`
- أسئلة مفتوحة: لا يوجد
