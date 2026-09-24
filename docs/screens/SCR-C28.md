# SCR-C28 — الإبلاغ عن فني

- النمط: E — `AppBottomSheet` فوق C07.
- المرجع البصري: C20 (`RadioCard` + `TextAreaField`) و43 §7.
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: `report_provider` في C07 فقط · الخروج إلى: C07 بعد الرجوع أو النجاح.
- القواعد: BR-121، 16، 23.
- الـ API: الأسباب من `GET /config` في `option_lists.provider_report_reasons`؛ `POST /providers/{provider}/reports` يرسل `order_id` والسّبب والتفاصيل الاختيارية.
- التخطيط: عنوان `provider.report.title` ← نص `provider.report.body` ← `RadioCard` للأسباب التي يعيدها الخادم ← `TextAreaField` اختياري للتفاصيل ← `PrimaryButton` بالمفتاح `provider.report.submit` ← زر رجوع ثانوي.
- السلوك: الإرسال متاح فقط مع `report_provider` القادم من ملف الفني؛ البلاغ ملف مستقل للإدارة ولا يغير الطلب أو حالة الفني مباشرة. أثناء الإرسال تُعطّل جميع المدخلات، والنجاح يعرض `SuccessState` ثم يعود إلى C07.
- الحالات: دون اختيار / سبب مختار / تفاصيل / إرسال / نجاح / خطأ.
- الأخطاء: `404` لفني غير ظاهر للعميل يعيد C07؛ `VALIDATION_FAILED` يظهر عند الحقول؛ حد المعدل يعرض `RATE_LIMITED`.
- fixtures: `design/fixtures/SCR-C28/cases.json`.
- الأسباب الابتدائية المدخلة بالسيدر، مع إمكان تعديل التسمية والترتيب والتفعيل من الداشبورد: `INAPPROPRIATE_BEHAVIOR` سلوك غير لائق؛ `HARASSMENT_OR_ABUSE` تحرش أو إساءة؛ `FRAUD_SUSPECTED` اشتباه احتيال؛ `OFF_PLATFORM_REQUEST` طلب تواصل أو دفع خارج التطبيق؛ `FALSE_INFORMATION` معلومات مهنية مضللة؛ `SAFETY_CONCERN` تهديد للسلامة؛ `OTHER` سبب آخر.
- لا توجد أسئلة مفتوحة.
