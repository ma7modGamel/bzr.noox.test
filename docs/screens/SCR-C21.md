# SCR-C21 — ملخص المبلغ والدفع

- النمط: C — تفاصيل.
- المرجع البصري: C08، و43 §4 (`SummaryCard` + `RadioCard`).
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: C09 بعد T-13 أو T-15 أو T-17 · الخروج إلى: C22 عند الدفع الإلكتروني، أو C09 أثناء انتظار تأكيد الاستلام النقدي.
- القواعد: BR-044، BR-050..BR-055، 14، 19، T-19/T-20.
- الـ API: `GET /orders/{order}` و`PATCH /orders/{order}/payment-method`.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `payment.summary.title`.
  2. `ScreenHeading` بالمفتاح `payment.summary.heading`.
  3. `SummaryCard` لصفوف `payment.amount.labor` و`payment.amount.materials` و`payment.amount.total` من `amounts` في استجابة الطلب.
  4. عنوان قسم `payment.method.title` ثم `RadioCard` للنقدي والإلكتروني، مع تحديد `amounts.payment_method`.
  5. `InfoBanner`: تعليمات الطريقة المختارة؛ النص `payment.method.cash.help` أو `payment.method.electronic.help`.
  6. زر `PrimaryButton`: `action.pay_electronic` فقط عندما يرسله `available_actions`. إجراء `change_payment_method` وحده يسمح بتبديل بطاقة الاختيار وإرسالها للخادم.
- السلوك: اختيار طريقة مختلفة يرسل `expected_version` ولا يغيّر الحالة محليًا قبل نجاح الخادم. تعارض النسخة يعيد تحميل الطلب. لا تُحسب المبالغ في التطبيق ولا تُستنتج صلاحية الأزرار من الحالة.
- الحالات: تحميل / خطأ شبكة / ملخص نقدي / ملخص إلكتروني / تبديل جارٍ / إجراء غير متاح.
- الأخطاء: `CONFLICT` يعيد التحميل؛ `INVALID_TRANSITION` يعيد C09 بالحالة الحديثة؛ بقية أخطاء الخادم تظهر في `ErrorState` مع إعادة المحاولة.
- fixtures: `design/fixtures/SCR-C21/cases.json`.
- أسئلة مفتوحة: لا يوجد.
