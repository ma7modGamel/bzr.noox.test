# تقرير تحويل أندرويد إلى XML Views — DEC-047 (الخطوات 1 إلى 4)

**الحالة:** اكتمل التحويل وحذف Compose. كل فحوص الإثبات (أ–هـ) ناجحة. **233 لقطة شاشة + 16 صفحة معرض = 249 من 249 مطابقة** للقطات Compose المعتمدة. اختبارات المنطق المشتركة كما هي: نفس الملفات ونفس الحالات، وكلها ناجحة.

- الفرع: `android/xml-conversion` في worktree منفصل: `/home/tro/bzr-android-xml`. لم يُدمج شيء في `main`.
- `androidapp/` وتقارير التحويل وأدوات أندرويد انتقلت إلى الـ worktree، وفي مجلد العمل الرئيسي روابط (symlinks) إليها، فالجلسات الأخرى تعمل كما هي.
- الملفات المشتركة (`design/`، `iosapp/`، `docs/`، `tools/gen-design`، `tools/check-parity`، `tools/lint-design`) تُقرأ من المجلد الرئيسي عبر روابط، ولا تدخل commits الفرع.
- صفحة المقارنة الشاملة: [`comparison.html`](comparison.html)، ولكل مجموعة صفحتها: المعتمدة | XML | الفرق | نسبة الاختلاف.

## ما تم، بالترتيب والبوابات

| الخطوة | الحالة |
|---|---|
| 1. التوليد والأنماط | ✅ (التقرير السابق) |
| 2. المكونات الـ 42 والمعرض | ✅ اعتمدتَها «كما هي» في 2026-09-25 |
| 3. الشاشات 3أ، 3ب، 3ج، 4أ، 4ب، 4ج، 4د | ✅ كل مجموعة لها صفحة مقارنة، وكلها مطابقة |
| 4. حذف Compose نهائيًا + فحص الإثبات | ✅ |

| المجموعة | الشاشات | اللقطات | الصفحة |
|---|---|---|---|
| المعرض | 11 صفحة | 16/16 | [gallery](gallery/comparison.html) |
| 3أ | C10–C13 (الدخول والتسجيل) | 28/28 | [3a](3a/comparison.html) |
| 3ب | C01–C08 | 41/41 | [3b](3b/comparison.html) |
| 3ج | C14–C17 | 28/28 | [3c](3c/comparison.html) |
| 4أ | C09، C20، C30، C31 | 29/29 | [4a](4a/comparison.html) |
| 4ب | C21–C24 | 31/31 | [4b](4b/comparison.html) |
| 4ج | C18، C19، C25–C28 | 46/46 | [4c](4c/comparison.html) |
| 4د | C29، C32–C35 | 30/30 | [4d](4d/comparison.html) |

المقياس نفسه المستخدم في المعرض: فرق القناة ≤ 24 من 255، مع إزاحة بكسل واحد، و«مطابق» إذا لم يتجاوز الاختلاف 0.5٪. أعلى فرق في الشاشات **0.38٪** (C01، عناوين الشريط السفلي، وهي الملاحظة التي اعتمدتَها في المعرض). لا توجد لقطة مختلفة ترجع للمالك.

**الهدف:** لقطات Compose المعتمدة للشاشات (233) جُمّدت قبل أي تعديل في `reports/xml-conversion/reference/compose-screens/`، وهو مجلد يملكه أندرويد لأن `design/` مشترك. لقطات المعرض المعتمدة في `design/reference/android-compose/gallery/` كما كانت.

**ملاحظة قياس واحدة:** شاشة الدخول C10 في Compose لم تكن ترسم خلفية، فلقطتها المعتمدة شفافة. أداة المقارنة الآن تركّب الصورتين فوق لون خلفية النافذة (`surface` من `tokens.json`، وهو ما يظهر على الجهاز) قبل القياس. بدون ذلك كانت الخلفية الشفافة ستُقرأ سوداء.

## التقنية المنفذة

- **Activity واحدة** (`MainActivity` : `AppCompatActivity`) + `NavHostFragment` + `res/navigation/nav_graph.xml` (39 وجهة) + **Fragment لكل شاشة** (`CustomerFragments.kt`، `AuthFragments.kt`). ViewBinding فقط، بدون DataBinding.
- التنقل يتبع حالة الـ ViewModel (`state.screen`) كما كان `CustomerJourneyFlow` يفعل: `CustomerRoutes.destination(state)`. شاشة واحدة في المكدس، مثل Compose الذي كان يستبدل المحتوى.
- الـ Fragment يجمع `StateFlow` بـ `repeatOnLifecycle(STARTED)` ويعرض فقط. كل شاشة View بـ layout مستقل (`screen_cXX_*.xml`) ودالة `render(state)`.
- القوائم: `RecyclerView` + `ListAdapter` و`DiffUtil` (`RowAdapter`) للعناوين، والأيام والفترات (أفقي وعمودان)، والوسائط، والمحادثات، والرسائل، والطلبات، والأسئلة الشائعة، والإشعارات، وخيارات الراديو.
- Material Components: `MaterialButton` و`MaterialCardView` و`BottomNavigationView` و`Chip`/`ChipGroup` داخل المكونات. الانحرافان المعتمدان في المعرض (الحقول بـ `EditText`، و`SelectableChip`) كما هما.
- RTL إجباري: جذر كل شاشة `layoutDirection = RTL`، و`supportsRtl="true"`، و`start`/`end` فقط (يتأكد منها `tools/lint-design` وAndroid Lint).
- لا WebView لأي شاشة. الدفع الإلكتروني (C22) يفتح رابط البوابة في المتصفح كما كان في Compose.
- لا لون hex ولا dp/sp ولا نص مكتوب مباشرة خارج الملفات المولّدة (`tools/lint-design --no-compose` ✅).

### ما تغيّر في الـ ViewModels
**حاوية الحالة فقط:** `mutableStateOf` (Compose) صار `MutableStateFlow` + `stateFlow`، في `CustomerViewModel` (+ `requiresAuthenticationFlow`) و`AuthViewModel`. لا تغيير في أي دالة أو قرار. `CustomerLogic` و`AuthLogic` و`BzrFormat` والـ API واختبارات المنطق كلها **بدون أي فرق** عن نقطة البداية (`git diff ab79ae6` فارغ لها).

منطق كان داخل Composables واتنقل:
- التوجيه (`CustomerJourneyFlow`/`CustomerAuthFlow`) انتقل إلى `CustomerRoutes` و`MainActivity` و`AuthFragment`، بنفس الشروط: C06 الفارغة تفتح C35، والشاشة غير المعروفة تفتح C01، و`route` في الدخول يفتح C12/C10 أو يدخل التطبيق.
- `loadHome()` عند ظهور شاشات العميل، كما كان `LaunchedEffect(Unit)`، ويُعاد عند إعادة إنشاء الـ Activity مثل Compose.
- لم يُنقل أي قرار عمل إلى الـ ViewModel، لأن الـ Composables لم يكن فيها غير التوجيه والعرض.

### تجربة على جهاز (محاكي Pixel 9a، API 37)
- الدخول ← «إنشاء حساب» ← رجوع ← كتابة البريد وكلمة المرور ← التحقق ← الإرسال ← رسالة تعذّر الاتصال: ✅
- دخول ناجح أمام خادم تجريبي محلي يرد على `auth/login` فقط ← انتقال لشاشات العميل ← `loadHome` ← C01 بحالة الخطأ: ✅
- تدوير الشاشة: بدون انهيار، والحالة محفوظة: ✅
- الخادم الحقيقي لم يكن يعمل، فلم تُجرَّب شاشات العميل ببيانات حقيقية.

### إصلاحان ظهرا في التجربة (كانا موجودين في نسخة Compose أيضًا)
1. **الحواف (edge-to-edge):** مع `targetSdk 35` يرسم أندرويد المحتوى تحت شريط الحالة، فكان شريط العنوان مغطى. أضفت هوامش أشرطة النظام على حاوية الشاشات.
2. **الاتصال بالخادم المحلي في نسخة debug:** `targetSdk 35` يمنع HTTP، والعنوان الافتراضي `http://10.0.2.2:8000`، فنسخة debug لم تكن تصل للخادم المحلي أصلًا. أضفت `network_security_config` في `src/debug` فقط، يسمح بـ `10.0.2.2` و`localhost`. نسخة release بدون تغيير (HTTPS فقط).

## جدول المكونات والشاشات: قبل (Compose) ← بعد (XML) ← حالة اللقطة

### الشاشات (35 شاشة، 233 لقطة)

| المجموعة | الشاشة | قبل (Compose) | بعد (XML View / Fragment / layout) | اللقطات |
|---|---|---|---|---|
| 3a | SCR-C10 | `CustomerLoginScreen` | `CustomerLoginScreen` · `CustomerLoginFragment` · `screen_c10_login.xml` | 8/8 مطابق (أعلى فرق 0.02٪) |
| 3a | SCR-C11 | `CustomerRegisterScreen` | `CustomerRegisterScreen` · `CustomerRegisterFragment` · `screen_c11_register.xml` | 7/7 مطابق (أعلى فرق 0.19٪) |
| 3a | SCR-C12 | `CustomerEmailVerificationScreen` | `CustomerEmailVerificationScreen` · `CustomerEmailVerificationFragment` · `screen_c12_verify.xml` | 7/7 مطابق (أعلى فرق 0.01٪) |
| 3a | SCR-C13 | `CustomerPasswordRecoveryScreen` | `CustomerPasswordRecoveryScreen` · `CustomerPasswordRecoveryFragment` · `screen_c13_recovery.xml` | 6/6 مطابق (أعلى فرق 0.01٪) |
| 3b | SCR-C01 | `C01HomeScreen` | `C01HomeView` · `C01HomeFragment` · `screen_c01_home.xml` | 5/5 مطابق (أعلى فرق 0.38٪) |
| 3b | SCR-C02 | `C02AccountScreen` | `C02AccountView` · `C02AccountFragment` · `screen_c02_account.xml` | 4/4 مطابق (أعلى فرق 0.02٪) |
| 3b | SCR-C03 | `C03ProblemScreen` | `C03ProblemView` · `C03ProblemFragment` · `screen_c03_problem.xml` | 5/5 مطابق (أعلى فرق 0.03٪) |
| 3b | SCR-C04 | `C04TimingScreen` | `C04TimingView` · `C04TimingFragment` · `screen_c04_timing.xml` | 7/7 مطابق (أعلى فرق 0.02٪) |
| 3b | SCR-C05 | `C05ReviewScreen` | `C05ReviewView` · `C05ReviewFragment` · `screen_c05_review.xml` | 5/5 مطابق (أعلى فرق 0.01٪) |
| 3b | SCR-C06 | `C06OffersScreen` | `C06OffersView` · `C06OffersFragment` · `screen_c06_offers.xml` | 6/6 مطابق (أعلى فرق 0.39٪) |
| 3b | SCR-C07 | `C07ProviderScreen` | `C07ProviderView` · `C07ProviderFragment` · `screen_c07_provider.xml` | 4/4 مطابق (أعلى فرق 0.03٪) |
| 3b | SCR-C08 | `C08OfferDetailsScreen` | `C08OfferDetailsView` · `C08OfferDetailsFragment` · `screen_c08_offer_details.xml` | 5/5 مطابق (أعلى فرق 0.02٪) |
| 3c | SCR-C14 | `C14AddressesScreen` | `C14AddressesView` · `C14AddressesFragment` · `screen_c14_addresses.xml` | 6/6 مطابق (أعلى فرق 0.02٪) |
| 3c | SCR-C15 | `C15AddressFormScreen` | `C15AddressFormView` · `C15AddressFormFragment` · `screen_c15_address_form.xml` | 8/8 مطابق (أعلى فرق 0.06٪) |
| 3c | SCR-C16 | `C16SlotPickerScreen` | `C16SlotPickerView` · `C16SlotPickerFragment` · `screen_c16_slots.xml` | 6/6 مطابق (أعلى فرق 0.03٪) |
| 3c | SCR-C17 | `C17MediaScreen` | `C17MediaView` · `C17MediaFragment` · `screen_c17_media.xml` | 8/8 مطابق (أعلى فرق 0.03٪) |
| 4a | SCR-C09 | `C09TrackingScreen` | `C09TrackingView` · `C09TrackingFragment` · `screen_c09_tracking.xml` | 11/11 مطابق (أعلى فرق 0.04٪) |
| 4a | SCR-C20 | `C20CancellationScreen` | `C20CancellationView` · `C20CancellationFragment` · `screen_c20_cancellation.xml` | 5/5 مطابق (أعلى فرق 0.03٪) |
| 4a | SCR-C30 | `C30ExecutionQuoteScreen` | `C30ExecutionQuoteView` · `C30ExecutionQuoteFragment` · `screen_c30_proposal.xml` (مشترك بين C30 وC31، كما في Compose) | 6/6 مطابق (أعلى فرق 0.01٪) |
| 4a | SCR-C31 | `C31AdditionalCostScreen` | `C31AdditionalCostView` · `C31AdditionalCostFragment` · `screen_c30_proposal.xml` | 7/7 مطابق (أعلى فرق 0.01٪) |
| 4b | SCR-C21 | `C21PaymentSummaryScreen` | `C21PaymentSummaryView` · `C21PaymentSummaryFragment` · `screen_c21_payment_summary.xml` | 6/6 مطابق (أعلى فرق 0.01٪) |
| 4b | SCR-C22 | `C22ElectronicPaymentScreen` | `C22ElectronicPaymentView` · `C22ElectronicPaymentFragment` · `screen_c22_electronic_payment.xml` | 10/10 مطابق (أعلى فرق 0.02٪) |
| 4b | SCR-C23 | `C23CompletionScreen` | `C23CompletionView` · `C23CompletionFragment` · `screen_c23_completion.xml` | 5/5 مطابق (أعلى فرق 0.01٪) |
| 4b | SCR-C24 | `C24RatingScreen` | `C24RatingView` · `C24RatingFragment` · `screen_c24_rating.xml` | 10/10 مطابق (أعلى فرق 0.12٪) |
| 4c | SCR-C18 | `C18MessagesScreen` | `C18MessagesView` · `C18MessagesFragment` · `screen_c18_messages.xml` | 6/6 مطابق (أعلى فرق 0.14٪) |
| 4c | SCR-C19 | `C19ChatScreen` | `C19ChatView` · `C19ChatFragment` · `screen_c19_chat.xml` | 8/8 مطابق (أعلى فرق 0.04٪) |
| 4c | SCR-C25 | `C25OrdersScreen` | `C25OrdersView` · `C25OrdersFragment` · `screen_c25_orders.xml` | 7/7 مطابق (أعلى فرق 0.07٪) |
| 4c | SCR-C26 | `C26OrderHistoryScreen` | `C26OrderHistoryView` · `C26OrderHistoryFragment` · `screen_c26_order_history.xml` | 8/8 مطابق (أعلى فرق 0.02٪) |
| 4c | SCR-C27 | `C27DisputeScreen` | `C27DisputeView` · `C27DisputeFragment` · `screen_c27_dispute.xml` | 10/10 مطابق (أعلى فرق 0.02٪) |
| 4c | SCR-C28 | `C28ProviderReportScreen` | `C28ProviderReportView` · `C28ProviderReportFragment` · `screen_c28_provider_report.xml` | 7/7 مطابق (أعلى فرق 0.02٪) |
| 4d | SCR-C29 | `C29HelpScreen` | `C29HelpView` · `C29HelpFragment` · `screen_c29_help.xml` | 6/6 مطابق (أعلى فرق 0.01٪) |
| 4d | SCR-C32 | `C32NotificationsScreen` | `C32NotificationsView` · `C32NotificationsFragment` · `screen_c32_notifications.xml` | 5/5 مطابق (أعلى فرق 0.01٪) |
| 4d | SCR-C33 | `C33AccountSettingsScreen` | `C33AccountSettingsView` · `C33AccountSettingsFragment` · `screen_c33_account_settings.xml` | 9/9 مطابق (أعلى فرق 0.45٪) |
| 4d | SCR-C34 | `C34TermsScreen` | `C34TermsView` · `C34TermsFragment` · `screen_c34_terms.xml` | 4/4 مطابق (أعلى فرق 0.01٪) |
| 4d | SCR-C35 | `C35NoOffersScreen` | `C35NoOffersView` · `C35NoOffersFragment` · `screen_c35_no_offers.xml` | 6/6 مطابق (أعلى فرق 0.01٪) |

### المكونات (42 مكوّنًا — المعرض 16/16 صفحة مطابقة)

| المكوّن (Compose) | بعد (XML) | الـ layout | اللقطة |
|---|---|---|---|
| `AppTopBar` | `AppTopBarView` | `view_app_top_bar.xml` | مطابق — ضمن صفحات المعرض |
| `PrimaryButton` | `PrimaryButtonView` | `view_primary_button.xml` | مطابق — ضمن صفحات المعرض |
| `SecondaryButton` | `SecondaryButtonView` | `view_secondary_button.xml` | مطابق — ضمن صفحات المعرض |
| `LinkButton` | `LinkButtonView` | `view_link_button.xml` | مطابق — ضمن صفحات المعرض |
| `DangerTextButton` | `DangerTextButtonView` | `view_danger_text_button.xml` | مطابق — ضمن صفحات المعرض |
| `IconSquareButton` | `IconSquareButtonView` | `view_icon_square_button.xml` | مطابق — ضمن صفحات المعرض |
| `BottomNav` | `BottomNavView` | `view_bottom_nav.xml` | مطابق — ضمن صفحات المعرض |
| `MenuRow` | `MenuRowView` | `view_menu_row.xml` | مطابق — ضمن صفحات المعرض |
| `DrawerMenu` | `DrawerMenuView` | `view_drawer_menu.xml` | مطابق — ضمن صفحات المعرض |
| `StepIndicator` | `StepIndicatorView` | `view_step_indicator.xml` | مطابق — ضمن صفحات المعرض |
| `SelectableTile` | `SelectableTileView` | `view_selectable_tile.xml` | مطابق — ضمن صفحات المعرض |
| `SelectableChip` | `SelectableChipView` | `view_selectable_chip.xml` | مطابق — ضمن صفحات المعرض |
| `RadioCard` | `RadioCardView` | `view_radio_card.xml` | مطابق — ضمن صفحات المعرض |
| `CheckRow` | `CheckRowView` | `view_check_row.xml` | مطابق — ضمن صفحات المعرض |
| `AppTextField` | `AppTextFieldView` | `view_app_text_field.xml` | مطابق — ضمن صفحات المعرض |
| `SecureTextField` | `SecureTextFieldView` | `view_secure_text_field.xml` | مطابق — ضمن صفحات المعرض |
| `AmountField` | `AmountFieldView` | `view_amount_field.xml` | مطابق — ضمن صفحات المعرض |
| `TextAreaField` | `TextAreaFieldView` | `view_text_area_field.xml` | مطابق — ضمن صفحات المعرض |
| `SummaryCard` | `SummaryCardView` | `view_summary_card.xml` | مطابق — ضمن صفحات المعرض |
| `Badge` | `BadgeView` | `view_badge.xml` | مطابق — ضمن صفحات المعرض |
| `ProviderHeader` | `ProviderHeaderView` | `view_provider_header.xml` | مطابق — ضمن صفحات المعرض |
| `OfferCard` | `OfferCardView` | `view_offer_card.xml` | مطابق — ضمن صفحات المعرض |
| `SortChips` | `SortChipsView` | `view_sort_chips.xml` | مطابق — ضمن صفحات المعرض |
| `StatRow` | `StatRowView` | `view_stat_row.xml` | مطابق — ضمن صفحات المعرض |
| `RatingBars` | `RatingBarsView` | `view_rating_bar_row.xml` | مطابق — ضمن صفحات المعرض |
| `ReviewCard` | `ReviewCardView` | `view_review_card.xml` | مطابق — ضمن صفحات المعرض |
| `StickyActionBar` | `StickyActionBarView` | `view_sticky_action_bar.xml` | مطابق — ضمن صفحات المعرض |
| `InfoBanner` | `InfoBannerView` | `view_info_banner.xml` | مطابق — ضمن صفحات المعرض |
| `WarningBox` | `WarningBoxView` | `view_warning_box.xml` | مطابق — ضمن صفحات المعرض |
| `OfflineBanner` | `OfflineBannerView` | `view_offline_banner.xml` | مطابق — ضمن صفحات المعرض |
| `EtaCard` | `EtaCardView` | `view_eta_card.xml` | مطابق — ضمن صفحات المعرض |
| `MapCard` | `MapCardView` | `view_map_card.xml` | مطابق — ضمن صفحات المعرض |
| `StatusStepper` | `StatusStepperView` | `view_status_step.xml` | مطابق — ضمن صفحات المعرض |
| `OrderCard` | `OrderCardView` | `view_order_card.xml` | مطابق — ضمن صفحات المعرض |
| `AppBottomSheet` | `AppBottomSheetView` | `view_app_bottom_sheet.xml` | مطابق — ضمن صفحات المعرض |
| `EmptyState` | `EmptyStateView` | `view_empty_state.xml` | مطابق — ضمن صفحات المعرض |
| `ErrorState` | `ErrorStateView` | `view_error_state.xml` | مطابق — ضمن صفحات المعرض |
| `LoadingSkeleton` | `LoadingSkeletonView` | `view_loading_skeleton.xml` | مطابق — ضمن صفحات المعرض |
| `Countdown` | `CountdownView` | `view_countdown.xml` | مطابق — ضمن صفحات المعرض |
| `ChatBubble` | `ChatBubbleView` | `view_chat_bubble.xml` | مطابق — ضمن صفحات المعرض |
| `ChatInput` | `ChatInputView` | `view_chat_input.xml` | مطابق — ضمن صفحات المعرض |
| `MediaThumb` | `MediaThumbView` | `view_media_thumb.xml` | مطابق — ضمن صفحات المعرض |

## إثبات «مفيش Compose» (أ–هـ)

الأمر: `scripts/check-no-compose.sh` (يبني release APK وAAB ثم يفحص). الخرج الفعلي:

```
== a. Gradle
PASS  no org.jetbrains.kotlin.plugin.compose / org.jetbrains.compose plugin
PASS  no buildFeatures compose = true, no composeOptions
PASS  no compose dependency in any module or version catalog
      :app: 126 configurations resolved
      :core:design: 124 configurations resolved
PASS  ./gradlew <module>:dependencies (every configuration, incl. test / androidTest): no androidx.compose, no org.jetbrains.compose

== b. Source
PASS  no @Composable, setContent {, ComposeView, import androidx.compose, createComposeRule, @Preview in src/ (main, test, androidTest)

== c. Release APK and AAB
      app-release-unsigned.apk: 8340 classes
PASS  app-release-unsigned.apk: 0 classes under androidx.compose / org.jetbrains.compose
      app-release.aab: 8340 classes
PASS  app-release.aab: 0 classes under androidx.compose / org.jetbrains.compose

== d. No cross-platform UI framework
PASS  no Flutter (pubspec.yaml, io.flutter, flutter plugin)
PASS  no React Native (com.facebook.react, react-native)
PASS  no Compose Multiplatform / Kotlin Multiplatform (org.jetbrains.compose, kotlin("multiplatform"), kotlin.multiplatform)

== Result
OK: no Compose and no cross-platform UI framework in the Android app.
```

| البند | ما يُفحص | النتيجة |
|---|---|---|
| أ | ملفات Gradle كلها: لا plugin `org.jetbrains.kotlin.plugin.compose`، لا `compose = true` ولا `composeOptions`، لا dependency فيها compose (bom، activity-compose، lifecycle-*-compose، navigation-compose، hilt-navigation-compose، coil-compose، maps-compose، accompanist، material3، ui-tooling). ثم `./gradlew :app:dependencies` (126 configuration) و`:core:design:dependencies` (124)، بما فيها test وandroidTest: ولا سطر فيه `androidx.compose` أو `org.jetbrains.compose` | ✅ |
| ب | `src/` في كل module (main وtest وandroidTest): صفر لـ `@Composable` و`setContent {` و`ComposeView` و`import androidx.compose` و`createComposeRule` و`@Preview` | ✅ |
| ج | `app-release-unsigned.apk` و`app-release.aab`: جرد كل الكلاسات بـ `dexdump` (8338 كلاس في كلٍّ منهما): صفر تحت `androidx.compose` و`org.jetbrains.compose`. `apkanalyzer` غير مثبّت في الـ SDK، فاستخدمت جرد الكلاسات كما يسمح البند | ✅ |
| د | لا Flutter، ولا React Native، ولا Compose Multiplatform، ولا Kotlin Multiplatform | ✅ |
| هـ | `scripts/check-no-compose.sh` يشغّل أ وب وج ويفشل عند أي أثر، ويعمل في كل تعديل عبر `.github/workflows/android-no-compose.yml`. **جرّبته عمدًا** بزرع `import androidx.compose...` وسطر dependency: فشل بـ exit 1 وحدّد الملفين، ثم أزلت الزرع | ✅ |

ويبقى أيضًا: `php tools/lint-design --no-compose` ✅.

**ما حُذف:** 30 ملف شاشة Compose (`C01…C35Screen` و`ProposalDecisionScreen`)، و`CustomerJourneyFlow` و`CustomerScreenCommon` و`CustomerAuthScreens`، و`BzrComponents` و`BzrTheme` و`ComponentGallery`، و`DesignTokens.kt` المولّد (Color/dp/sp)، واختبارات لقطات Compose الثلاثة و249 لقطة ذهبية لها. الأهداف المعتمدة محفوظة قبل الحذف. `BzrFormat` بقي لأنه Kotlin عادي بدون Compose.

**بدل معرض Compose التفاعلي:** `ComponentGalleryApp` كان فيه زرّا «السابق/التالي» للتنقل بين الصفحات، زي `GalleryPage` في iOS. اتعمل بدله بـ XML: `ComponentGalleryAppView` + `view_gallery_app.xml`، بنفس مفتاحي النص `gallery.back`/`gallery.next`، عشان يفضل تطابق المفاتيح مع iOS في `check-parity`.

## اختبارات المنطق المشتركة

`./gradlew testDebugUnitTest`: **كلها ناجحة، بلا حالة ناقصة.** نفس ملفات الاختبار ونفس الحالات قبل التحويل وبعده (لا فرق في `git diff`).

| الاختبار | ما يغطيه | قبل | بعد |
|---|---|---|---|
| `AuthSharedFixtureTest` | كل حالات `SCR-C10..C13` (29 حالة) | ✅ | ✅ |
| `CustomerSharedFixtureTest` | كل حالات `SCR-C01..C09` و`C14..C35` (206 حالات) | ✅ | ✅ |
| `SharedCoreFixtureTest` (اختباران) | التنسيق المشترك: الأرقام والمبالغ والعد التنازلي والوقت والتاريخ | ✅ | ✅ |
| لقطات | قبل: Compose (المعرض والعميل والدخول). بعد: `ComponentGalleryViewsSnapshotTest` و`CustomerAuthViewsSnapshotTest` و`CustomerJourneyViewsSnapshotTest` على نفس الـ fixtures | ✅ | ✅ |

وأيضًا: `assembleDebug` و`verifyPaparazziDebug` و`lintDebug` (أخطاء RTL توقف البناء) و`tools/gen-design --check` و`tools/check-parity` (42 مكوّنًا، 58 شاشة، 471 مفتاح نص): كلها ✅.

## قرارات صغيرة للعلم

- **الأوراق السفلية** (C14 تأكيد الحذف، وC20 تأكيد الإلغاء، وC28 إرسال البلاغ، وC33 حذف الحساب): ثابتة أسفل الشاشة داخل الـ layout، زي Compose بالظبط. `BottomSheetDialogFragment` (`AppBottomSheetFragment`) جاهز في المكتبة، لكن استخدامه كان هيغيّر السلوك (نافذة تُغلق بالسحب) واللقطات، فما استخدمتهوش.
- **C17:** زرار «صورة» و«فيديو» و«تسجيل صوتي» ما كانوش موصولين بحاجة في Compose. `CustomerJourneyFlow` كان بيمرّر الحذف والإنهاء بس. سبتهم زي ما هم، لأن توصيلهم ميزة جديدة.
- **الخرائط:** `MapCard` بطاقة ثابتة في Compose وفي XML، ومفيش خريطة حية في أي شاشة حاليًا. فمحتاجناش `SupportMapFragment`، ومفيش `maps-compose`.
- **الصور:** مفيش تحميل صور من الشبكة في الشاشات الحالية، فما اتضافتش مكتبة صور.

## الملفات المشتركة — [`SHARED-CHANGES.md`](SHARED-CHANGES.md)

- `tools/gen-design`: شلت سطر توليد `DesignTokens.kt` (جزء أندرويد الخاص بـ Compose)، ولا حاجة تانية. وبناءً عليه اتحدّث `design/generated/manifest.json` بسطر واحد، وده ملف مولّد.
- `design/parity.json`: عمود أندرويد بس للشاشات C01–C35 (`file` و`symbol` بقوا بيشاوروا على الـ XML Views). أعمدة iOS متلمستش.
- **مطلوب منك:** حذف `design/reference/android-compose/screens/` (212 ملفًا كتبتهم بالغلط، والحذف اتمنع عليّ).
- **مطلوب قرارك:** تحديث fixtures دمياط، وتوثيق انحراف الحقول/الشرائح في `docs/43`.

## الأسئلة المفتوحة

1. **الدمج:** الفرع فيه `androidapp/` كامل، بس `design/` و`tools/gen-design` و`tools/check-parity` و`tools/lint-design` و`iosapp/` مش في `main` ولا في الفرع (لسه untracked في المجلد الرئيسي). فـ CI على الفرع لوحده مش هيلاقي المدخلات المشتركة لحد ما تتعمل commit في `main`. تحب مين يعمل commit للملفات المشتركة، وإمتى ندمج الفرع؟
2. **fixtures دمياط** (من خطة الخطوة 3): ملف مشترك، فما اتعدّلش. لو اتحدّث، لقطات الشاشات المتأثرة هتتغير في المنصتين، وهتحتاج إعادة اعتماد مرة واحدة.
3. **الـ worktree:** `/home/tro/bzr-android-xml`. المجلد الرئيسي فيه روابط `androidapp` و`reports/xml-conversion` وأدوات أندرويد الثلاث بتشاور عليه. تحب يفضل كده لحد الدمج؟
4. **C17:** نوصل أزرار اختيار الوسائط؟ ميزة جديدة، فمستنية قرارك.
