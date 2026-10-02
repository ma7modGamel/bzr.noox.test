# SCR-P03 — إثبات الهوية

- النمط: A
- المرجع البصري: SCR-C17 (منتقي الصور)، SCR-C34 (النص التوضيحي)
- الطرف والوضع: طالب الانضمام؛ المستندات خاصة ولا تُعرض إلا للإدارة
- الدخول من: SCR-P02 · الخروج إلى: SCR-P04 محليًا بعد اختيار الوجهين
- القواعد: DEC-022، DEC-033؛ صورة وجه البطاقة وظهرها إلزاميتان، التخزين خاص، والتوثيق النهائي يدوي
- الـ API: يرفع الملفان إلى `POST /media` ثم يرسل SCR-P06 المعرّفين `id_front_media_id` و`id_back_media_id` إلى `POST /provider/application`؛ لا يُعاد رابط خاص في الاستجابة
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.onboarding.identity.title`
  2. نص تقدم — `provider.onboarding.step.2`
  3. `InfoBanner` — `provider.onboarding.identity.private`
  4. منتقي صورة — `provider.onboarding.identity.front`
  5. منتقي صورة — `provider.onboarding.identity.back`
  6. `PrimaryButton` — `common.next`
- المتغيرات: فارغ؛ وجه واحد؛ الوجهان؛ إعادة تقديم مع مؤشري «مرفوع» بلا تنزيل للمستند
- الحالات: خطأ قراءة الملف؛ صيغة/حجم مرفوضان؛ محتوى
- الأخطاء: ملف مفقود ← `provider.onboarding.identity.required`؛ ملف غير صورة أو أكبر من 10MB ← `media.error.invalid`
- fixtures: `design/fixtures/SCR-P03/cases.json`
- أسئلة مفتوحة: لا يوجد
