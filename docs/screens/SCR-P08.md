# SCR-P08 — رئيسية الفني

- النمط: C
- المرجع البصري: SCR-C01 (الرئيسية)، `OrderCard` و`WarningBox` من معرض المكونات
- الطرف والوضع: فني بحالة `ACTIVE`؛ في الدفعة 5ب يُعتمد متغير الموظفين، مع بقاء استجابة الخادم صالحة لوضع السوق لاحقًا
- الدخول من: SCR-P07 عند `open_provider_home`، أو فتح وضع الفني لملف نشط · الخروج إلى: SCR-P12 عند `open_assigned_order`؛ وفي وضع السوق إلى P09 عند `open_available_requests` وإلى P11 عند `open_my_offers`؛ وإلى SCR-C32 بوضع الفني عند `open_notifications` (DEC-058)
- القواعد: BR-007، BR-022، DEC-057؛ لا ورديات، والإتاحة هي `available_now` فقط، والتعيين يدوي من الإدارة من المتاحين المؤهلين
- الـ API: `GET /provider/home` (يشمل `unread_notifications`) و`PUT /provider/availability`; كل انتقال من `available_actions`، ولا تعتمد الواجهة على قيمة `offers_enabled` لتكوين أفعال من عندها
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` — `provider.home.title`
  2. كارت إتاحة يحتوي `CheckRow` كمفتاح — `provider.home.available.title` و`provider.home.available.body`
  3. `InfoBanner` بعد حفظ الإتاحة — `provider.home.availability.saved`
  4. عنوان `provider.home.assigned.title`
  5. `OrderCard` للطلب النشط المعيّن: الرقم، الفئة/المشكلة، المنطقة، الموعد، و`display_status`
  6. `PrimaryButton` — `action.open_assigned_order`، يظهر فقط من `available_actions`
  7. `EmptyState` — `provider.home.no_assignment.title/body` عند عدم وجود طلب معيّن
  8. `SecondaryButton` — `action.open_notifications` مع عدد غير المقروء من `unread_notifications` (`action.open_notifications.count` عند > 0)، يظهر من `available_actions` في الوضعين
  9. في وضع السوق فقط: زرا الطلبات المتاحة و«عروضي» من `open_available_requests` و`open_my_offers`، وتحذير المستحقات كما ترسله استجابة الخادم؛ كلها مخفية في وضع الموظفين
- المتغيرات: غير متاح بلا تعيين؛ متاح بلا تعيين؛ حفظ الإتاحة؛ طلب معيّن؛ إشعارات غير مقروءة؛ خطأ شبكة
- الحالات: تحميل؛ محتوى؛ حفظ؛ خطأ قابل لإعادة المحاولة
- الأخطاء: `UNAUTHENTICATED` ← SCR-C10؛ `BUSINESS_RULE_VIOLATION` مع `rule=BR-022` ← SCR-P07؛ خطأ الشبكة ← `error.body`
- fixtures: `design/fixtures/SCR-P08/cases.json`
- أسئلة مفتوحة: لا يوجد
