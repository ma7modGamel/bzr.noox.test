# تقرير دفعة التحويل إلى XML Views — الخطوتان 1 و2 (بوابة اعتماد)

القرار: DEC-047 (43 §13). هذه الدفعة تغطي **التوليد والأنماط** و**المكونات الـ 42 والمعرض**، وتقف عند بوابة الاعتماد قبل تحويل الشاشات.

## بوابة الاعتماد

- صفحة المقارنة: [`gallery/comparison.html`](gallery/comparison.html). تعرض لكل لقطة: Compose المعتمدة | XML | خريطة الاختلاف، مع نسبة الاختلاف.
- النتيجة: **16 من 16 لقطة مطابقة** وفق المقياس (فرق قناة ≤ 24 من 255، مع إزاحة بكسل واحد لتنعيم الخط وموضع النص تحت البكسل، و«مطابقة» إذا لم يتجاوز الاختلاف 0.5٪).
- المطلوب منك: اعتماد مكتبة XML والمعرض، والبت في الملاحظة المرئية الوحيدة أدناه. بعد الاعتماد تبدأ الخطوة 3 (الشاشات 3أ ← 4د).

| اللقطة | الاختلاف | اللقطة | الاختلاف |
|---|---:|---|---:|
| buttons | 0.02٪ | provider-top | 0.15٪ |
| navigation | 0.39٪ | provider-bottom | 0.06٪ |
| selection-top | 0.06٪ | tracking | 0.02٪ |
| selection-bottom | 0.06٪ | feedback-top | 0.00٪ |
| fields-top | 0.01٪ | feedback-bottom | 0.01٪ |
| fields-bottom | 0.02٪ | chat | 0.01٪ |
| cards-top | 0.30٪ | media-sheet | 0.00٪ |
| cards-bottom | 0.45٪ | text-area | 0.01٪ |

### ملاحظة مرئية واحدة رغم المطابقة بالمقياس (للمالك)
- **navigation — عناوين `BottomNav` أسفل الأيقونة بحوالي 7 بكسل** مقارنة بـ Compose. السبب: `BottomNavigationView` في Material Components يثبّت العنوان على خط سفلي خاص به، ولا يوفّر ضبطًا لموضعه داخل الشريط. المسافات والألوان والحجم متطابقة. البديلان: (أ) اعتماد الشكل كما هو، أو (ب) بناء شريط مخصص بدل `BottomNavigationView` لمطابقة Compose بالبكسل. التوصية: (أ).
- **cards-bottom (0.45٪)**: نص الزر المضغوط داخل `OfferCard` أعلى بـ 1–2 بكسل (توسيط `MaterialButton` الرأسي)، وحافة الدرع الأبيض حول صورة الفني. غير ملحوظ بالعين.

## ما تم

### 1. التوليد والأنماط (`tools/gen-design` ← `tools/gen-design-android-views.php`)
| المخرج | المحتوى |
|---|---|
| `values/colors.xml` | `bremo_*` لكل لون |
| `values/dimens.xml` | المسافات والزوايا والمقاسات والحدود وأحجام الخط (sp) |
| `values/bremo_values.xml` | الشفافية، النسب، العتبات، أعمدة الشبكات، مدة الحركة، شفافية الخلفية المعتمة |
| `values/bremo_styles.xml` | `TextAppearance.Bremo.*` (11 نمطًا)، `Widget.Bremo.*` (أزرار، كروت، شرائح، حقل، شريط سفلي، ورقة سفلية)، `Theme.Bremo` |
| `drawable/bremo_*` | 50 شكلًا (كروت، حقول بحالاتها، شارات، دوائر المراحل…)، ومحددات الحالة المختارة، وقوس التحميل الثابت، وفواصل المسافات |
| `color/bremo_*` | قوائم ألوان بالحالات (الضغط، الاختيار، التركيز، عنصر الشريط) |
| `font/cairo.xml` | عائلة Cairo بأوزانها الأربعة |

`DesignTokens.kt` الخاص بـ Compose ما زال يُولَّد لأن شاشات Compose ما زالت تعمل؛ يُحذف في الخطوة 4 مع Compose.

### 2. المكونات والمعرض
- 42 مكوّنًا في `androidapp/core/design/src/main/kotlin/noox/bzr/design/views/`، لكل مكوّن كلاس وملف layout مستقل (`OfferCardView.kt` + `view_offer_card.xml`)، بخصائص `declare-styleable` وsetters بنفس أسماء مدخلات SwiftUI وترتيبها.
- `design/parity.json`: عمود أندرويد يشير إلى الكلاس والـ layout، و`tools/check-parity` يستخرج المدخلات من خصائص الكلاس ويتأكد أنه يُحمّل الـ layout.
- المعرض أعيد بناؤه بـ XML (`GalleryViews.kt` و`gallery_page_*.xml`) بنفس الصفحات والترتيب وبيانات `design/fixtures/gallery/components.json`.
- Paparazzi على الـ Views بنفس المقاس 400×800، RTL، `Theme.Bremo`.
- لقطات Compose المعتمدة جُمّدت هدفًا في `design/reference/android-compose/gallery/` حتى تبقى بعد حذف Compose.

### القواعد والفحص الآلي
| الفحص | الحالة |
|---|---|
| `tools/lint-design`: لا hex ولا dp/sp ولا نص مكتوب في `res/layout` والكود، ولا `left/right`، و`supportsRtl` في كل manifest | ✅ (مختبر بملف مخالف عمدًا) |
| Android Lint: `RtlHardcoded` و`RtlCompat` و`RtlEnabled` أخطاء تُوقف البناء | ✅ |
| `tools/lint-design --no-compose`: لا اعتماد ولا import لـ Compose | ⏳ مفعّل عند الخطوة 4 (278 سطرًا متبقيًا الآن) |
| `tools/compare-android-xml gallery --strict` في CI | ✅ 16/16 |
| `verifyPaparazziDebug` (لقطات Compose الحالية + XML) و`assembleDebug` و`lintDebug` واختبارات الوحدة | ✅ |

## انحرافات موثّقة عن قائمة المالك (تحتاج موافقتك)
1. **الحقول الأربعة (`AppTextField`/`SecureTextField`/`AmountField`/`TextAreaField`)**: `EditText` داخل إطار مولّد بدل `TextInputLayout`. السبب: تسمية الحقل في 38 فوق الصندوق بينما `TextInputLayout` يضعها داخل الحد، ومحرّك اللقطات لا يرسم نص الحقل داخله (ظهر فارغًا في المقارنة).
2. **`SelectableChip`**: `TextView` بإطار اختيار مولّد بدل `Chip`، لأن `Chip` يفرض محاذاة النص للبداية ولا يوسّطه في الشرائح المتساوية العرض. `SortChips` يستخدم `Chip`/`ChipGroup` كما طُلب.
3. **الـ ViewModels**: تستخدم حاليًا `mutableStateOf` من Compose لحمل الحالة. «بلا تعديل» و«لا import لـ Compose» متعارضان، فالحل في الخطوة 3: استبدال **حاوية الحالة فقط** بـ `StateFlow` دون أي تغيير في المنطق أو حالات الاختبار المشتركة. يُذكر في تقرير الخطوة 3.

## تغييرات صغيرة مرافقة
- `applicationId = com.bremo.app`، والتطبيق يستخدم `Theme.Bremo`، والاسم تحت الأيقونة «بريمو» (`app_launcher_name`) — DEC-048.
- إصلاح في المولّد: لون `scrim` كان يُكتب في CSS بترتيب ARGB؛ صار RRGGBBAA الصحيح.

## الخطوات التالية بعد الاعتماد
3. الشاشات بالترتيب 3أ، 3ب، 3ج، 4أ..4د: Fragment لكل شاشة + `nav_graph.xml` + RecyclerView/ListAdapter للقوائم + الخريطة، وفي نفس الدفعة تحديث fixtures «مدينة نصر، القاهرة» ← «الحي الأول، دمياط الجديدة» وإعادة اعتماد اللقطات المتأثرة مرة واحدة.
4. حذف Compose وتفعيل `--no-compose` في CI.

## iOS
لا تغيير واجهة في هذه الخطوة (iOS يبقى SwiftUI). تغيّر `bundle id` إلى `com.bremo.app` والاسم تحت الأيقونة في `InfoPlist.xcstrings`.
