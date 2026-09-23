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
      Actions/                 ← AssignProviderAction (T-27/T-29)، CompleteInspectionOnlyAction (T-13/T-28)
    Offers/ Pricing/ Payments/ Settlements/ Communication/ Reviews/ Support/
  Filament/
    Support/NavigationGroups.php
    Resources/                 ← مجمّعة حسب الوحدة
    Widgets/                   ← عدّادات 16 + شريط وضع التشغيل
    Pages/OperatingSettings.php ← مفاتيح المرحلة + كل CFG
  Support/Exceptions/          ← رموز الأخطاء في 31
```

## القواعد الملزمة في الكود
1. **الحالة لا تتغير إلا عبر `OrderStateMachine`.** يحرسها اختبار يفحص التوكِنز في `tests/Feature/Architecture/StateMachineGuardTest.php`.
2. **كل انتقال له صف في 10.** إضافة انتقال بلا تعديل الوثيقة تكسر اختبار الرموز.
3. **الميزة المعطّلة تُرفض قبل أي قفل أو كتابة** (`FeatureGate` داخل الآلة نفسها) — 39، 30.
4. **الإعدادات تُقرأ من `SettingsRepository` وحده**، لا `Setting::query()` في كود المجال.
5. **المبالغ من `OrderAmounts` وحده** (BR-044)؛ لا حساب مبلغ في واجهة أو Action.
6. **لا حذف فعلي** للطلبات والعروض والمقترحات والدفعات والأحداث (29 §الحذف) — ولهذا لا `cascadeOnDelete` عليها.

## ما ينقص للإطلاق (حالة اليوم)
مبني وجاهز: المخطط الكامل، الـ enums، آلة الحالات بجدولها الكامل، بوابة المفاتيح، التعيين وإعادة التعيين، إنهاء المعاينة المجانية، حساب المبالغ، لوحة الإدارة (الطلبات، مقدمو الخدمة، الكتالوج، المدن، الإعدادات، العدّادات).

لم يُبنَ بعد: باقي Actions الطلب (النشر، التتبع، المقترحات، الدفع، الإنهاء، النزاع)، واجهات API v1، تكامل فوري، FCM، المهام المجدولة، سياسات الصلاحيات التفصيلية (23)، شاشات النزاعات والمال في اللوحة.

## التشغيل محليًا
```bash
composer install && npm install
cp .env.example .env && php artisan key:generate
php artisan migrate --seed          # ينشئ الإعدادات والكتالوج وحسابَي إدارة
npm run build && php artisan serve
```
حسابا التطوير: `super@bzr.test` / `ops@bzr.test` بكلمة مرور `password`.
الاختبارات تعمل على MySQL (`bzr_ddb_test`) لأن المخطط يستخدم أعمدة مولّدة: `php artisan test`.
