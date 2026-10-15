# RTL/LTR compliance report

Arabic uses resource-driven RTL; English uses LTR. Directional behavior is inherited from the existing Android/SwiftUI component systems rather than forced on all text.

| Area | Inspection / correction | Verification |
| --- | --- | --- |
| Android app locale | AppCompat locale persistence and supported locale configuration drive both resources and HTTP language | Build and negotiation/header tests pass; API 26–32 and 33+ OS setting restoration still need emulator/device testing |
| Android layout | Existing shared components use start/end padding, relative drawables and alignment; manifests support RTL | Source gate passes; all shared customer/provider screen layouts measured on eight profiles |
| Navigation icons | Existing back/chevron vectors have `autoMirrored=true`; non-directional symbols retain their meaning | Source inspected; Arabic and English legal images visually inspected with back arrows on the appropriate side |
| iOS layout | Existing leading/trailing SwiftUI layout follows app direction; phone numbers retain LTR | Source inspected; simulator layout/animation and VoiceOver checks pending |
| Legal body | Android native spans retain headings, emphasis, unordered bullets and ordered numbering; iOS native attributed paragraphs retain list style and set base writing direction | Eight Android HTML rendering snapshots pass; iOS tests added but not executed here |
| Legal metadata | Numeric version/date values are directionally isolated; effective day is expressed as ISO date in Africa/Cairo | Arabic screenshot review revealed visual date-order reversal; corrected and new baselines recorded; both native Core date tests pass |
| Public HTML | Legal, account-link, app-link and shared-visit pages emit locale-specific `lang` and `dir`; footer links retain locale | Backend response assertions and public-page regression tests pass |
| Identifiers and contact fields | Existing phone display/input helpers retain LTR where appropriate; public email/footer addresses use `bdi`/LTR; machine codes and links unchanged | Source inspection; mixed-direction real-name/email/phone and assistive-technology device checks pending |
| Prices and dates | Currency labels follow active language, decimal fractions are preserved, user-visible timestamps use selected locale; western digits remain the existing shared-format convention | Exact-decimal/native date tests pass; locale/resource quantity tests pass on Android |
| Language recreation | Customer wizard selections and text remain in view-model fields while display catalogs reload; localized history is discarded; stale-language transport results are rejected | Transport/history tests pass; rapid switching, slow requests and complete navigation-stack behavior require device exercises |

Android profiles exercised: 320×568, 360×800 and 412×915 in Arabic; 375×667, 390×844 and 430×932 in English; plus 320×568 at 2× font scale in each language. The measured screen set spans C01–C38 and P01–P21. The new HTML fixture includes both unordered and ordered lists; version/date and content direction are visible in the new regression images.

Layout assertions detect text-height clipping. They do not certify every horizontal glyph edge, touch target, keyboard interaction, accessibility announcement, scroll position or animation. Some pre-existing snapshot fixtures contain Arabic system labels even under English test resources; the API contract/live-response tests establish translated system labels, while fixture screenshots establish layout geometry. Full live-screen language leakage checks remain a release gate.

Existing approved screenshot collections were not rewritten to conceal changes. Only the eight new legal-rendering images were recorded and compared against their new baselines. The complete historic golden-image comparison is not claimed to pass.
