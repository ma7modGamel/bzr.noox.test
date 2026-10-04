# تقرير الدفعة 9أ — المرحلة 1 (الدخول) + تجهيز المراقبة والإصدار

## بوابة الاعتماد

- ✅ **اعتمدها المالك 2026-10-04.** المرحلة 2 (رئيسية العميل والطلبات) بدأت.
- القرار: DEC-061 في `docs/34-DECISION-LOG.md` (بند 4: التحويل على مراحل ببوابة لكل مرحلة).
- معها في نفس التسليم البند 1 من مراجعة التطبيقات: Crashlytics في iOS، وR8 لنسخة إصدار أندرويد.

## 1) المرحلة 1 من 9أ — موديلات الدخول في أندرويد

| الـ endpoint | دالة Retrofit | الموديل |
|---|---|---|
| `POST auth/login` | `AuthService.login` | `LoginRequest` → `AuthResponse(user: AuthUser, token)` |
| `POST auth/register` | `AuthService.register` | `RegisterRequest` → `AuthResponse` (+ `email_verification_required`) |
| `GET me` | `AuthService.me` | `AccountResponse(user: AccountUser)` (مع `terms` و`available_actions`) |
| `POST auth/email/resend` | `AuthService.resendVerification` | 204 بلا جسم |
| `POST auth/password/forgot` | `AuthService.forgotPassword` | `ForgotPasswordRequest` → 204 |
| `POST auth/logout` | `AuthService.logout` | 204 |
| أي خطأ | — | `ApiError(code, message, rule, fields)` |

- `noox.bzr.auth` لم يعد فيه أي `JSONObject`. الكلاس اتغير اسمه من `UrlConnectionAuthApi` إلى `RetrofitAuthApi`.
- `BremoHttp.typed(...)` يبني Retrofit مكتوبًا على نفس عميل OkHttp، فالتسجيل في Logcat (`BremoHttp`/`BremoNet`) وتصنيف أعطال الشبكة كما هما. المراحل القادمة تستخدم `bremoCall` و`ApiException` نفسيهما.
- `AuthViewModel` أصبح يعمل بـ `viewModelScope` بدل `Thread {}` و`Handler`، فالطلبات تُلغى تلقائيًا مع إغلاق الشاشة.
- **سلوك الشاشات لم يتغير:** الخطأ يصل لـ AuthLogic بالكود نفسه كما كان (`HTTP_<status>` لو الخادم لم يرسل كودًا). ولم أستخدم `fields` لتحديد الحقل الخاطئ، لأن iOS لا يستخدمها، والتماثل بين المنصتين أولى. هذا تحسين ممكن لاحقًا للمنصتين معًا.

### ردود حقيقية من الخادم

`openapi.json` لا يصف أجسام الردود، فالمطابقة تمت على ردود الخادم الفعلية:
- اختبار جديد في الخادم `tests/Feature/Api/MobileAuthResponsesTest.php`. يتحقق في كل تشغيل من الحقول التي يقرؤها التطبيقان، ومع `RECORD_API_RESPONSES=1` يكتب 9 ردود (نجاح وأخطاء 401/403/409/422) في `androidapp/app/src/test/resources/api/auth/`.
- القيم في الردود المسجلة ثابتة (المعرّف والتوكن والأسماء)، فإعادة التسجيل لا تُظهر فرقًا إلا لو تغيّر العقد فعلًا.

## 2) Crashlytics في iOS

- `iosapp/project.yml`: أُضيف `FirebaseCrashlytics`، و`DEBUG_INFORMATION_FORMAT: dwarf-with-dsym`، وخطوة رفع dSYM لنسخة Release.
- يبدأ مع `FirebaseApp.configure()` الموجود. مثل الإشعارات، لا يعمل قبل إضافة `GoogleService-Info.plist` (DEP-PUSH-02).
- ⚠️ **لم يُبنَ بعد:** لا يوجد Xcode على Linux. يُتحقق منه في الدفعة 12 «تحقق iOS» (`ios-final-validation.yml`) أو على أي Mac.

## 3) R8 لنسخة إصدار أندرويد

- `release`: `isMinifyEnabled` و`isShrinkResources`، وملف `app/proguard-rules.pro` (أرقام الأسطر لـ Crashlytics).
- الحجم: **3.3MB** للإصدار مقابل 10.4MB لنسخة التطوير.
- R8 حافظ على serializers الموديلات وواجهة Retrofit (تم التأكد من `mapping.txt`).
- النسخة اتوقّعت بمفتاح التطوير للتجربة فقط، واتسطّبت على emulator `Pixel_9a`، وفتحت شاشة الدخول بلا أعطال.

## التحقق

- اختبارات أندرويد: **app 28/28، core/design 5/5**. الجديد منها 13 اختبارًا: `AuthModelsDecodeTest` (5)، و`RetrofitAuthApiTest` (5، بالردود المسجلة على MockWebServer)، و`AuthViewModelTest` (3).
- `verifyPaparazziDebug`: كل اللقطات مطابقة. واجهات الدخول لم تتغير، فلا لقطات جديدة.
- `lintDebug` و`assembleRelease` ناجحان.
- الخادم: `MobileAuthResponsesTest` ناجح (44 تحققًا)، وPint نظيف.

## المتبقي وقيود

- 290 استخدامًا لـ `JSONObject` باقية في أندرويد للمراحل 2–4.
- **لم أجرب دخولًا حقيقيًا من نسخة الإصدار على الخادم:** نسخة الإصدار تمنع HTTP، والخادم المحلي على الويب متوقف لأن php-fpm أقدم من PHP 8.4 المطلوب. ولم أرسل طلبات لخادم الإنتاج. المطلوب: تجربة دخول من نسخة الإصدار على staging.
- **عوائق تخص المالك:** `GoogleService-Info.plist` لـ iOS (DEP-PUSH-02)، ومفتاح توقيع إصدار أندرويد (الدفعة 9).
- التغييرات غير مُودَعة (commit): أندرويد في الـ worktree `/home/tro/bzr-android-xml`، والخادم وiOS والتقرير في الشجرة الرئيسية.
