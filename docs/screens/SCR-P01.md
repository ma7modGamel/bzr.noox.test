# SCR-P01 — الدخول إلى وضع الفني

- النمط: C
- المرجع البصري: SCR-C02 (تبديل الوضع)، SCR-C35 (الحالة الفارغة)
- الطرف والوضع: مستخدم موثّق البريد، في وضع العميل قبل إنشاء/فتح ملف الفني؛ ينطبق في وضعي الموظفين والسوق
- الدخول من: القائمة الجانبية، زر `menu.provider_mode` · الخروج إلى: SCR-P02 عند `start_provider_application`، أو SCR-P07 عند وجود طلب مراجعة، أو SCR-P08 لملف `ACTIVE`
- القواعد: DEC-001، DEC-022، DEC-025، BR-005؛ `employment_type` تحدده الإدارة ولا يظهر هنا
- الـ API: `GET /provider/application`؛ كل انتقال من `available_actions`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.onboarding.intro.title`
  2. `EmptyState` بأيقونة الحساب — `provider.onboarding.intro.title` و`provider.onboarding.intro.body`
  3. ثلاث `CheckRow` — `provider.onboarding.benefit.jobs` و`provider.onboarding.benefit.schedule` و`provider.onboarding.benefit.support`
  4. `PrimaryButton` — `action.start_provider_application`، يظهر فقط من `available_actions`
- المتغيرات: لا ملف؛ `PENDING_REVIEW`؛ `REJECTED`؛ `ACTIVE`؛ `SUSPENDED`
- الحالات: تحميل؛ خطأ شبكة مع إعادة المحاولة؛ البريد غير موثّق؛ محتوى
- الأخطاء: `EMAIL_NOT_VERIFIED` ← الانتقال إلى SCR-C12؛ `AUTH_REQUIRED` ← SCR-C10؛ خطأ الشبكة ← `error.body`
- fixtures: `design/fixtures/SCR-P01/cases.json`
- أسئلة مفتوحة: لا يوجد
