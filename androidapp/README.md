# androidapp — تطبيق أندرويد

Kotlin · Jetpack Compose · Material 3 (ASM-09). عقد بناء الواجهة الملزم: **docs/43-UI-BUILD-CONTRACT**، وتفاصيل المعمارية في **docs/42-MOBILE-ARCHITECTURE**.

## الهيكل
```
app/                     نقطة دخول معرض المكونات في الدفعة 1
core/design/             tokens ومكونات Compose والمعرض واختبارات Paparazzi
core/network/            طبقة الشبكة وعقد 31
feature/<module>/        وحدات تطابق وحدات الخادم (orders, offers, pricing, …)
```

## قواعد لا تُخالَف
1. **اسم ملف الشاشة يحمل رمزها من 25** — `C09TrackingScreen.kt`. شاشة بلا رمز تُرفض في المراجعة.
2. **لا قيمة لون أو مقاس مكتوبة يدويًا** — كلها من الملفات المولّدة. المصدر الوحيد هو `design/tokens.json`، والتوليد يتم بـ `tools/gen-design`.
3. **شاشات العروض ورسوم المعاينة تُبنى كاملة وتُخفى بمفتاح** من `GET /config` — لا بـ build flavor ولا بثابت (DEC-041).
4. **لا طابور إجراءات دون اتصال** (DEC-042).
5. **RTL أولًا** — كل شاشة تُختبر بالعربية قبل أي شيء.

## أوامر الدفعة 1

```bash
php tools/gen-design --check
php tools/lint-design
cd androidapp
export ANDROID_HOME=$HOME/Android/Sdk   # أو sdk.dir في local.properties (غير متتبع)
./gradlew :app:assembleDebug :core:design:verifyPaparazziDebug
```

نسخة `debug` تستخدم `applicationId = com.bremo.app` **بلا أي لاحقة**. مفتاح Maps لا يُكتب في الكود ولا في ملف متتبع. محليًا يوضع في `androidapp/local.properties` غير المتتبع بالاسم التالي:

```properties
MAPS_ANDROID_API_KEY=development-key
```

ويمكن بدلًا من ذلك حقنه وقت البناء كمتغير بيئة أو Gradle property:

```bash
MAPS_ANDROID_API_KEY='development-key' ./gradlew :app:assembleDebug
```

يُقيد المفتاح في Google Cloud بـ`com.bremo.app` وSHA-1 لشهادة البيئة (development/staging/production)، مع Maps SDK for Android فقط. مفتاح Routes الخادمي منفصل.

بصمة شهادة development الحالية من `./gradlew :app:signingReport`:

```text
61:2B:B6:53:61:A4:02:24:F6:14:C4:09:00:7D:5E:3F:05:44:C7:13
```

تُستخرج بصمتا staging وproduction من شهادتيهما عند توفير إعدادات التوقيع؛ لا توجد شهادة release داخل المستودع.

لتحديث لقطات المعرض بعد تغيير مقصود:

```bash
cd androidapp
./gradlew :core:design:recordPaparazziDebug
```

## ترتيب البناء

الدفعة 1 محصورة في الأساس المشترك ومكتبة المكونات والمعرض. لا تبدأ شاشة منتج قبل اعتماد صفحة مقارنة المعرض، ثم يُتبع تسلسل 43 §11.

## الحالة
✅ معرض المكونات و16 لقطة Paparazzi معتمدة ومحفوظة في `design/reference/gallery/`. لم تُبنَ أي شاشة منتج بعد.
