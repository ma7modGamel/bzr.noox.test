# Remaining risks and final acceptance checklist

Status: implementation and available local checks completed; **mobile production acceptance remains open**. No deployment or legal publication was performed.

## Release blockers

1. **Legal approval:** no approved English Terms, Privacy, Cancellation or FAQ version was found. Complete translated drafts are in `legal-drafts/`, marked Pending Legal Approval. The connected database also contains only Arabic drafts requiring review. Preserve any approved Arabic wording in another deployment; reconcile its version/hash before approving a translation. Publish only authorized approved versions through the existing super-admin workflow. The API currently withholds unavailable/unpublished English legal content and English FAQ fallback rather than returning Arabic or drafts.
2. **iOS native verification:** this host is Linux. Swift Core builds/tests pass, but UIKit/SwiftUI compilation, string-catalog plural compilation, legal text rendering, Dynamic Type, VoiceOver, app-language Settings behavior and simulator/device screenshots require the Mac validation batch. `iosapp/BzrAppTests/LegalLocalizationTests.swift` supplies legal rendering/direction and native-plural tests for that batch. Run the test target once with Arabic app language and once with English, in addition to the existing gallery tests. Apple's native plural/string-catalog behavior is documented in [Localizing strings that contain plurals](https://developer.apple.com/documentation/xcode/localizing-strings-that-contain-plurals) and [String catalogs](https://developer.apple.com/documentation/xcode/localizing-and-varying-text-with-a-string-catalog).
3. **Interactive locale-switch matrix:** test fresh/returning sessions, logged out/in, request wizard, request details, notifications, restart/process restoration, slow concurrent requests, foreground refresh and back stacks. Header negotiation, cached service reuse and history invalidation have automated coverage; the complete device matrix has not been executed. Rapid switches with concurrent mutations/polling need particular attention. Language changes preserve customer wizard data, while some other provider/customer routes intentionally reload from their canonical entry or current order; exact route/draft retention outside that wizard is not certified.
4. **Live notification delivery:** payload generation, per-device locale and retry/deletion paths are tested with the sender mocked. Foreground/background FCM/APNs display, OS notification history and real deep-link routing still require devices and configured delivery credentials. Already-delivered OS notifications cannot be retroactively translated; new payloads use the synchronized device locale.
5. **Full visual release review:** Android layout and new legal-image checks pass. Original defect screenshots were described in the request but image files were not attached here. Existing approved golden collections and live server-driven English screen states require review; their old baselines were not automatically replaced. Static fixture data is not proof of zero leakage in every rendered state.

## Environment and content considerations

- The normal local host's web server uses a PHP runtime that Composer rejects (dependencies require PHP ≥8.4.1). The installed CLI is PHP 8.4.23. Live locale checks used a temporary PHP 8.4 server; persistent web-server configuration was not changed. Correct the local/server runtime before relying on that host.
- New/edited managed service or support labels without English content fail explicitly with 409. Administrators must supply translations before enabling such content for an English release. Geographic proper names and genuine user/admin-authored prose remain as written.
- Pre-existing Arabic-only notification history uses an explicit unavailable-translation message/flag in English, preserving the original database content and deep link. Historical detail translations are not fabricated.
- Account-level email language follows the last synchronized preference; push language remains per device. Offline switches cannot update the server until a successful session/device preference synchronization; verify retry behavior on next launch.
- The source legal drafts contain factual claims such as password storage and retention periods. Translation preserves those claims; legal/product reviewers must validate the source against deployed behavior before approval.
- The checkout already contained extensive unrelated changes and snapshot deletions. They were retained; this remediation was not committed or deployed.

## Acceptance checklist

| Requirement | Current evidence / status |
| --- | --- |
| English controlled UI has no unintended Arabic | Catalog/API gates and live catalog/config pass; complete live-device screen proof pending |
| Arabic controlled UI has no unintended English | Native catalog script exceptions are explicit; complete live-device screen proof pending |
| Correct RTL/LTR layout | Android eight-profile checks and legal image review pass; iOS/device and animation/accessibility checks pending |
| Dynamic data follows active locale | API contracts, launch catalog, managed-content failure and live responses pass; all deployed managed records need readiness review |
| Notifications follow recipient language | Structured events/history/device payload tests pass; real foreground/background delivery pending |
| Legal documents render safely | Backend sanitizer and Android legal rendering pass; iOS runtime and legal approvals pending |
| Switching works without stale translations | HTTP/current-service/history tests pass and reload implementation in place; complete interactive matrix pending |
| Missing translations are detected | Key/empty/duplicate/placeholder/script/plural/source gates and explicit backend failure tests pass |
| Existing business workflows remain functional | Full backend suite passes; Android app/design suite results recorded separately; full mobile end-to-end flows pending |
| Available tests and visual checks pass | See exact final results in `test-results.md`; full historic golden and iOS checks are not represented as passing |

Do not mark production localization accepted solely from catalog parity or these implementation changes. Legal approvals and the outstanding native/device checks are explicit release gates.
