# SCR-C25 — طلباتي

- النمط: B — قائمة بتبويبين.
- المرجع البصري: C06 (`OrderCard`) و43 §4 (`SortChips`).
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: تبويب `nav.orders` أو الرئيسية · الخروج إلى: C09 للطلب الحالي، وC26 للطلب السابق.
- القواعد: 05، 10، 14، 19، 23.
- الـ API: `GET /orders?scope=current&page={page}` و`GET /orders?scope=past&page={page}`.
- التخطيط (DEC-076، Figma «طلباتي» 3002:3889، من أعلى لأسفل):
  1. `AppTopBar` بعنوان `orders.title`.
  2. ثلاث شرائح: `orders.current` و`orders.past` و`orders.cancelled`، والعدد جنب المختارة. «السابقة» هي نطاق `past` من غير الملغي والمنتهي، و«الملغاة» هي `CANCELLED` و`EXPIRED` منه.
  3. كارت لكل طلب: شارة `status_label` و`orders.number`، ثم نوع المشكلة وأيقونة الفئة والمكان.
  4. بعد تعيين الفني: شريط الفني (الاسم، والتوثيق، والتقييم، وزرار اتصال لو الرقم موجود)، ثم `orders.total` بالمبلغ، وزرار المحادثة، و`orders.follow` (مش ظاهر للطلبات المنتهية).
  5. الطلب المفتوح: شريط العروض (`customer.home.offers.new` و`orders.offers_from`) من `GET /orders/{id}/offers`، و`customer.home.offers.action`.
  6. حالة فارغة لكل شريحة: `orders.current.empty.*` أو `orders.past.empty.*` أو `orders.cancelled.empty.*`.
- السلوك: كل تبويب يحتفظ بصفحاته وموضع تمريره. الترتيب `created_at DESC, id DESC`. الضغط على الحالي يفتح C09؛ الضغط على CLOSED/CANCELLED/EXPIRED يفتح C26. زرار المحادثة بيفتح محادثة الطلب، والاتصال بيفتح شاشة الاتصال برقم الفني (BR-101).
- الحالات: تحميل / خطأ / الحالي فارغ / السابق فارغ / طلب حالي واحد / عدة طلبات سابقة / تحميل صفحة تالية.
- الأخطاء: `UNAUTHENTICATED` يعيد للدخول؛ خطأ الشبكة يعرض `ErrorState` مع إعادة المحاولة.
- fixtures: `design/fixtures/SCR-C25/cases.json`.
- أسئلة مفتوحة: لا يوجد.
