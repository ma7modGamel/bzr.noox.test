# iosapp — تطبيق iOS

Swift · SwiftUI (ASM-09). عقد بناء الواجهة الملزم: **docs/43-UI-BUILD-CONTRACT**، وتفاصيل المعمارية في **docs/42-MOBILE-ARCHITECTURE**.

## الهيكل
```
BzrApp/                          نقطة دخول معرض المكونات في الدفعة 1
Core/                            حزمة Swift مستقلة: models وAPI وJSON والتنسيق وavailable_actions والنصوص
Packages/DesignSystem/           tokens ومكونات SwiftUI والمعرض واختبارات SnapshotTesting
Packages/<Module>/               وحدات تطابق وحدات الخادم
```

## قواعد لا تُخالَف
1. **اسم ملف الشاشة يحمل رمزها من 25** — `C09TrackingView.swift`. شاشة بلا رمز تُرفض في المراجعة.
2. **لا قيمة لون أو مقاس مكتوبة يدويًا** — كلها من الملفات المولّدة. المصدر الوحيد هو `design/tokens.json`، والتوليد يتم بـ `tools/gen-design`.
3. **شاشات العروض ورسوم المعاينة تُبنى كاملة وتُخفى بمفتاح** من `GET /config` — لا بـ scheme ولا بثابت (DEC-041). مراجعة App Store تستغرق أيامًا، فربط الظهور بإصدار يكسر وعد 39.
4. **لا طابور إجراءات دون اتصال** (DEC-042).
5. **RTL أولًا** — `layoutDirection = .rightToLeft` في كل معاينة.

## تحقق Linux اليومي

```bash
swift build --package-path iosapp/Core
swift test --package-path iosapp/Core
find iosapp -type f -name '*.swift' -not -path '*/.build/*' -print0 \
  | xargs -0 swift-format lint --strict --parallel
docker run --rm -v "$PWD:/workspace" -w /workspace \
  ghcr.io/realm/swiftlint:0.65.0 lint --strict --config .swiftlint.yml
php tools/check-parity
php tools/lint-design
```

`Core` لا يستورد SwiftUI أو UIKit، ويستخدم نفس fixtures التي تختبر Android. `DesignSystem` يعتمد عليه للتنسيق بدل تكرار المنطق.

## تحقق iOS النهائي على macOS

```bash
cd iosapp
xcodegen generate
xcodebuild -project BzrApp.xcodeproj -scheme BzrApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' build

# لقطات المعرض (1×، 400×800) لصفحة المقارنة
cd Packages/DesignSystem
GALLERY_EXPORT_DIR="$PWD/../../../reports/batch-1/gallery/ios" \
  xcodebuild -scheme DesignSystem -destination 'platform=iOS Simulator,name=iPhone 16' test
cd ../../..
AUTH_EXPORT_DIR="$PWD/reports/batch-3a/auth/ios" \
  xcodebuild -project iosapp/BzrApp.xcodeproj -scheme BzrApp \
  -destination 'platform=iOS Simulator,name=iPhone 16' test
php tools/gen-gallery-report && php tools/gen-auth-report
```

الاختبار يصدّر كل الحالات دائمًا، ويقارنها بمراجع Android المعتمدة عبر صفحة المقارنة. مهمة `Final iOS validation` يدوية فقط ولا تُشغّل قبل الدفعة 8؛ تبني الصفحة وترفع artifact باسم `final-ios-comparison`.

## الرفع إلى App Store من الـ Mac

الكود جاهز للرفع. المطلوب من المالك فقط ما يخص حسابه:

1. ضع `GoogleService-Info.plist` في `iosapp/BzrApp/`، وارفع مفتاح APNs (`.p8`) على Firebase (DEP-PUSH-02).
2. في Apple Developer: فعّل Push Notifications وAssociated Domains على `com.bremo.app`.
3. استبدل `BzrApp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png` بلوجو بريمو النهائي (1024×1024، بلا شفافية). الأيقونة الحالية مؤقتة.
4. ابنِ النسخة وارفعها:

```bash
cd iosapp
xcodegen generate
xcodebuild -project BzrApp.xcodeproj -scheme BzrApp -configuration Release \
  -destination 'generic/platform=iOS' -archivePath build/Bremo.xcarchive \
  BZR_TEAM_ID=<Team ID> -allowProvisioningUpdates archive
```

ثم ارفعها من Xcode › Organizer › Distribute App › App Store Connect. نسخة Release توقّع تلقائيًا، وتستخدم `aps-environment = production`، وترفع رموز dSYM إلى Crashlytics.

الجاهز في الكود: iOS 17، وiPhone فقط بالوضع الرأسي، و`PrivacyInfo.xcprivacy`، ونصوص الأذونات بالعربي في `InfoPlist.xcstrings` (المصدر `design/strings.ar.json`)، و`ITSAppUsesNonExemptEncryption = NO`، وحذف الحساب من C33، والإصدار `1.0.0 (1)`.

## ترتيب البناء

كل شاشة تُكتب في Compose وSwiftUI في الدفعة نفسها. اعتماد لقطة Android يفتح الدفعة التالية؛ Xcode ولقطات iOS في الدفعة 8 وفق DEC-045 و43 §11.

## الحالة
✅ معرض SwiftUI واختبار اللقطات جاهزان، و`Core` يُبنى ويُختبر على Linux، وكل Swift يمر على swift-format وSwiftLint. **لم يُشغّل Xcode بعد عمدًا**؛ أول بناء ولقطات iOS في الدفعة 8.
