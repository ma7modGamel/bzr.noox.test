# SCR-C30 — عرض التنفيذ بعد المعاينة

- النمط: C — تفاصيل.
- المرجع البصري: C08، و43 §11 (`SummaryCard` + صورة + `Countdown` + `WarningBox`).
- الطرف والوضع: العميل — السوق والموظفين.
- الدخول من: C09 في AWAITING_QUOTE_APPROVAL · الخروج إلى: C09 في IN_PROGRESS عند الموافقة، أو CLOSED/AWAITING_PAYMENT عند الرفض.
- القواعد: 11، BR-041..BR-044، BR-056، T-14/T-15/T-28.
- الـ API: `GET /orders/{order}/proposals` ثم `POST /orders/{order}/proposals/{proposal}/decide`.
- التخطيط: `AppTopBar` ← `ScreenHeading` ← `SummaryCard` للمبلغ والوصف والإجمالي المتوقع ← صورة عند `photo_path` ← `Countdown` من `expires_at` الخادم ← `WarningBox`. في وضع الموظفين يظهر `proposal.free_inspection`; لا يظهر أي خصم من رسوم المعاينة.
- الأزرار: تظهر الموافقة فقط مع `approve_proposal` والرفض فقط مع `reject_proposal`; لا تستنتج الشاشة الصلاحية من الحالة.
- الحالات: تحميل / خطأ / محتوى بلا صورة / محتوى بصورة / انتهاء المهلة / إرسال القرار.
- fixtures: `design/fixtures/SCR-C30/cases.json`.
- أسئلة مفتوحة: لا يوجد.
