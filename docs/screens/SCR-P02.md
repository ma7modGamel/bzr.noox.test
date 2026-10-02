# SCR-P02 — بيانات الفني

- النمط: A
- المرجع البصري: SCR-C15 (نموذج)، SCR-C17 (اختيار صورة)
- الطرف والوضع: مستخدم موثّق البريد يبدأ طلب الانضمام؛ لا يظهر نوع التعاقد
- الدخول من: SCR-P01 أو SCR-P07 عند إعادة التقديم · الخروج إلى: SCR-P03 محليًا بعد نجاح التحقق
- القواعد: DEC-022؛ الصورة الشخصية إلزامية، الخبرة رقم من 0 إلى 50، النبذة اختيارية وبحد 300 حرف
- الـ API: الصورة تُرفع أولًا إلى `POST /media`، وتحتفظ الطبقة المشتركة بـ`profile_photo_media_id`؛ الحالة الأساسية من `GET /provider/application`، والإرسال النهائي في SCR-P06
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.onboarding.data.title`
  2. نص تقدم — `provider.onboarding.step.1`
  3. منتقي صورة دائري — `provider.onboarding.profile_photo`
  4. `AppTextField` رقمي — `provider.onboarding.experience.label`
  5. `TextAreaField` — `provider.onboarding.bio.label` و`provider.onboarding.bio.placeholder`
  6. `PrimaryButton` — `common.next`
- المتغيرات: فارغ؛ صالح؛ صورة مختارة؛ إعادة تقديم ببيانات سابقة قابلة للتعديل
- الحالات: تحميل؛ خطأ اختيار/قراءة الصورة؛ أخطاء حقول؛ محتوى
- الأخطاء: صورة مفقودة ← `provider.onboarding.profile_photo.required`؛ خبرة خارج النطاق ← `provider.onboarding.experience.invalid`؛ نبذة أطول من 300 ← `provider.onboarding.bio.max`
- fixtures: `design/fixtures/SCR-P02/cases.json`
- أسئلة مفتوحة: لا يوجد
