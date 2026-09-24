# SCR-C31 — الموافقة على تكلفة إضافية

- النمط: E — `AppBottomSheet` فوق C09 أثناء IN_PROGRESS.
- المرجع البصري: C30 مصغّرًا وفق 43 §11.
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: C09 عند وجود مقترح MATERIALS أو EXTRA_WORK معلّق · الخروج إلى: C09 بعد القرار.
- القواعد: 12، BR-040..BR-045.
- الـ API: `GET /orders/{order}/proposals` ثم `POST /orders/{order}/proposals/{proposal}/decide`.
- التخطيط: عنوان نوع المقترح ← المبلغ والسبب والإجمالي الجديد في `SummaryCard` ← صورة اختيارية ← `Countdown` ← `WarningBox` بأن البند متوقف حتى القرار ← زرا الموافقة والرفض من `available_actions` فقط.
- الحالات: تحميل / خطأ / خامات / عمل إضافي / بصورة / انتهاء المهلة / إرسال القرار.
- fixtures: `design/fixtures/SCR-C31/cases.json`.
- أسئلة مفتوحة: لا يوجد.
