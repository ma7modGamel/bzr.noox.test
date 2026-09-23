# 31 — واجهات API

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
| `EMAIL_NOT_VERIFIED` | 403 | BR-001 |
| `ACCOUNT_BLOCKED` | 403 | BR-003 |
| `NOT_FOUND` | 404 | يشمل المورد غير المملوك |
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
| POST | `/auth/password/forgot` · `/auth/password/reset` | |
| GET/PATCH | `/me` | الاسم، الهاتف، الصورة |
| POST | `/me/password` | |
| DELETE | `/me` | حذف الحساب (مرفوض مع طلب نشط) |
| POST/DELETE | `/me/devices` | رمز FCM |

## المراجع
`GET /cities`, `GET /cities/{id}/areas`, `GET /catalog?city_id=`, `GET /slots?city_id=&date=`, `GET /terms/current`.

`GET /config` — بلا مصادقة، يُقرأ عند إقلاع التطبيق وعند العودة للمقدمة:
```json
{ "offers_enabled": false, "inspection_fee_enabled": false, "service_hours": {"from":"08:00","to":"22:00"}, "min_offer_amount": "50.00", "terms_version": 3 }
```
التطبيق يخفي شاشات العروض وحقل طريقة التسعير والميزانية عندما `offers_enabled = false`، ويخفي رسوم المعاينة عندما `inspection_fee_enabled = false` (39). الإخفاء للعرض فقط؛ الحسم على الخادم.

## العميل
| الطريقة | المسار | الإجراء | الانتقال |
|---|---|---|---|
| GET/POST/PATCH/DELETE | `/addresses` | العناوين | — |
| POST | `/media` | رفع وسيط مؤقت ← `media_id` | — |
| POST | `/orders` | `publishRequest` | T-01 |
| GET | `/orders?scope=current|past` | طلباتي | — |
| GET | `/orders/{id}` | التفاصيل (+ الملخص الزمني) | — |
| PATCH | `/orders/{id}` | `updateRequest` (BR-021) | — |
| POST | `/orders/{id}/republish` | إعادة نشر | — |
| GET | `/orders/{id}/offers?sort=rating|price|eta` | العروض — قائمة فارغة في وضع الموظفين | — |
| GET | `/providers/{id}` | ملف الفني العام + التقييمات (ترقيم) | — |
| POST | `/orders/{id}/offers/{offerId}/accept` | `acceptOffer` {payment_method} — `FEATURE_DISABLED` في وضع الموظفين | T-02 |
| POST | `/orders/{id}/cancel` | {reason_code, note} | T-04/08/09 |
| PATCH | `/orders/{id}/payment-method` | BR-050 | — |
| GET | `/orders/{id}/tracking` | آخر موقع + وقت الوصول | — |
| POST | `/orders/{id}/share-links` · DELETE `/share-links/{id}` | DEC-037 | — |
| POST | `/orders/{id}/proposals/{pid}/approve` · `/reject` | | T-14/15 |
| POST | `/orders/{id}/payments` | إنشاء دفع فوري ← بيانات SDK/الكود | — |
| POST | `/orders/{id}/confirm-completion` | | T-21 |
| POST | `/orders/{id}/review` | BR-090 | — |
| POST | `/providers/{id}/reports` | بلاغ | — |

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
| POST | `/provider/orders/{id}/start-trip` | | T-05 |
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
| POST | `/orders/{id}/disputes` | T-22 أو نزاع بعد الإغلاق |
| POST | `/disputes/{id}/attachments` | |
| GET | `/notifications` · POST `/notifications/read` | |
| POST | `/support/messages` | بريد للدعم |

## الويب والتكاملات
- `GET /v/{token}` — SCR-W01 (HTML، بلا مصادقة، حد معدل).
- `POST /webhooks/fawry` — تحقق التوقيع، البحث بـ `merchant_ref`، تحديث الدفعة، T-20. يعيد 200 دائمًا بعد التسجيل؛ التكرار لا أثر له.

## أمثلة تفصيلية
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

## حدود المعدل
الدخول والتسجيل: 5/دقيقة لكل IP. نشر الطلبات: 10/ساعة للمستخدم. العروض: 60/ساعة للفني. الرسائل: 30/دقيقة. الموقع: 4/دقيقة للطلب. رابط المشاركة: 60/دقيقة لكل IP.

## لوحة الإدارة
إجراءات الإدارة تُنفذ داخل Filament عبر نفس Actions (لا API عام للإدارة). يشمل ذلك `assignProvider` (T-27) و`reassignProvider` (T-29) في وضع الموظفين، وتبديل CFG-090/091 من الإعدادات (المدير العام فقط — 23).
