# تقرير النشر الإنتاجي — `dg.dnbscy.com`

التاريخ: 2026-10-03

## النتيجة

- الموقع يعمل عبر `https://dg.dnbscy.com`، مع تحويل دائم من HTTP إلى HTTPS.
- API الإنتاجية: `https://dg.dnbscy.com/api/v1/`.
- صفحة سياسة الخصوصية منشورة: `https://dg.dnbscy.com/privacy`.
- API سياسة الخصوصية: `https://dg.dnbscy.com/api/v1/pages/privacy`.
- لوحة الإدارة: `https://dg.dnbscy.com/admin/login`.
- بيانات دخول المدير الأول محفوظة على الخادم فقط في `/home/dimensae/.bzr-admin-credentials` بصلاحية `600`، ولا توجد في Git أو هذا التقرير.

## الخادم وقاعدة البيانات

- الإصدار المنشور: `d3b857a5bf44a5c4a5aab2895e863913dd8d37b4`.
- مسار الإصدار: `/home/dimensae/apps/bzr/releases/d3b857a`.
- الرابط الحالي: `/home/dimensae/apps/bzr/current`.
- PHP-FPM: PHP `8.4.26` مع `pdo_mysql`.
- Apache وPHP-FPM وMariaDB وعامل الطوابير و`crond` تعمل كخدمات نشطة.
- قاعدة الإنتاج الجديدة: `dimensae_bzr`، وجميع migrations منفذة.
- نُفذت seeders الخاصة بالإعدادات والكتالوج والصفحات القانونية وأسباب الدعم.
- عامل الطوابير مُدار بواسطة `/etc/systemd/system/bzr-queue.service`.
- Laravel scheduler يعمل كل دقيقة من `/etc/cron.d/bzr-scheduler`، وتم التحقق من تنفيذ مهامه.
- قاعدة `dimensae_ea` القديمة حُذفت بعد حفظ نسخة سليمة في `/home/dimensae/backupcwp/bzr-predeploy/dimensae_ea-2026-10-02.sql.gz`.

## تطبيقات الهاتف

- Android وiOS يستخدمان افتراضيًا `https://dg.dnbscy.com/api/v1/`.
- مضيف App Links وUniversal Links مضبوط على `dg.dnbscy.com`.
- معرّف Android وiOS هو `com.bremo.app`.
- تغييرات Android محفوظة في commit مستقل: `09140111f2fd355dce28dcb8d5ef56fd453b2f30`.
- بُني Android App Bundle بنجاح في `/home/tro/bzr-android-xml/androidapp/app/build/outputs/bundle/release/app-release.aab`.
- SHA-256 للملف: `b476a6c979cf04c2a6bcb96ffc2552c28e2057c7449dc9402aee7457b379f34a`.
- ملف `google-services.json` محلي فقط وأُضيف إلى التجاهل لمنع دخوله Git.

## التحقق المنفذ

- Laravel: `233` اختبارًا ناجحًا و`1515` assertion.
- Android: `./gradlew test bundleRelease` ناجح، ويتضمن الـ bundle نطاق API وApp Links الإنتاجي.
- iOS Core: `15` اختبارًا ناجحًا.
- فحص parity: `42` مكوّنًا و`58` شاشة هاتف و`686` مفتاح نص.
- بناء الواجهة `npm run build` ناجح.
- الروابط `/` و`/up` و`/privacy` و`/api/v1/pages/privacy` و`/admin/login` ترجع `200` عبر HTTPS.
- `/.well-known/assetlinks.json` و`/.well-known/apple-app-site-association` متاحان عبر HTTPS.

## المطلوب قبل الرفع الفعلي للمتاجر

هذه البنود تحتاج هويات أو أسرار مالك الحساب ولا يمكن استنتاجها أو توليد بديل عنها بأمان:

1. توفير مفتاح Android Upload/Release الأصلي أو إنشاء مفتاح جديد بعد تأكيد أن التطبيق جديد. ملف AAB الحالي ناجح لكنه غير موقّع، لذلك ليس جاهزًا للرفع إلى Play Console حتى التوقيع.
2. بعد التوقيع، إضافة بصمة SHA-256 لشهادة الرفع وشهادة Play App Signing إلى `ANDROID_APP_LINK_SHA256` وإعادة بناء AAB النهائي.
3. توفير Apple Team ID وشهادات App Store Connect وملف provisioning وAPNs، ثم ضبط `DEVELOPMENT_TEAM` و`APPLE_TEAM_ID` وبناء Archive/TestFlight على macOS. لا يتوفر SwiftUI/Xcode في خادم Linux الحالي.
4. اختبار Maps وPush على أجهزة حقيقية باستخدام مفاتيح production المقيدة بشهادات التوقيع.
5. إكمال إعداد البريد والدفع وبيانات المتجر ونماذج الخصوصية قبل الإطلاق العام.

ملاحظة: تم إنشاء commits محلية والنشر المباشر على الخادم. لم يُرفع GitHub لأن الوجهة الخارجية لم تكن مخولة صراحةً داخل بيئة التنفيذ.
