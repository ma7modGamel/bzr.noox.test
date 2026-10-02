# SCR-C06 — عروض الفنيين

- النمط: B — قائمة.
- المرجع البصري: `design/reference/C06-offers-list.jpeg` بعد تعديلات 27 §أ.
- الطرف والوضع: العميل، وضع السوق فقط؛ لا تظهر في وضع الموظفين.
- الدخول من: نشر طلب سوق أو طلب OPEN · الخروج إلى: SCR-C07، SCR-C08، SCR-C03 للتعديل، SCR-C20 للإلغاء، أو SCR-C35 عند الفراغ.
- القواعد: DEC-003..005، DEC-028، DEC-031، BR-021.
- الـ API: `GET /orders/{id}`، `GET /orders/{id}/offers?sort=rating|price|eta`.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `offers.title`.
  2. كارت ملخص المشكلة و`display_status`.
  3. `Countdown` من `deadlines.offers_close_at` أو `selection_deadline_at`.
  4. `SortChips`: الأعلى تقييمًا، أفضل سعر، والأسرع فقط للطلبات NOW.
  5. قائمة `OfferCard`; المجدول يخفي ETA، والمعاينة تعرض رسوم المعاينة وشارة الخصم.
  6. زر اختيار العرض داخل كل كارت يظهر فقط عند وجود `accept_offer` في `available_actions`؛ فتح الملف يذهب C07.
  7. `edit_request` يظهر قبل أول عرض فقط؛ `cancel` يظهر فقط من `available_actions`.
- المتغيرات: تنفيذ/معاينة؛ الآن/مجدول؛ عروض/لا عروض؛ نافذة مفتوحة/منتهية؛ الفرز الثلاثي أو الثنائي.
- الحالات: تحميل / خطأ / عدم اتصال / لا عروض C35 / قائمة عروض / انتهاء المهلة.
- الأخطاء: FEATURE_DISABLED ← C36؛ INVALID_TRANSITION أو CONFLICT ← إعادة تحميل؛ 404 ← طلباتي؛ RATE_LIMITED ← رسالة مشتركة.
- عقد 6ب: عنوان الطلب والعدّاد والمهلة والفرز وكل `OfferCard` تُبنى من استجابة الطلب والعروض؛ لا أسماء أو تقييمات أو أسعار أو ETA تجريبية، ويُمرر `offer_id` و`provider_id` الفعليان إلى C07/C08.
- fixtures: `design/fixtures/SCR-C06/cases.json`.
- أسئلة مفتوحة: لا يوجد.
