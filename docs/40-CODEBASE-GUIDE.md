# 40 — دليل الكود (ربط الوثائق بالملفات)

هذه الوثيقة تصف **أين** تعيش كل قاعدة في الكود. الوثائق من 00 إلى 39 تبقى المصدر الوحيد لسلوك المنتج؛ هذا الملف خريطة تنفيذ، لا مكان لقرار جديد.

## المكدس
| البند | الاختيار | المرجع |
|---|---|---|
| الإطار | Laravel 13 · PHP 8.4 | DEC-036 |
| اللوحة | Filament 5 (أحدث إصدار) | DEC-036 |
| قاعدة البيانات | MySQL 8 / MariaDB — أعمدة مولّدة للقيود الشرطية | 29 |
| الطوابير والتخزين المؤقت | Redis | 30 |
| الواجهة | Tailwind 4 عبر ثيم فيلامنت، عربية RTL | 38، DEC-035 |

## الخريطة
```
app/
  Modules/                     ← وحدات المجال (30 §هيكل الكود المقترح)
    Settings/                  ← 04 §الإعدادات + مفاتيح المرحلة (39)
      Enums/Cfg.php            ← كل CFG-0xx بقيمته الافتراضية ونوعه ومجموعته
      Enums/OperatingMode.php  ← BR-006
      Services/SettingsRepository.php ← القراءة الوحيدة لجدول settings (مخزَّن مؤقتًا)
      Services/FeatureGate.php ← CFG-090/091، BR-009، رفض FEATURE_DISABLED
    Identity/ Geography/ Catalog/ Customers/ Providers/
    Orders/
      Enums/                   ← OrderStatus (21)، OrderEventCode (24)، CancelReason (13)
      Models/Order.php         ← الكيان الواحد (DEC-027)
      StateMachine/TransitionTable.php ← جدول T-01..T-29 حرفيًا من 10
      StateMachine/OrderStateMachine.php ← المنفذ الوحيد لتغيير الحالة
      Services/ProviderEligibility.php ← BR-022 بفرعَي الوضع
      Services/SchedulingCalendar.php ← ساعات الخدمة والفترات (BR-017، CFG-020..022)
      Services/OrderDeadlines.php ← مهلات الوضعين (CFG-010/013 مقابل CFG-093)
      Services/OrderClosure.php   ← آثار الإغلاق: العمولة والتسوية (BR-060، BR-061)
      Services/OrderTermination.php ← آثار الخروج النهائي: العروض والمقترحات والمحادثات
      Jobs/                       ← المهلات التلقائية (30 §المهام المجدولة)
      Actions/                 ← PublishRequestAction (T-01)، AssignProviderAction (T-27/T-29)، CompleteInspectionOnlyAction (T-13/T-28)
    Offers/ Pricing/ Settlements/ Reviews/ Support/
    Payments/
      Services/PaymentChannels.php ← القنوات المفعّلة CASH/INSTAPAY_MANUAL/FAWRY (CFG-053..058، DEC-050)
      Actions/Submit|Confirm|RejectInstapayTransferAction.php ← BR-057
    Content/                   ← الصفحات القانونية بنسخ (DEC-051) + TermsService (BR-018)
    Communication/Mail/ZeptoMailTransport.php ← البريد عبر ZeptoMail API (DEC-049)
    Catalog/Services/CoverageCheck.php ← تحذير فئة/منطقة بلا فني نشط (DEC-053)
  Filament/
    Support/NavigationGroups.php
    Resources/                 ← مجمّعة حسب الوحدة
    Widgets/                   ← عدّادات 16 + شريط وضع التشغيل
    Resources/PendingTransfers ← تحويلات إنستاباي بانتظار التأكيد (المدير العام)
    Resources/LegalPages       ← وحدة «الصفحات» (المدير العام)
    Pages/OperatingSettings.php ← مفاتيح المرحلة + كل CFG
  Http/Api/V1/               ← كونترولرز وموارد API v1 (31)
  Http/Middleware/           ← X-App-Mode، Idempotency-Key، expected_version
  Console/Commands/GenerateDesignTokens.php ← `design:tokens`: غلاف لـ `tools/gen-design` (DEC-043)
  Console/Commands/GenerateOpenApiSpec.php  ← توليد عقد API من المسارات (31)
  Support/Exceptions/          ← رموز الأخطاء في 31

docs/api/openapi.json          ← العقد المولَّد (لا يُعدَّل يدويًا)
design/tokens.json             ← المصدر الواحد للهوية (43 §2) → `tools/gen-design` → CSS + موارد XML لأندرويد (DEC-047) + Swift + سمة البريد + أيقونات الفئات
tools/compare-android-xml      ← مقارنة لقطات XML بأهداف Compose المعتمدة مع نسبة الاختلاف (43 §13)
androidapp/                    ← تطبيق أندرويد (42)
iosapp/                        ← تطبيق iOS (42)
```

## القواعد الملزمة في الكود
1. **الحالة لا تتغير إلا عبر `OrderStateMachine`.** يحرسها اختبار يفحص التوكِنز في `tests/Feature/Architecture/StateMachineGuardTest.php`.
2. **كل انتقال له صف في 10.** إضافة انتقال بلا تعديل الوثيقة تكسر اختبار الرموز.
3. **الميزة المعطّلة تُرفض قبل أي قفل أو كتابة** (`FeatureGate` داخل الآلة نفسها) — 39، 30.
4. **الإعدادات تُقرأ من `SettingsRepository` وحده**، لا `Setting::query()` في كود المجال.
5. **المبالغ من `OrderAmounts` وحده** (BR-044)؛ لا حساب مبلغ في واجهة أو Action.
6. **لا قيمة لون أو مقاس مكتوبة يدويًا** في أي منصة؛ المصدر `design/tokens.json` (DEC-043)، والتوليد بـ `tools/gen-design`، ويحرسها `tools/lint-design` و`DesignTokensTest`.
7. **لا مسار API يعدّل الحالة مباشرة**، ولا `PUT` للإجراءات؛ كل إجراء POST مرتبط بانتقال في 10 — يحرسها `ApiContractTest`.
8. **فشل السياسة يُترجم 404 لا 403** (23 §قاعدة الملكية)، والإدارة خارج قاعدة الملكية (`before()` في السياسات).
9. **لا حذف فعلي** للطلبات والعروض والمقترحات والدفعات والأحداث (29 §الحذف) — ولهذا لا `cascadeOnDelete` عليها.

## ما ينقص للإطلاق (حالة اليوم)
الخطة الكاملة وحالة كل بند: **41-DELIVERY-PLAN**.

مبني وجاهز: المخطط الكامل، الـ enums، آلة الحالات بجدولها الكامل، بوابة المفاتيح، **دورة الطلب كاملة** (نشر، تعيين، زيارة، مقترحات، عروض، إلغاء، اعتذار، دفع، إغلاق، نزاع)، المهام المجدولة، السياسات، **API v1 وعقد OpenAPI مولَّد ومحروس**، لوحة الإدارة مع التدخلات، **توكنات التصميم المولَّدة للمنصات الثلاث**.

نُفذت في API: الوسائط والمحادثة والتتبع وETA، وروابط المشاركة وصفحتها العامة SCR-W01، والتقييم. لم يُبنَ بعد: تكامل فوري وFCM، بقية واجهات النزاعات والبلاغات والإشعارات، وشاشات المنتج في التطبيقين (بوابة الدفعة 3 مغلقة حتى اعتماد معرض المنصتين).

## التشغيل محليًا
```bash
composer install && npm install
cp .env.example .env && php artisan key:generate
php artisan migrate --seed          # ينشئ الإعدادات والكتالوج وحسابَي إدارة
npm run build && php artisan serve
```
حسابا التطوير: `super@bzr.test` / `ops@bzr.test` بكلمة مرور `password`.
الاختبارات تعمل على MySQL (`bzr_ddb_test`) لأن المخطط يستخدم أعمدة مولّدة: `php artisan test`.
