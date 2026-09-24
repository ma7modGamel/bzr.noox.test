# SCR-C10 — الترحيب / الدخول

- النمط: A
- المرجع البصري: C04 (الحقول)، C03 (الزر الرئيسي)
- الطرف والوضع: العميل، قبل تسجيل الدخول
- الدخول من: تشغيل التطبيق أو انتهاء الجلسة · الخروج إلى: SCR-C01 للحساب المفعّل، أو SCR-C12 للحساب غير المفعّل، أو SCR-C11/SCR-C13 من روابط الشاشة
- القواعد: BR-003، BR-004، DEC-025
- الـ API: `POST /auth/login`
- التخطيط (من أعلى لأسفل):
  1. اسم التطبيق `app.name`.
  2. عنوان `auth.login.title` ووصف `auth.login.body`.
  3. `AppTextField` للبريد: `auth.email.label` و`auth.email.placeholder`.
  4. `SecureTextField` لكلمة المرور: `auth.password.label` و`auth.password.placeholder`.
  5. `PrimaryButton` بالمفتاح `auth.login.action`.
  6. `SecondaryButton` بالمفتاح `auth.register.open` إلى SCR-C11.
  7. `LinkButton` بالمفتاح `auth.password.forgot.open` إلى SCR-C13.
- التحقق المحلي: البريد مطلوب وبصيغة بريد؛ كلمة المرور مطلوبة. لا يرسل الطلب قبل نجاح التحقق.
- تحويل الاستجابة: `user.is_verified=true` يفتح SCR-C01، و`false` يفتح SCR-C12، ويحفظ رمز Sanctum في مخزن الأسرار الخاص بالمنصة.
- الحالات: فراغ ابتدائي / تحميل / خطأ حقول / بيانات دخول غير صحيحة / حساب محظور / نجاح مفعّل / نجاح غير مفعّل / عدم اتصال.
- الأخطاء: `VALIDATION_FAILED` ← أخطاء الحقول أو `auth.error.credentials`؛ `ACCOUNT_BLOCKED` ← `auth.error.blocked`؛ `RATE_LIMITED` ← `auth.error.rate_limited`؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C10/cases.json`
- ملاحظة الإجراءات: أزرار المصادقة ثابتة لأنها ليست تمثيلًا لطلب؛ قاعدة `available_actions` في 43 §6 تخص إجراءات الطلب.
- أسئلة مفتوحة: لا يوجد.
