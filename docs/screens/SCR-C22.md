# SCR-C22 — الدفع الإلكتروني وكود المنافذ

- النمط: C — تفاصيل.
- المرجع البصري: C08، و43 §4 (`RadioCard` + `Countdown`). شاشة البطاقة المستضافة من مزود الدفع عنصر نظام مسموح اختلافه وفق 43 §7.
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: C21 عبر `pay_electronic` · الخروج إلى: C09/C24 بعد نجاح T-20، أو C21 عند الفشل والانتهاء.
- القواعد: BR-050..BR-055، 19، T-20، EC-15.
- الـ API: `POST /orders/{order}/payments` فقط؛ التطبيق لا يتصل بفوري مباشرة.
- التخطيط (من أعلى لأسفل):
  1. `AppTopBar` بعنوان `payment.electronic.title`.
  2. `ScreenHeading` بالمفتاح `payment.electronic.heading`.
  3. `SummaryCard` بالإجمالي الثابت من الخادم.
  4. عنوان `payment.channel.title` وثلاثة `RadioCard`: `payment.channel.card` و`payment.channel.wallet` و`payment.channel.kiosk`.
  5. قبل الإنشاء: `PrimaryButton` بالمفتاح `payment.create` يظهر فقط مع `pay_electronic`.
  6. لمحاولة البطاقة أو المحفظة المعلقة: `InfoBanner` بالمفتاح `payment.checkout.ready` وزر `payment.checkout.open` للرابط الذي أعاده الخادم.
  7. لمحاولة منافذ فوري: رقم المرجع في `SummaryCard` مع `payment.kiosk.reference`، وزر نسخ `payment.kiosk.copy`، و`Countdown` من `expires_at`.
  8. للنجاح: `SuccessState` بالمفتاح `payment.success`; للفشل `ErrorState` بالمفتاح `payment.failed`; وللانتهاء `ErrorState` بالمفتاح `payment.expired`.
- السلوك: الخادم وحده ينشئ `merchant_ref` ويتحقق من webhook والتوقيع والمبلغ والتكرار. التطبيق يرسل القناة و`expected_version` ويعرض الحقول المعادة. زر إعادة المحاولة يظهر فقط إذا أعادت استجابة الطلب `pay_electronic`. النسخ ينسخ رقم المرجع فقط.
- المحاكي: على staging يمر الطلب عبر نفس `PaymentGateway` ويمكن للخادم محاكاة `PENDING` و`SUCCESS` و`FAILURE` و`EXPIRY`; اختيار `KIOSK` يعيد كود منافذ. لا يوجد اختيار محاكاة في واجهة العميل، ولا يُقبل معامل المحاكاة في الإنتاج.
- الحالات: تحميل / اختيار قناة / إنشاء / بطاقة أو محفظة معلقة / كود منافذ / نجاح / فشل / انتهاء / خطأ شبكة.
- الأخطاء: `CONFLICT` و`INVALID_TRANSITION` يعيدان تحميل الطلب؛ فشل إنشاء المحاولة يعرض إعادة المحاولة إن بقي الإجراء متاحًا.
- fixtures: `design/fixtures/SCR-C22/cases.json`.
- أسئلة مفتوحة: لا يوجد.
