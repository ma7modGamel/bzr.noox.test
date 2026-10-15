# Automated localization test results

Verification date: 2026-10-11. Host: Linux; PHP CLI 8.4.23, Laravel 13.33, PHPUnit 12.5.35. Counts describe executed tests, not the number of screens or catalog entries.

## Executed checks

| Check | Command / scope | Exact result |
| --- | --- | --- |
| Full backend regression suite | `php artisan test --compact` | **293 tests passed; 4,407 assertions; 64.559 seconds** |
| Final backend follow-up after English copy normalization | `php artisan test --compact tests/Feature/LocalizationTest.php tests/Feature/Architecture/DesignTokensTest.php` | **39 tests passed; 2,375 assertions; 39.211 seconds**; overlaps the full suite |
| Android debug application and complete app/design JVM suites | From `androidapp`: `./gradlew :app:assembleDebug :app:testDebugUnitTest :core:design:testDebugUnitTest --offline` | **BUILD SUCCESSFUL; 120 app tests + 7 design tests = 127 passed; 0 failures/errors/skips** |
| New legal rendering golden comparisons | From `androidapp`: `./gradlew :app:verifyPaparazziDebug --tests '*MobileLayoutQualityTest.legal_html_renders_formatted_text_in_the_selected_direction*' --offline` | **8 comparisons passed**, one for each Arabic/English viewport/font-scale profile; overlaps the app suite |
| Swift Core package | `swift test --package-path iosapp/Core --scratch-path /tmp/bremo-localization-swift-2` | **40 XCTest cases passed; 0 failures; 0.447 seconds test execution** |
| Generated bilingual resources | `php tools/gen-design --check` | Pass; generated outputs current |
| Cross-platform keys and inventories | `php tools/check-parity` | Pass; **42 components, 60 screen entries, 907 referenced shared keys** |
| Hardcoded copy and layout source gate | `php tools/lint-design` | Pass; no violations detected by the source heuristics |
| PHP formatting | `vendor/bin/pint --dirty --format agent` | Pass; final backend catalog formatting applied |
| Swift formatting | `swift-format` format/lint over the edited Swift localization files | Pass; full SwiftLint/application compilation was not run |

The backend localization tests cover header negotiation and preference fallback; both material-supply option languages and unchanged codes; launch catalog translations; explicit missing-translation errors; preserved user names; validation fields; unpublished legal versions; HTML structure and unsafe-content removal; public page direction; English FAQ withholding; recipient email/device language; current-language notification history; safe handling of old Arabic-only history; six Arabic plural categories; missing keys/parameters; error-response language; server-detail redaction; and cross-language idempotent replay without a second mutation. Existing authentication, order, payment, settlement, support, admin and API suites provide business regression coverage.

Android's 127 include **40 parameterized layout tests** across eight profiles, **2 active-language HTTP tests**, **5 customer screen-history tests**, native shared formatting/plural assertions and existing auth/customer/provider/transport regression tests. Paparazzi runs on the JVM, not an Android emulator. Ordinary snapshot recording tests do not establish approval against every historic golden. Only the eight new legal snapshots were explicitly verified against their new baselines.

Swift Core's 40 include supported-language negotiation, outgoing headers, precise monetary fractions, Cairo legal effective dates, transport/session handling and shared customer/provider/auth fixtures. They compile Foundation/Core code on Linux. They do not compile SwiftUI/UIKit or Apple's localized string catalogs.

## Real local HTTP and database checks

A temporary PHP 8.4 server exercised the connected local application database, independently of test factories:

| Request | Result |
| --- | --- |
| English catalog | HTTP 200; eight categories and their problem names returned without Arabic script |
| Arabic configuration | HTTP 200; original Arabic material-supply labels retained |
| English configuration | HTTP 200; English material-supply labels and unchanged machine codes |

Responses included the requested `Content-Language` and `Vary: Accept-Language`. The regular local host returned Composer's PHP platform requirement error; it was not represented as a passing application endpoint. Persistent server configuration was not changed.

The additive localization migration was applied **once to the local application database**, after confirming `APP_ENV=local` and reviewing the rollback plan: `2026_10_11_192422_add_localization_fields`, 539.54 ms. All four Arabic body hashes and draft/review flags remained unchanged. Automated tests independently exercised the schema in the isolated test database. No production migration or data publication was performed.

| Arabic source | Unchanged SHA-256 |
| --- | --- |
| Terms | `d832139c3c9931ab1eb9f9988802f8a3835bc513408e16d2c71cfd121529de94` |
| Privacy | `9858d1878553195dcbf2f02d52a60a8f0df512d4b08c2c9059ca8c303a790a80` |
| Cancellation | `c1fb395804d303d9ba78695ca818337a743228ce9ae1af7f8640e296e3fa4f99` |
| FAQ | `6f94189cb1f58cf65f09d36080f1b74aeca27b578e3de94fe6b9800947ff7ac6` |

## Findings resolved during verification

- Arabic legal image review exposed reversed visual date ordering; directional isolation and an unambiguous Cairo ISO date corrected it.
- Legal test metadata previously passed an integer to a text-only reducer; the fixture now matches the actual string payload and displays version 1.
- Ordered legal lists needed a native Android list handler because the default HTML conversion omitted numbering.
- Full regression testing exposed additional system labels selected without their English fields; resource and eager-loading selections were corrected.
- A final Android rerun initially could not create Gradle locks in the sandbox's read-only user cache; the permitted rerun uses the existing Gradle installation. This was an environment failure, not a passing test run.

No unresolved executed-test failures remain. Unavailable release checks below are not counted as passes.

## Unexecuted checks

- iOS app build, UIKit/SwiftUI legal rendering, native plural resources, Dynamic Type, VoiceOver and gallery images require macOS/Xcode. Two regression methods in `iosapp/BzrAppTests/LegalLocalizationTests.swift` are prepared for Arabic and English app-language runs.
- Fresh install, OS app-language persistence, process death, complete back stacks, all wizard/provider states, slow competing requests and offline preference synchronization require the interactive device matrix.
- FCM/APNs foreground/background display and real notification deep links require delivery credentials and devices; backend push tests use a mocked sender.
- Complete historic Android golden verification and live-data screen review were not performed; the pre-existing checkout includes unrelated snapshot modifications/deletions.
- Legal approval is unavailable for the translated drafts. Tests prove drafts are withheld; they cannot establish legal approval.

See [remaining risks and acceptance](risks-acceptance.md) for the release decision. Populated catalogs and these test totals do not certify every rendered state.
