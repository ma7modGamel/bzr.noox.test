# الدفعة figma-a — الخط والألوان والأيقونات من ملف Figma (DEC-070)

**التاريخ:** 2026-10-09 · **الحالة:** ✅ معتمدة من المالك 2026-10-10 · **الخادم:** من غير أي تغيير (الملاحظات في `BACKEND-NOTES.md`)

## 1. اللي اتعمل
| البند | التفاصيل |
|---|---|
| **الربط بـ Figma** | سيرفر Figma MCP الرسمي (`https://mcp.figma.com/mcp`) متسجّل بحساب المالك، وإعداده محلي على الجهاز بس |
| **الخط** | IBM Plex Sans Arabic، 4 أوزان ثابتة، من Google Fonts بإصدار 1.101. اتولّد للتطبيقين والويب. Cairo فضل للشعار بس، وده قفل OD-07 |
| **الألوان** | نظام Material 3 اللي في الملف على نفس أسماء الرموز، فكل الشاشات (العميل والفني والدخول) خدت الشكل الجديد من غير تعديل في الكود |
| **التدرجات** | بقت مسطحة زي الملف (الأزرار والأيقونات والخلفيات). تدرّج كارت الرئيسية (`hero`) والشعار (`splash`) لسه زي ما هم، وهيتغيّروا في دفعة الشاشات |
| **الأحجام والزوايا** | زر وحقل ارتفاعهم 48 وزاويتهم 14، وكارت زاويته 16، وشرائح وشارات كاملة الاستدارة، وسلّم خطوط 32/22/19/17/15/14/13/12 |
| **الأيقونات** | 45 أيقونة واجهة و13 أيقونة فئة من Material Symbols Outlined، نفس عائلة الملف. منهم 5 جداد: `search` و`more-horizontal` و`tag` و`payments` و`verified`. أيقونات الفئات بقت بلون واحد، وده قفل OD-08 |
| **الوثائق** | DEC-070 في 34، وقفل OD-07 وOD-08 في 35، و38 §12، وREADME الخطوط والأيقونات |

## 2. تصحيحات على ملف Figma (اتعالجت في الكود)
1. **لون الزر الأساسي:** الملف بيستخدم `#0AAE9F` في 8 أزرار، و`#006A61` في 4. النص الأبيض على `#0AAE9F` تباينه 2.8:1، وده أقل من 4.5:1، فاتعمد `#006A61` (تباين 6.5:1)، وهو نفس اللون اللي الملف سمّاه «Primary Decision CTA».
2. **زوايا الأزرار وارتفاعها:** الملف فيه 14 و12 و8، وارتفاعات 40 و44 و48. اتوحّدت على زر «متابعة» في التدفق: ارتفاع 48 وزاوية 14.
3. **أقل حجم نص:** 11 في الملف بقت 12، عشان النقط والتشكيل العربي يفضلوا مقروءين.
4. **الخطوط الغلط:** 40 نص في الملف مكتوبين بخط Noto Sans Arabic، فاتوحّدوا على IBM Plex.
5. **الشاشات المكررة:** في نسخ متعددة من «طلباتي» و«حسابي» و«الإشعارات» و«عروض الفنيين» و«التقييم»، ومنها نسختين Desktop (1280). المعتمد هو أحدث نسخة بعرض 390، وده هيتحدد بالتفصيل في دفعة كل شاشة.

## 3. جدول ربط الأيقونات (اسم الملف ← رمز Material)
`account`←person · `arrow-back`←arrow_back · `bell`←notifications · `calendar`←calendar_month · `camera`←photo_camera · `car`←directions_car · `chat`←chat_bubble · `check`←check · `chevron`←chevron_right · `clipboard-plus`←assignment_add · `clock`←schedule · `close`←close · `edit`←edit · `empty`←inbox · `error`←error · `home`←home · `image`←image · `info`←info · `location`←location_on · `logout`←logout · `menu`←menu · `messages`←chat · `mic`←mic · `more`←more_vert · `more-horizontal`←more_horiz · `navigation`←navigation · `offline`←wifi_off · `orders`←assignment · `payments`←payments · `phone`←call · `plus`←add · `search`←search · `send`←send · `share`←share · `shield`←verified_user · `siren`←emergency · `star`←star · `star-filled`←star (fill) · `tag`←sell · `trash`←delete · `verified`←verified · `video`←videocam · `warning`←warning · `warning-filled`←warning (fill) · `wrench`←handyman

**الفئات:** plumbing←plumbing · electricity←bolt · air-conditioning←mode_fan · carpentry←carpenter · appliances←kitchen · painting←format_paint · aluminum-glass←window · home-installations←home_repair_service · ironwork←hardware · pest-control←pest_control · satellite←satellite_alt · tiles-flooring←grid_view · water-tanks←water_full

أول 8 فئات مطابقة لأيقونات Figma. الخمسة الباقيين مش موجودين في الملف، فاتختاروا من نفس العائلة.

## 4. التحقق
| الفحص | النتيجة |
|---|---|
| `tools/gen-design` و`--check` | ✅ 374 مخرج، ومحدّثين |
| `tools/lint-design` | ✅ مفيش قيم أو نصوص مكتوبة يدوي |
| `tools/check-parity` | ✅ 42 مكوّن، و58 شاشة، و697 مفتاح نص |
| خادم `php artisan test` | ✅ 262/262، بعد تحديث قيدين في `DesignTokensTest` (اسم الخط وأسلوب الفئات) لأن القرار نفسه اتغيّر |
| أندرويد: الاختبارات واللقطات | ✅ نجحت. **885 لقطة اتسجّلت تاني** و2 جداد (`scr-c10-session_expired` و`scr-p18-dues_blocked`، كانوا ناقصين من دفعات DEC-065 و7b) |
| iOS | ⚠️ ملفات Swift اتولّدت من نفس المصدر، لكن ما اتبنتش. SwiftUI مش بتشتغل على Linux، فمحتاجة Xcode |

**قبل وبعد** (الرئيسية، والعروض، وتطبيق الفني، والدخول): `before-after.png`

## 5. عوائق (محتاجة المالك)
1. **حد استدعاءات Figma (خطة Starter):** اتقرت 11 شاشة كاملة بالكود والصورة، و21 شاشة رجعت بخطأ الحد. الحلول:
   - (أ) ترقية الخطة لـ Professional بمقعد Dev أو Full.
   - (ب) **Personal Access Token** من إعدادات Figma ← Security، يتحط في `~/.figma_token`. واجهة Figma REST العادية مش محسوبة على حد MCP، وبتدّي الألوان والصور والأيقونات كاملة.
   - (ج) نستنى الحد يتصفّر.

   من غير واحد من دول، دفعات (ج) و(د) هتتبني على هيكل العناصر بس، من غير القيم الدقيقة.
2. **قرارات الخادم B-01 لحد B-12** في `BACKEND-NOTES.md`.

## 6. الدفعات الجاية (بعد الموافقة)
- **(ب)** طلب الخدمة 5 خطوات (3002:2 و144 و1125 و1270 و1401 و1519)، ومعاه المكوّنات المشتركة: كارت الخدمة من غير إطار، وشريط التقدم المقسّم، والشريط السفلي الثابت. **الكود والصور جاهزين.**
- **(ج)** الرئيسية والعروض وملف الفني وتأكيد الحجز (270 و446 و873 و636 و1631 و1757). **الكود والصور جاهزين.**
- **(د)** التتبع والفحص والدفع والتقييم والرسائل وطلباتي وحسابي والإشعارات. **مستنية حل العائق 1.**

بعد الموافقة: `tools/approve-android-gallery` عشان صور مكوّنات المعرض المرجعية تتحدّث.
