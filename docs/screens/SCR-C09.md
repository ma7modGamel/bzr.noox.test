# SCR-C09 — متابعة الطلب

- النمط: D — متابعة حالة.
- المرجع البصري: `design/reference/C09-tracking-on-the-way.jpeg` بعد تعديلات 27 §أ و§أ-2.
- الطرف والوضع: العميل، في وضعي الموظفين والسوق بعد تعيين الفني.
- الدخول من: تأكيد العرض، التعيين الإداري، الرئيسية، طلباتي · الخروج إلى: المحادثة، المشاركة، الإلغاء، المشكلة، الدفع أو التأكيد وفق `available_actions`.
- القواعد: BR-070، DEC-030، DEC-037، DEC-039، CFG-081.
- الـ API: `GET /orders/{id}`، و`GET /orders/{id}/tracking` في ON_THE_WAY فقط؛ إجراءات الطلب من المسارات المطابقة لأسماء `available_actions`.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان من `display_status`.
  2. CONFIRMED: `EtaCard` بنص `tracking.waiting_to_move` بلا خريطة.
  3. ON_THE_WAY فقط: `EtaCard` من ETA الخادم، وإضافة `tracking.approximate` عند `eta_approximate=true`، ثم خريطة النظام الأصلية داخل `MapCard` (Google Maps في Android وMapKit في iOS) بدبوسي الفني ووجهة الطلب. يعاد `GET /tracking` كل 30 ثانية ما دامت الشاشة ظاهرة والحالة ON_THE_WAY؛ التطبيقات لا تتصل بخدمة Routes ولا تحسب ETA.
  4. `StatusStepper` كما يرسله الخادم: تم التأكيد ← في الطريق ← وصل ← جاري التنفيذ ← الدفع ← مكتمل؛ في وضع الموظفين تبدأ تسمية المرحلة الأولى بـ`tracking.step.assignment`.
  5. DISPUTED: آخر مرحلة `on_hold` و`WarningBox` بالمفتاح `tracking.on_hold`.
  6. `ProviderHeader` متوسط.
  7. زرا اتصال ومحادثة لا يظهران إلا من `available_actions`.
  8. تفاصيل الطلب المختصرة.
  9. `InfoBanner` لمشاركة تفاصيل الزيارة عند `share_visit`.
  10. قبل الوصول يظهر الإلغاء فقط عند `cancel`; من ARRIVED فصاعدًا يظهر `open_dispute` بعنوان `tracking.problem` بدل الإلغاء.
- المتغيرات: CONFIRMED / ON_THE_WAY / ARRIVED / IN_PROGRESS / AWAITING_PAYMENT / CLOSED / DISPUTED؛ موظفين/سوق؛ ETA دقيق/تقريبي.
- الحالات: تحميل / خطأ / عدم اتصال مع آخر قراءة مخزنة / كل حالات المتابعة أعلاه.
- الأخطاء: 404 ← طلباتي؛ INVALID_TRANSITION/CONFLICT ← إعادة التحميل؛ خطأ Routes لا يصل للتطبيق كخطأ، بل ETA تقريبي؛ خطأ النقل ← `error.body`.
- fixtures: `design/fixtures/SCR-C09/cases.json`.
- أسئلة مفتوحة: لا يوجد.
