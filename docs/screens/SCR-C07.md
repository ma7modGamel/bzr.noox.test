# SCR-C07 — ملف الفني

- النمط: C — تفاصيل.
- المرجع البصري: `design/reference/C07-provider-profile.jpeg` بعد تعديلات 27 §أ.
- الطرف والوضع: العميل، وضع السوق فقط.
- الدخول من: `OfferCard` في SCR-C06 · الخروج إلى: SCR-C06، SCR-C08، SCR-C28.
- القواعد: BR-005، BR-121، DEC-035.
- الـ API: `GET /providers/{id}` مع `order_id` لربط `available_actions` بالسياق.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `provider.profile.title` وعلم البلاغ إلى SCR-C28.
  2. `ProviderHeader` كبير: الصورة والدرع والاسم والفئة والتقييم والخدمات؛ `provider.available_now` فقط إذا كانت القيمة true.
  3. `StatRow`: التقييم، الخدمات، سنوات الخبرة.
  4. تخصصات كـ `SelectableChip` معلوماتية.
  5. معرض الأعمال، ثم `RatingBars` و`ReviewCard` للتقييمات المرقمة.
  6. `InfoBanner` للتوثيق.
  7. `StickyActionBar`: السعر وزر `provider.choose`; يظهر الاختيار فقط إذا كان `accept_offer` ضمن `available_actions`، والمحادثة فقط عند `chat`.
- المتغيرات: متاح الآن/غير متاح؛ معرض موجود/فارغ؛ تقييمات موجودة/فارغة.
- الحالات: تحميل / خطأ / عدم اتصال / ملف محمّل.
- الأخطاء: 404 ← العودة للعروض وإعادة تحميلها؛ FEATURE_DISABLED ← C36؛ خطأ النقل ← `error.body`.
- عقد 6ب: الاسم والصورة والتوثيق والإحصاءات والتخصصات والنبذة والتقييمات والمراجعات وسعر العرض كلها من `GET /providers/{id}?order_id=...`، ولا يظهر إجراء إلا إذا أعاده `available_actions`.
- fixtures: `design/fixtures/SCR-C07/cases.json`.
- أسئلة مفتوحة: لا يوجد.
