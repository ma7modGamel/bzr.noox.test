# تقرير الدفعة 9أ — المرحلة 2 (رئيسية العميل والطلبات) + شريط التنقل + تجهيز iOS للمتجر

## بوابة الاعتماد

- ✅ **اعتمدها المالك نهائيًا 2026-10-05**، ودُمجت في `main` مع تطبيق أندرويد (commit `ce0848e`). التعارض مع Codex انحسم: تصميمه اعتُمد أساسًا وبُني عليه DEC-063.
- ⚠️ **قبل الاعتماد لازم يتحسم تعارض:** فيه وكيل Codex شغال بالتوازي على الشجرة الرئيسية، وبيعدّل نظام التصميم (`design/tokens.json`، الألوان، الحركة). استبدل شريط التنقل المتحرك في iOS بتصميم capsule ثابت، وغيّر `motion.navBarMs` من 300 إلى 180. لم أرجّع أي تعديل له. التفاصيل في آخر التقرير.

## 1) المرحلة 2 من 9أ — أندرويد

كل endpoints الرئيسية والطلبات أصبحت دوال Retrofit بموديلات مكتوبة (`customer/CustomerOrderModels.kt`):

| الـ endpoint | الموديل |
|---|---|
| `GET config` | `AppConfig` |
| `GET catalog` (+`city_id`) | `DataList<CatalogCategory>` |
| `GET addresses` | `DataList<CustomerAddress>` |
| `GET me` | `AccountResponse` (من المرحلة 1) |
| `GET orders?scope&page` | `OrdersPage` (مع `meta`) |
| `GET orders/{id}` | `DataItem<CustomerOrder>` |
| `GET orders/{id}/tracking` | `TrackingResponse` |
| كل إجراء يعيد الطلب: نشر، تعديل، قبول عرض، إلغاء، قرار مقترح، طريقة الدفع، تأكيد الإنهاء، إعادة النشر | `DataItem<CustomerOrder>` |

- `CustomerOrder` يغطي `OrderResource` كله: المبالغ والإنهاء و`latest_payment` والفني والـ stepper والمواعيد.
- `currentOrderPayload` في `CustomerViewModel` أصبح `CustomerOrder` بدل `JSONObject`. كل الشاشات التي تبني حالتها من الطلب (C01 وC09 وC21 وC22 وC23 وC26 وC30/C31 وC36 وC06/C08) تقرأ الآن الموديل.
- `X-App-Mode: CUSTOMER` يُرسل كما كان، عبر `BremoHttp.typed(..., appMode)`.
- خطأ الخادم يصل للشاشة برسالته كما كان (`CustomerApiException.error`).
- الطلبات ما زالت متزامنة على threads، لأن `CustomerViewModel` لسه على `Thread`. الانتقال لـ coroutines يكون مع تقسيمه (بند 6 من المراجعة).
- **جسم طلب النشر والتعديل** (C03–C05) ما زال يُبنى كما هو، ويُرسل كـ JSON. نوعه يتحدد في المرحلة 3.

### أخطاء ظاهرة للمستخدم اتصلحت كأثر جانبي

`optString` في Android كان يرجّع النص `"null"` لما القيمة في الـ JSON تكون `null`. أمثلة:
- **C26:** سبب الإلغاء بدون ملاحظة كان يظهر `null`.
- **C26:** تاريخ الطلب الفوري (اللي مالوش موعد) كان يظهر `null`.
- **C21/C23:** المبالغ قبل التسعير كانت ممكن تظهر «null جنيه»، وطريقة الدفع الفارغة كانت تتبعت `"null"`.

### خطأ في المرحلة 1 اكتشفته هنا وصلّحته

`rating_avg` نوعه `decimal:2` على الخادم، فبيوصل كنص `"4.50"`، لكن موديل الدخول كان مكتوب `Double?`. أي عميل عنده تقييم كان دخوله هيفشل. الردود المسجلة وقتها كان التقييم فيها `null`، فما ظهرش.
- **الإصلاح:** `ratingAvg: String?`، وتسجيل الدخول الآن لعميل مقيَّم `4.50`.
- **القاعدة من دلوقتي:** أي حقل رقمي يتسجل بقيمة حقيقية، مش `null`.
- iOS سليم: يقرأ `rating_avg` كـ `String?` من البداية.

### ردود حقيقية من الخادم

`tests/Feature/Api/MobileCustomerOrderResponsesTest.php` يمشي رحلة كاملة: نشر ← تعيين ← في الطريق مع موقع ← وصول ← مقترح ← موافقة ← إنهاء ← استلام نقدي ← تأكيد، ثم طلب ملغي. يسجّل 13 ردًا في `androidapp/app/src/test/resources/api/customer/`، والمعرّفات فيها ثابتة.

### تجربة حقيقية

على emulator مع `php artisan serve` محلي، ببيانات demo:
- الرئيسية: config ← orders ← addresses ← catalog ← me، كلها 200.
- «طلباتي» (الحالية والسابقة).
- فتح طلب مجدول على C36.

ولا خطأ قراءة واحد في Logcat.

## 2) شريط التنقل المتحرك (DEC-062)

- **أندرويد ✅:** `BottomNavView` مبني بـ XML Views بنفس حركة exyte: الانبعاج، والكرة الطائرة، والقطرة. الشريط ثابت أسفل C01 وC25 وC18 وC02 في `MainActivity`، والحركة اتصورت فريم بفريم على emulator.
  - اللقطات: اتغيرت 3 بس (C01 مرتين + معرض المكونات)، وباقي اللقطات متطابقة.
  - `scripts/check-no-compose.sh` نجح.
- **iOS ⚠️:** كتبته في `BottomNav` واتنقل لـ `CustomerJourneyRoot`، لكن **Codex استبدله بعدها** بتصميم capsule ثابت. لم يُستعد.
- القرار مسجل في `docs/34` (DEC-062)، و`docs/43` اتحدّث.

## 3) تجهيز iOS للمتجر (كود فقط)

- **iOS 17:** `project.yml` والحزمتين، و`onChange` بالصيغة الجديدة.
- **`BzrTheme` يلف التطبيق كله.** كان الخط Cairo لا يُسجَّل لو التطبيق فتح والمستخدم داخل بالفعل.
- **ملفات المتجر:** `PrivacyInfo.xcprivacy`، ونصوص الأذونات بالعربي (من `design/strings.ar.json` عبر المولّد)، و`ITSAppUsesNonExemptEncryption = NO`.
- **إعدادات الإصدار:**
  - iPhone فقط، والوضع الرأسي.
  - الإصدار `1.0.0 (1)`.
  - `aps-environment` = development في Debug وproduction في Release.
  - التوقيع بـ `BZR_TEAM_ID`.
- **CI:** على `latest-stable` Xcode.
- **أيقونة مؤقتة** (بيت أبيض على `primary600`) في iOS وAndroid (adaptive). ⛔ **عائق:** لوجو بريمو النهائي غير موجود في المشروع.
- **خطوات الرفع من الـ Mac** في `iosapp/README.md`.
- ⚠️ **لم يُبنَ على Xcode:** Core اتبنى واتختبر على Linux (15/15)، وswift-format وSwiftLint نجحوا.

## التحقق

- **أندرويد:**
  - app **40/40** وcore/design **5/5**.
  - اللقطات متطابقة، و`lintDebug` نجح.
  - `assembleRelease` نجح (R8، 3.3MB).
  - check-no-compose نجح.
- **الخادم:** اختبارا التسجيل ناجحان، و`api:spec --check` مطابق.
- **فشل الحزمة الكاملة (235/238): الثلاثة كلهم في `DesignTokensTest`، وسببهم تعديلات Codex الجارية في نظام التصميم:**
  - الملفات المولّدة غير محدثة.
  - parity بتاع `AppTopBar` في iOS.
  - قيمة توكن متوقعة 250 ولقت 220.
- **Pint:** فشل في 8 ملفات لم أعدّلها.

## المتبقي

- **المراحل الباقية:** المرحلة 3 (بقية مسار العميل) ثم 4 (الفني). باقي 206 استخدامًا لـ `JSONObject` في أندرويد.
- **محتاج قرار منك:**
  - مين يملك نظام التصميم وiOS دلوقتي؟
  - هل حركة الشريط في iOS ترجع (DEC-062) ولا تصميم Codex يحل محلها؟ ولو حلّ محلها، أندرويد لازم يتغير معاه علشان التماثل.
- **عوائق تخصك:** لوجو بريمو، و`GoogleService-Info.plist`، وTeam ID.
- لا شيء مُودَع (commit) بعد.
