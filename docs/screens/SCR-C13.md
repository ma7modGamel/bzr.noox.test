# SCR-C13 — استعادة كلمة المرور

- النمط: A
- المرجع البصري: C04 (الحقل)، C03 (الزر الرئيسي)
- الطرف والوضع: العميل، قبل تسجيل الدخول
- الدخول من: SCR-C10 زر `auth.password.forgot.open` · الخروج إلى: SCR-C10 بعد إرسال الرسالة أو من زر الرجوع
- القواعد: BR-004، DEC-025
- الـ API: `POST /auth/password/forgot`؛ ويستهلك W02 رابط الاستعادة عبر `POST /auth/password/reset`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `auth.password.forgot.title`.
  2. وصف `auth.password.forgot.body`.
  3. `AppTextField` للبريد: `auth.email.label` و`auth.email.placeholder`.
  4. `PrimaryButton` بالمفتاح `auth.password.forgot.action`.
  5. في النجاح `InfoBanner` بالمفتاح `auth.password.forgot.sent`.
  6. `LinkButton` بالمفتاح `auth.login.open` إلى SCR-C10.
- التحقق المحلي: البريد مطلوب وبصيغة بريد. لا يكشف نجاح الاستجابة وجود حساب للبريد من عدمه.
- تحويل الاستجابة: كل استجابة مقبولة تعرض حالة الإرسال نفسها؛ تعيين كلمة المرور الجديدة يتم من W02 كما يحدد جرد الشاشات.
- الحالات: فراغ ابتدائي / تحميل / خطأ بريد / تم الإرسال / عدم اتصال.
- الأخطاء: `VALIDATION_FAILED` ← خطأ الحقل؛ `RATE_LIMITED` ← `auth.error.rate_limited`؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C13/cases.json`
- ملاحظة الإجراءات: أزرار المصادقة ثابتة لأنها ليست تمثيلًا لطلب؛ قاعدة `available_actions` في 43 §6 تخص إجراءات الطلب.
- أسئلة مفتوحة: لا يوجد.
