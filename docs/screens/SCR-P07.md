# SCR-P07 — حالة طلب الانضمام

- النمط: EmptyState
- المرجع البصري: SCR-C35 (حالة فارغة)، `WarningBox` من المعرض
- الطرف والوضع: صاحب ملف الفني فقط؛ يطبق في وضع الموظفين والسوق
- الدخول من: SCR-P01 أو بعد إرسال SCR-P06 · الخروج إلى: SCR-P02 عند `resubmit_provider_application`، أو SCR-P08 عند `open_provider_home`
- القواعد: DEC-022، DEC-033، BR-130..132؛ الرفض يعرض السبب، التعليق يعرض سببه، ولا يستطيع `PENDING_REVIEW` إعادة الإرسال
- الـ API: `GET /provider/application`؛ أزرار الشاشة من `available_actions` فقط
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.application.status.title`
  2. `EmptyState` — العنوان والنص حسب `display_status`
  3. `WarningBox` بسبب الرفض في `REJECTED` أو سبب الإيقاف في `SUSPENDED`
  4. `PrimaryButton` — `action.resubmit_provider_application` عند وروده
  5. `PrimaryButton` — `action.open_provider_home` عند وروده
- المتغيرات: `PENDING_REVIEW`؛ `REJECTED` بسبب؛ `ACTIVE`؛ `SUSPENDED` بسبب
- الحالات: تحميل؛ خطأ شبكة؛ محتوى
- الأخطاء: `AUTH_REQUIRED` ← SCR-C10؛ ملف اختفى ← SCR-P01؛ خطأ الشبكة ← `error.body`
- fixtures: `design/fixtures/SCR-P07/cases.json`
- أسئلة مفتوحة: لا يوجد
