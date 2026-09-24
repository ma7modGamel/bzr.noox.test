# تقرير الدفعة 3أ — C10–C13

## بوابة الاعتماد

- صفحة المقارنة: `reports/batch-3a/auth/comparison.html`
- المطلوب: اعتماد 28 لقطة Android (كل الحالات المرئية؛ حالة انتهاء الجلسة تعيد إلى C10 الموجودة) قبل بدء 3ب.
- iOS: الكود مكتوب بالتوازي، لكن الرسم الفعلي والقياس والخط ولقطات SnapshotTesting مؤجلة إلى دفعة «تحقق iOS» النهائية وفق DEC-045.

## التطابق

| الشاشة | المواصفة | Compose | SwiftUI | fixture | الحالات في Android | الحالات في Swift Core |
|---|---|---|---|---|---:|---:|
| SCR-C10 | `docs/screens/SCR-C10.md` | `CustomerLoginScreen` | `CustomerLoginScreen` | `design/fixtures/SCR-C10/cases.json` | 8 | 8 |
| SCR-C11 | `docs/screens/SCR-C11.md` | `CustomerRegisterScreen` | `CustomerRegisterScreen` | `design/fixtures/SCR-C11/cases.json` | 7 | 7 |
| SCR-C12 | `docs/screens/SCR-C12.md` | `CustomerEmailVerificationScreen` | `CustomerEmailVerificationScreen` | `design/fixtures/SCR-C12/cases.json` | 8 | 8 |
| SCR-C13 | `docs/screens/SCR-C13.md` | `CustomerPasswordRecoveryScreen` | `CustomerPasswordRecoveryScreen` | `design/fixtures/SCR-C13/cases.json` | 6 | 6 |
| **الإجمالي** |  |  |  |  | **29** | **29** |

## الإضافات

- مكوّنان جديدان في مكتبة المنصتين وتعرضهما شاشات 3أ: `LinkButton` و`SecureTextField`. لم يتغير معرض الدفعة 1 أو مراجعه المعتمدة.
- لا رموز أو أيقونات أو قيم تصميم جديدة.
- أضيفت مفاتيح النصوص `auth.*` للدخول والتسجيل والتفعيل والاستعادة والتحقق والأخطاء؛ كلها في `design/strings.ar.json` وتولد للمنصتين.
- رمز Sanctum مشفّر بـ Android Keystore، ومحفوظ في iOS Keychain.
- اكتملت endpoints التفعيل وإعادة الإرسال وطلب/تنفيذ استعادة كلمة المرور، مع عدم كشف وجود الحساب وإلغاء رموز Sanctum بعد تغيير كلمة المرور. اكتمال رابط البريد نفسه متوقف على W02 كما هو موضح أدناه.

## الأسئلة المفتوحة

- لا توجد أسئلة سلوكية مفتوحة داخل واجهات C10–C13 نفسها.
- صفحة الويب `SCR-W02` التي يستقبل عليها المستخدم رابط إعادة تعيين كلمة المرور نُقلت بقرار المالك إلى دفعة ويب لاحقة. طلب الرابط نفسه مربوط ومختبر، لكن رحلة الرابط لا تعد مكتملة إنتاجيًا قبل تلك الدفعة.
- OD-03 (اختيار مزود البريد الإنتاجي) ما زال قرار نشر؛ التنفيذ يستخدم Mailer لارافيل القابل للضبط من البيئة.

## غير المتحقق منه على iOS

- لم يُبنَ تطبيق iOS بـ Xcode ولم تُلتقط لقطات SwiftUI، تنفيذًا لقرار DEC-045.
- تحقق Linux يشمل بناء واختبارات Swift Core، و29 حالة مشتركة، وswift-format وSwiftLint وفحص القيم البصرية والتطابق. لا يثبت Linux الرسم الفعلي أو تسجيل Cairo أو سلوك Keychain على جهاز iOS؛ تُراجع في الدفعة النهائية.
