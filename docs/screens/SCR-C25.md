# SCR-C25 — طلباتي

- النمط: B — قائمة بتبويبين.
- المرجع البصري: C06 (`OrderCard`) و43 §4 (`SortChips`).
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: تبويب `nav.orders` أو الرئيسية · الخروج إلى: C09 للطلب الحالي، وC26 للطلب السابق.
- القواعد: 05، 10، 14، 19، 23.
- الـ API: `GET /orders?scope=current&page={page}` و`GET /orders?scope=past&page={page}`.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `orders.title`.
  2. `SortChips`: `orders.current` و`orders.past`.
  3. `OrderCard` لكل طلب: الفئة ونوع المشكلة، رقم الطلب والمنطقة والموعد، و`status_label` القادم من الخادم.
  4. حالة فارغة مختلفة لكل تبويب: `orders.current.empty.*` أو `orders.past.empty.*`.
- السلوك: كل تبويب يحتفظ بصفحاته وموضع تمريره. الترتيب `created_at DESC, id DESC`. الضغط على الحالي يفتح C09؛ الضغط على CLOSED/CANCELLED/EXPIRED يفتح C26. لا تعرض القائمة عنوانًا تفصيليًا أو أزرار إجراءات.
- الحالات: تحميل / خطأ / الحالي فارغ / السابق فارغ / طلب حالي واحد / عدة طلبات سابقة / تحميل صفحة تالية.
- الأخطاء: `UNAUTHENTICATED` يعيد للدخول؛ خطأ الشبكة يعرض `ErrorState` مع إعادة المحاولة.
- fixtures: `design/fixtures/SCR-C25/cases.json`.
- أسئلة مفتوحة: لا يوجد.
