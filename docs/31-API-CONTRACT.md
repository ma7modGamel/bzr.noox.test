# 31 — واجهات API

> **العقد المولَّد**: `docs/api/openapi.json` يُولَّد من جدول المسارات الحقيقي بـ `php artisan api:spec`،
> و`--check` يكسر البناء إذا انحرف. هذه الوثيقة تبقى المرجع للقواعد والسلوك؛ الملف المولَّد هو الشكل الملزم للتطبيقين (41 §البوابة 1.9).

## الاتفاقيات
- المسار الأساسي: `/api/v1`. JSON. المصادقة: `Authorization: Bearer <sanctum token>`.
- الوضع: `X-App-Mode: CUSTOMER | PROVIDER` (يحدد ما يُعرض فقط؛ الصلاحية من الخادم).
- الإجراءات (POST) تقبل `Idempotency-Key` (UUID)؛ نفس المفتاح خلال 24 ساعة يعيد نفس النتيجة.
- الإجراءات على الطلب تقبل `expected_version` اختياريًا ← `409 CONFLICT` عند الاختلاف.
- **لا يوجد أي endpoint لتعديل الحالة مباشرة.**
- الميزات المرتبطة بوضع التشغيل (39) تُقرأ من `GET /config`، وأي إجراء يخص ميزة معطّلة يُرفض بـ `FEATURE_DISABLED` مهما كانت صلاحية صاحبه.
- التواريخ ISO-8601 UTC. المبالغ نصوص عشرية ("350.00").

## الأخطاء
| الكود | HTTP | المعنى |
|---|---|---|
| `VALIDATION_FAILED` | 422 | حقول غير صالحة |
| `UNAUTHENTICATED` | 401 | — |
| `EMAIL_NOT_VERIFIED` | 403 | BR-001 — يفتح التطبيق شاشة التفعيل مباشرة (42) |
| `ACCOUNT_BLOCKED` | 403 | BR-003 |
| `NOT_FOUND` | 404 | يشمل المورد غير المملوك: فشل السياسة يُترجم 404 لا 403 (23 §قاعدة الملكية) |
| `INVALID_TRANSITION` | 409 | الإجراء غير مسموح في الحالة الحالية |
| `CONFLICT` | 409 | تعارض نسخة أو تزامن |
| `BUSINESS_RULE_VIOLATION` | 422 | مع `rule: "BR-0xx"` |
| `FEATURE_DISABLED` | 409 | الإجراء يخص ميزة معطّلة في الوضع الحالي (CFG-090/091) |
| `RATE_LIMITED` | 429 | — |

## الهوية والحساب
| الطريقة | المسار | ملاحظة |
|---|---|---|
| POST | `/auth/register` | اسم، بريد، هاتف، كلمة مرور ← يرسل بريد التفعيل |
| POST | `/auth/login` | ← token |
| POST | `/auth/logout` | |
| POST | `/auth/email/resend` | حد 3/ساعة |
| GET | `/auth/email/verify/{id}/{hash}` | رابط موقّع ومحدود المعدل؛ يفعّل البريد تكراريًا بأمان |
| POST | `/auth/password/forgot` | يعيد 204 للبريد الموجود وغير الموجود حتى لا يكشف الحسابات |
| POST | `/auth/password/reset` | بريد + token + كلمة مرور (8 أحرف على الأقل)؛ يلغي كل رموز Sanctum عند النجاح |
| GET/PATCH | `/me` | الاسم، البريد للعرض، الهاتف، الصورة، و`available_actions` للحساب |
| POST | `/me/password` | الحالية + الجديدة + التأكيد؛ يلغي الخطأ الحفظ كله |
| DELETE | `/me` | حذف ناعم وإخفاء البيانات وإلغاء الجلسات؛ مرفوض بـ`ACTIVE_ORDER_EXISTS` مع طلب نشط |
| POST/DELETE | `/me/devices` | رمز FCM |

## المراجع
`GET /cities`, `GET /cities/{id}/areas`, `GET /catalog?city_id=`, `GET /slots?city_id=&date=`, `GET /terms/current`.

`GET /config` — بلا مصادقة، يُقرأ عند إقلاع التطبيق وعند العودة للمقدمة:
```json
{
  "tokens_version": "1",
  "offers_enabled": false,
  "inspection_fee_enabled": false,
  "service_hours": {"from":"08:00","to":"22:00"},
  "slots": {"duration_minutes": 120, "minimum_lead_minutes": 90, "booking_horizon_days": 7},
  "limits": {"max_offers": 10, "min_offer_amount": "50.00", "offer_window_minutes": 30},
  "media_limits": {"images":5,"image_mb":10,"video_seconds":60,"video_mb":50,"audio_seconds":120,"audio_mb":5,"audio_mime":"audio/mp4","audio_channels":1,"audio_bitrate_bps":64000},
  "option_lists": {
    "customer_cancellation_reasons": [{"code":"FOUND_ANOTHER","label":"وجدت فنيًا آخر"}],
    "dispute_reasons": [{"code":"WORK_QUALITY","label":"جودة التنفيذ غير مرضية"}],
    "provider_report_reasons": [{"code":"INAPPROPRIATE_BEHAVIOR","label":"سلوك غير لائق"}],
    "timing_types": [{"code":"NOW","label":"الآن"}],
    "materials_responsibilities": [{"code":"UNSURE","label":"غير محدد"}],
    "pricing_modes": [{"code":"EXECUTION","label":"استقبال عروض تنفيذ"}],
    "payment_methods": [{"code":"CASH","label":"نقدي"}],
    "payment_channels": [{"code":"CARD","label":"بطاقة بنكية"}]
  },
  "option_defaults": {
    "timing_type": "NOW",
    "materials_responsibility": "UNSURE",
    "payment_method": "CASH"
  },
  "terms_version": 3,
  "minimum_supported_app_version": {"android": "1.0.0", "ios": "1.0.0"}
}
```
التطبيق يخفي شاشات العروض وحقل طريقة التسعير والميزانية عندما `offers_enabled = false`، ويخفي رسوم المعاينة عندما `inspection_fee_enabled = false` (39). الإخفاء للعرض فقط؛ الحسم على الخادم.

كل `option_lists` مصدره الخادم، وتعرض التطبيقات `label` وترسل `code` دون أي قائمة أو قاموس تسميات ثابت في كود الهاتف. أسباب C27 وC28 يديرها فريق التشغيل من الداشبورد؛ لا يعيد `/config` إلا الأسباب الفعالة مرتبة، ويرفض الخادم أي كود غير فعال حتى لو ظل محفوظًا مؤقتًا على الهاتف.

## استجابة الطلب للعميل والفني
كل تمثيل تفاصيل للطلب يتضمن حقول العرض المحسوبة من منظور الطرف الطالب:
```json
{
  "available_actions": ["start_trip", "back_out", "call", "chat"],
  "deadlines": {
    "offers_close_at": null,
    "selection_deadline_at": null,
    "proposal_expires_at": null,
    "no_show_allowed_at": null,
    "auto_close_at": null
  },
  "display_status": "order.status.provider.CONFIRMED",
  "stepper": [
    {"key": "confirmed",   "state": "done"},
    {"key": "on_the_way",  "state": "active"},
    {"key": "arrived",     "state": "pending"},
    {"key": "in_progress", "state": "pending"},
    {"key": "payment",     "state": "pending"},
    {"key": "closed",      "state": "pending"}
  ]
}
```

قيم `stepper[].state`: `done | active | pending | on_hold`. في `OPEN` و`CANCELLED` و`EXPIRED` تكون `stepper = null` ويُعرض كارت الحالة والسبب بدلًا منها. في `DISPUTED` يُبنى الشريط من `disputed_from_status`، وتصبح آخر مرحلة قبل النزاع `on_hold` بلون `star` وتظهر رسالة «قيد المراجعة».

- `available_actions` تُحسب من جدول الانتقالات (10) والصلاحيات (23) للطرف الطالب، وتُستخدم نفس الدالة في التحقق عند تنفيذ الإجراء (مصدر واحد).
- أسماء إجراءات العميل الثابتة: `accept_offer`, `edit_request`, `republish`, `cancel`, `change_payment_method`, `pay_electronic`, `approve_proposal`, `reject_proposal`, `confirm_completion`, `open_dispute`, `rate`, `share_visit`, `call`, `chat`, `report_provider`.
- أسماء إجراءات الفني الثابتة: `submit_offer`, `withdraw_offer`, `start_trip`, `back_out`, `mark_arrived`, `start_work`, `submit_execution_quote`, `complete_inspection_only`, `submit_proposal`, `withdraw_proposal`, `complete_work`, `report_unable`, `report_no_show`, `confirm_cash`, `open_dispute`, `rate_customer`, `navigate`, `call`, `chat`.

## العميل
| الطريقة | المسار | الإجراء | الانتقال |
|---|---|---|---|
| GET/POST/PATCH/DELETE | `/addresses` | العناوين؛ الحذف ناعم ويعيد `deleted_id` و`default_address` البديل أو `null` | — |
| POST | `/media` | رفع وسيط مؤقت ← `media_id` | — |
| POST | `/orders` | `publishRequest` | T-01 |
| GET | `/orders?scope=current|past` | طلباتي | — |
| GET | `/orders/{id}` | التفاصيل (+ الملخص الزمني) | — |
| PATCH | `/orders/{id}` | `updateRequest` (BR-021) | — |
| POST | `/orders/{id}/republish` | إعادة نشر | — |
| GET | `/orders/{id}/offers?sort=rating|price|eta` | العروض — قائمة فارغة في وضع الموظفين | — |
| GET | `/providers/{id}?order_id={orderId}` | ملف الفني والتقييمات داخل سياق طلب مملوك؛ بلا هاتف أو بيانات خاصة | — |
| POST | `/orders/{id}/offers/{offerId}/accept` | `acceptOffer` {payment_method} — `FEATURE_DISABLED` في وضع الموظفين | T-02 |
| POST | `/orders/{id}/cancel` | {reason_code, note} | T-04/08/09 |
| PATCH | `/orders/{id}/payment-method` | BR-050 | — |
| GET | `/orders/{id}/tracking` | آخر موقع + وقت الوصول | — |
| POST | `/orders/{id}/share-links` · DELETE `/share-links/{id}` | DEC-037 | — |
| GET | `/orders/{id}/proposals` | المقترحات، ومنها `projected_total` للإجمالي المتوقع عند الموافقة | — |
| POST | `/orders/{id}/proposals/{pid}/decide` | `{approve, expected_version}` | T-14/15/28 |
| POST | `/orders/{id}/payments` | إنشاء دفع فوري ← بيانات SDK/الكود (قناة `FAWRY` مفعّلة) | — |
| POST | `/orders/{id}/instapay-transfers` | `{transfer_reference, receipt_media_id?, expected_version}` ← محاولة `PENDING_VERIFICATION` (BR-057) | — (T-20 عند تأكيد الإدارة) |
| POST | `/orders/{id}/confirm-completion` | | T-21 |
| POST | `/orders/{id}/review` | BR-090 | — |
| GET/POST | `/orders/{id}/disputes` | متابعة الملفات / فتح مشكلة `{reason_code, description, media_ids[]}` | T-22 أو نزاع بعد الإغلاق |
| POST | `/providers/{id}/reports` | بلاغ `{order_id, reason_code, description?}`؛ الفني يجب أن يكون داخل سياق طلب مملوك | — |

## الفني
| الطريقة | المسار | الإجراء | الانتقال |
|---|---|---|---|
| POST | `/provider/application` · PATCH للتعديل وإعادة التقديم | التسجيل | — |
| GET/PATCH | `/provider/profile` | الملف (الحقول المسموحة BR-130) | — |
| POST/DELETE | `/provider/portfolio` | | — |
| PUT | `/provider/availability` | {available_now} | — |
| GET | `/provider/requests` | الطلبات المتاحة (BR-022) — فارغة في وضع الموظفين | — |
| GET | `/provider/requests/{id}` | تفاصيل محدودة (BR-024) | — |
| POST | `/provider/requests/{id}/offers` | `submitOffer` — `FEATURE_DISABLED` في وضع الموظفين | O-01 |
| POST | `/provider/offers/{id}/withdraw` | | O-02 |
| GET | `/provider/offers` · `/provider/orders?scope=` | | — |
| POST | `/provider/orders/{id}/start-trip` | `{lat,lng}` لحساب ETA الأول وتسجيل نقطة البداية | T-05 |
| POST | `/provider/orders/{id}/location` | {lat,lng} كل 30 ثانية | — |
| POST | `/provider/orders/{id}/back-out` | {reason_code} | T-06/07 |
| POST | `/provider/orders/{id}/arrived` | {lat,lng} | T-10 |
| POST | `/provider/orders/{id}/start-work` | | T-11 |
| POST | `/provider/orders/{id}/proposals` | {type, amount, reason, photo} | T-12 أو بدون انتقال |
| POST | `/provider/orders/{id}/proposals/{pid}/withdraw` | | — |
| POST | `/provider/orders/{id}/complete-inspection-only` | تُغلق الطلب مباشرة إذا كانت المعاينة مجانية | T-13 أو T-28 |
| POST | `/provider/orders/{id}/complete` | | T-17 |
| POST | `/provider/orders/{id}/unable` · `/customer-no-show` | | T-16/18 |
| POST | `/provider/orders/{id}/cash-received` | {amount = final_amount} | T-19 |
| POST | `/provider/orders/{id}/customer-rating` | | — |
| GET | `/provider/statement` | كشف المستحقات BR-062 | — |

## مشترك
| الطريقة | المسار | ملاحظة |
|---|---|---|
| GET/POST | `/conversations` · `/conversations/{id}/messages` | BR-100..102 |
| GET | `/support/faqs` | قائمة الأسئلة الشائعة من الخادم؛ التطبيقات لا تثبتها داخلها |
| GET | `/notifications` | صفحة الإشعارات مع `deep_link` و`read_at` |
| POST | `/notifications/read` | `{notification_ids}`؛ يحدّث إشعارات الحساب الحالي فقط |
| POST | `/support/messages` | بريد للدعم |

## الويب والتكاملات
- `GET /v/{token}` — SCR-W01 (HTML، بلا مصادقة، حد معدل).
- `POST /webhooks/fawry` — تحقق التوقيع، البحث بـ `merchant_ref`، تحديث الدفعة، T-20. يعيد 200 دائمًا بعد التسجيل؛ التكرار لا أثر له.

## أمثلة تفصيلية
### `PATCH /orders/{id}/payment-method`
- **الطلب**: `{ "payment_method": "CASH" | "ELECTRONIC", "expected_version": 3 }`.
- **الشروط**: العميل صاحب الطلب، والحالة `AWAITING_PAYMENT`، ولم يُسجل الدفع (BR-050).
- **النتيجة**: الطلب المحدث؛ تغيير الطريقة يلغي المحاولة الإلكترونية المعلقة ويسجل EVT-013.

### `POST /orders/{id}/payments`
- **الطلب**: `{ "channel": "CARD" | "WALLET" | "KIOSK", "expected_version": 3 }`.
- **النتيجة**: `{id,status,amount,channel,reference_number,checkout_url,expires_at}` فقط؛ لا توقيع ولا مفتاح. `reference_number` لكود المنافذ و`checkout_url` للبطاقة/المحفظة.
- **الشروط**: العميل صاحب الطلب، `AWAITING_PAYMENT`، والطريقة `ELECTRONIC`، ووجود `pay_electronic` (BR-051). محاولة معلقة واحدة؛ الجديدة تلغي السابقة.
- **staging فقط**: يقبل `simulation = PENDING | SUCCESS | FAILURE | EXPIRY` عندما يكون `PAYMENT_GATEWAY_DRIVER=staging`; الحقل محظور في الإنتاج.
- **النجاح**: لا يُقبل من استجابة الإنشاء وحدها؛ يمر عبر webhook موقّع، ويطابق المبلغ، ثم T-20. الفشل والانتهاء يبقيان الطلب `AWAITING_PAYMENT`.

### `POST /orders/{id}/instapay-transfers` (DEC-050)
- متاح فقط والطلب `AWAITING_PAYMENT` وقناة `INSTAPAY_MANUAL` مفعّلة (وإلا `FEATURE_DISABLED`)، ولا توجد محاولة معلّقة أخرى (`PAYMENT_PENDING`).
- `transfer_reference` إلزامي (4–64 حرفًا/رقمًا)، و`receipt_media_id` اختياري لوسيط صورة مرفوع من العميل نفسه.
- الاستجابة: `payment` بحالة `PENDING_VERIFICATION`، والطلب يبقى `AWAITING_PAYMENT` و`display_status = order.status.customer.AWAITING_TRANSFER_VERIFICATION`.
- `GET /config` يعيد `payment_gateways` (القنوات المفعّلة `[{code, label}]`) و`instapay` (`address`، `display_name`، `link`، ويظهر فقط إذا كانت القناة مفعّلة). كل طلب فيه `payment_reference` = رقم الطلب الذي يُكتب في ملاحظة التحويل.

### `POST /orders/{id}/offers/{offerId}/accept`
- **المنفذ**: العميل صاحب الطلب.
- **الطلب**: `{ "payment_method": "CASH" | "ELECTRONIC", "expected_version": 3 }`
- **الشروط**: BR-034.
- **النتيجة**: `200` + الطلب بحالة `CONFIRMED` (مع بيانات الفني وهاتفه). الآثار: BR-035، EVT-010، NTF-05/06.
- **الأخطاء**: `INVALID_TRANSITION` (ليس OPEN أو بعد المهلة)، `BUSINESS_RULE_VIOLATION` (`BR-034`: الفني مشغول أو تعارض فترة)، `CONFLICT`.
- **التكرار**: نفس `Idempotency-Key` ← نفس الاستجابة.

### `POST /provider/requests/{id}/offers`
- **الطلب**: `{ "price": "350.00", "eta_minutes": 25, "inspection_fee_deductible": null, "includes_text": "المصنعية فقط", "note": "..." }`
- **الشروط**: BR-022، BR-030..032، BR-063.
- **النتيجة**: `201` + العرض `SUBMITTED` + `net_amount` بعد العمولة. EVT-006، NTF-04.
- **الأخطاء**: `BUSINESS_RULE_VIOLATION` (`BR-030` عرض قائم/تجاوز إعادة التقديم، `BR-031` النافذة مغلقة أو 10 عروض، `BR-063` ممنوع بسبب المستحقات).

### `POST /provider/orders/{id}/complete-inspection-only`
- **الشروط**: الحالة `ARRIVED`، `pricing_mode = INSPECTION`، ولم يُقدَّم عرض تنفيذ.
- **النتيجة**: رسوم المعاينة > 0 ← `AWAITING_PAYMENT` بالرسوم (T-13، EVT-035). رسوم = 0 ← `CLOSED` مباشرة بمبالغ صفرية وحالة دفع `WAIVED` (T-28، EVT-036، BR-056) ويصل NTF-16 للطرفين.

### `POST /provider/orders/{id}/complete`
- **الشروط**: الحالة `IN_PROGRESS`، BR-045.
- **النتيجة**: `AWAITING_PAYMENT` + المبالغ المحسوبة (BR-044). EVT-050، NTF-13.

### `POST /provider/orders/{id}/cash-received`
- **الطلب**: `{ "amount": "650.00" }` يجب أن يساوي `final_amount`.
- **النتيجة**: Payment `CASH/SUCCEEDED` ← `AWAITING_CONFIRMATION`. EVT-061، NTF-15.

### الوسائط
- `POST /media` يستقبل `multipart/form-data` في الحقل `file` ويعيد `media.id`. يتحقق الخادم من MIME الفعلي والحجم والمدة، ويعيد ترميز الصور لإزالة EXIF، ويحفظ الملفات على التخزين الخاص باسم عشوائي.
- الصور: JPEG/PNG/WebP، حتى 10MB. الفيديو: MP4/MOV/WebM، حتى 60 ثانية و50MB. الصوت: AAC داخل `.m4a` فقط، قناة واحدة، 64kbps، حتى 120 ثانية و5MB؛ يتحقق الخادم بـ `ffprobe` ويرفض غير ذلك.
- الرفع المؤقت ينتهي بعد 24 ساعة ويُحذف كل ساعة إن لم يُربط بطلب أو رسالة. `GET /media/{id}` للأطراف المخوّلة فقط، و`DELETE /media/{id}` لصاحب رفع مؤقت فقط.
- عند نشر الطلب: حتى 5 صور + فيديو واحد + تسجيل واحد، وكل المعرفات يجب أن تكون صالحة ومملوكة لنفس العميل.

### المحادثة
- `POST /conversations` في `OPEN`: العميل فقط، ومع فني لديه عرض `SUBMITTED` على الطلب. في وضع الموظفين تُنشأ المحادثة تلقائيًا عند التعيين، وعند قبول العرض تُفتح محادثة الفني المختار وتصبح البقية للقراءة فقط.
- `GET /conversations` يعيد محادثات الحساب مرتبة بآخر نشاط تنازليًا ثم `id`، ومع كل عنصر الفني والطلب وآخر رسالة. `GET /conversations/{id}/messages` يعيد أحدث 50 رسالة في الصفحة بترتيب زمني تصاعدي للعرض.
- `POST /conversations/{id}/messages`: نص حتى 1000 حرف، أو حتى 5 صور بعد `CONFIRMED`. قبل الاختيار تُحجب وسائل التواصل ويحفظ الأصل مشفرًا. الحد 30 رسالة/دقيقة.

### قوائم الطلبات والتفاصيل السابقة
- `GET /orders?scope=current|past&page={page}` يعيد 20 طلبًا مرتبة بـ `created_at DESC, id DESC`؛ `past` محصور في `CLOSED/CANCELLED/EXPIRED`.
- كل `OrderResource` يعيد `termination.reason_code` و`termination.reason_label` المترجم من الخادم و`termination.note` والتواريخ النهائية. تستخدم C26 النص المعاد ولا تحوّل كود السبب داخل التطبيق.

### التتبع والمشاركة
- `GET /orders/{id}/tracking` للعميل صاحب الطلب فقط: `last_location` و`eta_minutes` و`eta_approximate` و`eta_calculated_at`. الموقع لا يُقبل إلا من الفني المسند في `ON_THE_WAY` وبفاصل `CFG-080`.
- يحسب الخادم ETA عبر Google Routes API بوضع `DRIVE` و`TRAFFIC_AWARE` عند بدء التحرك، ثم كل 120 ثانية أو إذا تحرك الفني أكثر من 300 متر منذ آخر حساب (CFG-081). عند فشل Google يستخدم المسافة المستقيمة بسرعة 20 كم/س ويعيد `eta_approximate=true`؛ تعرض الواجهة «تقريبي». المفتاح من بيئة الخادم، ولا ترسل التطبيقات أي طلب إلى Google لحساب الوقت.
- `POST /orders/{id}/share-links` للعميل أثناء الزيارة يعيد رابطًا برمز عشوائي 256-bit؛ لا تاريخ انتهاء زمني مخمّن، ويصبح غير صالح عند الإلغاء اليدوي أو وصول الطلب لحالة نهائية. `DELETE /share-links/{id}` يلغي الرابط.
- صفحة `GET /v/{token}` (SCR-W01) منفذة وفق `docs/screens/SCR-W01.md`: RTL/Cairo، تحديث كل 30 ثانية، `noindex` و`no-store`، ولا تعرض العنوان التفصيلي أو الخريطة أو الموقع أو الهاتف أو السعر أو اسم العميل. الرابط غير الصالح يعيد HTTP 410.

### التقييم
- `POST /orders/{id}/review`: العميل صاحب الطلب، بعد `CLOSED` وداخل `CFG-070`، مرة واحدة؛ `quality` و`punctuality` و`conduct` من 1 إلى 5، وتعليق اختياري حتى 500 حرف.
- `POST /provider/orders/{id}/customer-rating`: الفني المسند، بعد `CLOSED` وداخل نفس النافذة، مرة واحدة؛ `stars` من 1 إلى 5.
- التقييمات غير قابلة للتعديل، والمخفي إداريًا مستبعد من المتوسطات المعاد حسابها.

## حدود المعدل
الدخول والتسجيل: 5/دقيقة لكل IP. نشر الطلبات: 10/ساعة للمستخدم. العروض: 60/ساعة للفني. الرسائل: 30/دقيقة. الموقع: 4/دقيقة للطلب. رابط المشاركة: 60/دقيقة لكل IP.

## ما نُفِّذ فعليًا (حتى الجزء المنجز من الدفعة 4ج)
| المجموعة | المسارات |
|---|---|
| عامة | `GET /config`، `/cities`، `/cities/{city}/areas`، `/catalog`، `/slots` |
| الهوية | `POST /auth/register`، `/auth/login`، `/auth/logout`، `/auth/email/resend`، `/auth/password/forgot`، `/auth/password/reset`، `GET /auth/email/verify/{id}/{hash}`، `GET /me` |
| العناوين | `GET/POST /addresses`، `PATCH/DELETE /addresses/{address}` |
| العميل | `GET /orders`، `POST /orders`، `GET /orders/{order}`، `/offers`، `/proposals`، `POST /orders/{order}/cancel`، `/offers/{offer}/accept`، `/proposals/{proposal}/decide`، `/confirm-completion` |
| الدفع | `PATCH /orders/{order}/payment-method`، `POST /orders/{order}/payments`، و`POST /webhooks/fawry` عبر `PaymentGateway` ومحاكي staging الموقّع |
| الفني | `GET /provider/requests`، `/provider/orders`، `POST /provider/requests/{order}/offers`، و`/provider/orders/{order}/{start-trip\|location\|arrived\|start-work\|proposals\|complete-inspection-only\|complete\|cash-received\|back-out\|unable\|customer-no-show}` |
| الوسائط | `POST /media`، `GET/DELETE /media/{media}`، ربط الرفع بالطلب أو صور الرسالة، وتنظيف الرفع المنتهي |
| المحادثة | `GET/POST /conversations`، `GET/POST /conversations/{conversation}/messages`، والحجب/القراءة فقط حسب دورة الطلب |
| التتبع والمشاركة | `GET /orders/{order}/tracking`، `POST /orders/{order}/share-links`، `DELETE /share-links/{shareLink}` |
| التقييم | `POST /orders/{order}/review`، `POST /provider/orders/{order}/customer-rating`، وتحديث المتوسطات |
| الدعم | `GET/POST /orders/{order}/disputes`، `POST /providers/{provider}/reports`، وأسباب C27/C28 المدارة من لوحة التشغيل ضمن `GET /config` |
| حقول العرض | كل `OrderResource`: `available_actions` بالأسماء الثابتة، وكل مفاتيح `deadlines`، و`display_status`، و`stepper` |

لم يُنفَّذ بعد: تنفيذ بوابة فوري الإنتاجية (يلزم OD-10؛ العقد والمحاكي وwebhook منتهية)، وواجهات الإشعارات. نُفذت endpoints التفعيل والاستعادة؛ لكن وجهة رابط إعادة كلمة المرور وصفحة `SCR-W02` لم تُحسم ضمن 3أ، لذلك لا تُعد رحلة الرابط مكتملة إنتاجيًا. يظل اختيار مزود البريد الإنتاجي OD-03 قرار نشر.

## لوحة الإدارة
إجراءات الإدارة تُنفذ داخل Filament عبر نفس Actions (لا API عام للإدارة). يشمل ذلك `assignProvider` (T-27) و`reassignProvider` (T-29) في وضع الموظفين، وتبديل CFG-090/091 من الإعدادات (المدير العام فقط — 23).

## إضافات DEC-048..053
| الطريقة | المسار | المحتوى |
|---|---|---|
| GET | `/pages/{slug}` | `terms` · `privacy` · `cancellation-policy` · `faq` — النسخة السارية المنشورة فقط `{slug, title, version, body_html, effective_at, web_url}`؛ غير منشورة ← 404 `PAGE_NOT_PUBLISHED` (DEC-051) |
| GET | `/terms/current` | الشروط السارية + `cancellation_policy`؛ `version` هو `terms_version` (BR-018) |
| GET | `/me` | يضيف `terms: {current_version, accepted_version, acceptance_required}`؛ C05 يعرض الموافقة فقط عند `acceptance_required` |
| POST | `/orders` | `terms_accepted` مطلوب فقط عندما `acceptance_required` (BR-018) |
| GET | `/catalog?city_id=` | كل فئة تضيف `icon_url` (OD-08) |
| GET | `/support/faqs` | من صفحة `faq` المنشورة (كل `h3` سؤال)، وإلا القائمة الافتراضية |
| GET | `/config` | `option_lists.payment_gateways` (القنوات المفعّلة) و`instapay` (DEC-050) |
| POST | `/orders/{id}/instapay-transfers` | BR-057 |

صفحات الويب (ليست API): `/terms`، `/privacy`، `/cancellation-policy`، `/faq` (SCR-W03)، و`/email/verify/{id}/{hash}` و`/password/reset/{token}` (SCR-W02).
