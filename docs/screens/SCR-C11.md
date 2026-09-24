# SCR-C11 — إنشاء حساب

- النمط: A
- المرجع البصري: C04 (الحقول)، C03 (الزر الرئيسي)
- الطرف والوضع: العميل، قبل تسجيل الدخول
- الدخول من: SCR-C10 زر `auth.register.open` · الخروج إلى: SCR-C12 بعد نجاح التسجيل، أو SCR-C10 من زر الدخول
- القواعد: BR-001، BR-002، BR-004، DEC-025
- الـ API: `POST /auth/register`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `auth.register.title`.
  2. `AppTextField` للاسم: `auth.name.label` و`auth.name.placeholder`.
  3. `AppTextField` للبريد: `auth.email.label` و`auth.email.placeholder`.
  4. `AppTextField` للهاتف: `auth.phone.label` و`auth.phone.placeholder`.
  5. `SecureTextField` لكلمة المرور: `auth.password.label` و`auth.password.hint`.
  6. `PrimaryButton` بالمفتاح `auth.register.action`.
  7. `LinkButton` بالمفتاح `auth.login.open` إلى SCR-C10.
- التحقق المحلي: الاسم مطلوب وبحد أقصى 100 حرف؛ البريد مطلوب وبصيغة بريد؛ الهاتف يطابق `01xxxxxxxxx`؛ كلمة المرور 8 أحرف على الأقل.
- تحويل الاستجابة: يحفظ رمز Sanctum، ويحفظ البريد لحالة SCR-C12، ثم يفتح SCR-C12. الخادم يرسل رسالة التفعيل عند إنشاء الحساب.
- الحالات: فراغ ابتدائي / تحميل / أخطاء حقول / بريد مستخدم / نجاح وانتقال / عدم اتصال.
- الأخطاء: `VALIDATION_FAILED` ← الحقول المقابلة؛ `RATE_LIMITED` ← `auth.error.rate_limited`؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C11/cases.json`
- ملاحظة الإجراءات: أزرار المصادقة ثابتة لأنها ليست تمثيلًا لطلب؛ قاعدة `available_actions` في 43 §6 تخص إجراءات الطلب.
- أسئلة مفتوحة: لا يوجد.
