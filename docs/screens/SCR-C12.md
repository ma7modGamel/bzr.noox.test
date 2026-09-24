# SCR-C12 — تفعيل البريد

- النمط: `EmptyState` + زر
- المرجع البصري: حالات `EmptyState` و`ErrorState` في معرض المكونات
- الطرف والوضع: العميل المسجل صاحب بريد غير مفعّل
- الدخول من: SCR-C11 أو SCR-C10 بعد دخول حساب غير مفعّل · الخروج إلى: SCR-C01 بعد أن يعيد `/me` مستخدمًا مفعّلًا، أو SCR-C10 بعد تسجيل الخروج
- القواعد: BR-001، BR-004، DEC-025
- الـ API: `POST /auth/email/resend`، `GET /me`، `POST /auth/logout`
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `auth.verify.title`.
  2. `EmptyState` بعنوان `auth.verify.waiting.title` ونص `auth.verify.waiting.body` متبوعًا بالبريد المحفوظ.
  3. `PrimaryButton` بالمفتاح `auth.verify.check` لفحص `/me`.
  4. `SecondaryButton` بالمفتاح `auth.verify.resend` لإعادة الإرسال.
  5. `LinkButton` بالمفتاح `auth.logout.action` لتسجيل الخروج والعودة إلى SCR-C10.
- تحويل الاستجابة: `/me.user.is_verified=true` يفتح SCR-C01؛ `false` يبقي حالة الانتظار. نجاح إعادة الإرسال يعرض `auth.verify.resent` من دون تغيير المسار.
- الحالات: انتظار / تحميل الفحص / تحميل إعادة الإرسال / تم الإرسال / غير مفعّل بعد / خطأ / عدم اتصال.
- الأخطاء: `EMAIL_ALREADY_VERIFIED` ← الانتقال إلى SCR-C01 بعد إعادة فحص `/me`؛ `RATE_LIMITED` ← `auth.error.rate_limited`؛ `UNAUTHENTICATED` ← مسح الجلسة وفتح SCR-C10؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C12/cases.json`
- ملاحظة دورة الحياة: رابط البريد يفتح صفحة W02 الموجودة في جرد الشاشات؛ هذه الشاشة لا تخمّن رمزًا أو آلية تفعيل بديلة.
- ملاحظة الإجراءات: أزرار المصادقة ثابتة لأنها ليست تمثيلًا لطلب؛ قاعدة `available_actions` في 43 §6 تخص إجراءات الطلب.
- أسئلة مفتوحة: لا يوجد.
