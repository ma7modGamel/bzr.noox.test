# SCR-C08 — تأكيد اختيار الفني

- النمط: C — تفاصيل.
- المرجع البصري: `design/reference/C08-confirm-selection.jpeg` بعد تعديلات 27 §أ.
- الطرف والوضع: العميل، وضع السوق فقط.
- الدخول من: SCR-C06 أو C07 · الخروج إلى: SCR-C09 عند النجاح أو SCR-C06 للعودة.
- القواعد: DEC-004، DEC-005، DEC-038، BR-050.
- الـ API: `GET /orders/{id}`، العرض المختار من `GET /orders/{id}/offers`، `POST /orders/{id}/offers/{offerId}/accept` مع `payment_method` ونسخة الطلب.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `offer.confirm.title`.
  2. `ProviderHeader` متوسط.
  3. `SummaryCard` لتفاصيل العرض: السعر أو رسوم المعاينة، ETA للآن أو الموعد للمجدول، والمشمول.
  4. `SummaryCard` لتفاصيل الطلب.
  5. `RadioCard` للدفع: نقدًا بعد انتهاء الخدمة، أو دفع إلكتروني (فوري) بعد انتهاء الخدمة.
  6. في المعاينة يظهر نص الخصم من المصنعية؛ `WarningBox` يبقى كما في المرجع بصيغة محايدة.
  7. `PrimaryButton` يظهر فقط عند `accept_offer` بعنوان `offer.confirm.action`، ثم `SecondaryButton` للعودة.
- المتغيرات: تنفيذ/معاينة؛ الآن/مجدول؛ نقدي/إلكتروني.
- الحالات: عادي / تحميل التأكيد / خطأ / عدم اتصال / تعارض نسخة / نجاح.
- الأخطاء: FEATURE_DISABLED ← C36؛ INVALID_TRANSITION/CONFLICT ← إعادة تحميل العرض؛ 404 ← C06؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C08/cases.json`.
- أسئلة مفتوحة: لا يوجد.
