# SCR-P11 — عروضي

- النمط: B — قائمة كروت.
- المرجع البصري: SCR-C25 مع `OrderCard` و`Badge` لحالة العرض.
- الطرف والوضع: فني نشط في وضع السوق فقط.
- الدخول من: SCR-P08 عبر `open_my_offers` أو نجاح SCR-P10 · الخروج إلى: SCR-P09 عند فتح الطلب، وإلى SCR-P08 عند الرجوع.
- القواعد: BR-030..034 و09 §دورة العرض؛ القائمة تعرض عروض الفني وحده بكل الحالات، ولا تسمح بتعديل عرض مرسل.
- الـ API: `GET /provider/offers` و`POST /provider/offers/{id}/withdraw`. فتح التفاصيل والسحب يظهران فقط من `available_actions` لكل عنصر.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `provider.offers.title`.
  2. `EmptyState` بعنوان ونص `provider.offers.empty.*` عند غياب العروض.
  3. لكل عرض `OrderCard`: رقم الطلب، الفئة/المشكلة، المنطقة والتوقيت، السعر وصافي الفني.
  4. `Badge` بنص `display_status` من الخادم لحالات SUBMITTED/WITHDRAWN/ACCEPTED/NOT_SELECTED/CLOSED/BACKED_OUT؛ لا يترجم التطبيق code محليًا.
  5. `SecondaryButton` لـ`open_available_request` و`withdraw_offer` حسب `available_actions` للعنصر.
  6. `AppBottomSheet` قبل السحب، بعنوان ونص `provider.offers.withdraw.*`؛ التأكيد يستدعي O-02 ثم يحدّث العنصر من استجابة الخادم.
- المتغيرات: تحميل؛ خطأ؛ فارغ؛ عروض متعددة بكل الحالات؛ تأكيد سحب؛ سحب جارٍ؛ سحب ناجح؛ فشل السحب بعد تغيّر الحالة.
- الحالات: تحميل؛ محتوى؛ فارغ؛ خطأ قابل لإعادة المحاولة؛ ورقة تأكيد؛ تنفيذ؛ نتيجة.
- الأخطاء: `FEATURE_DISABLED` ← SCR-P08؛ BR-030 أو تعارض نسخة ← إغلاق الورقة وإعادة تحميل القائمة؛ خطأ الشبكة ← `error.body`.
- fixtures: `design/fixtures/SCR-P11/cases.json`.
- أسئلة مفتوحة: لا يوجد.
