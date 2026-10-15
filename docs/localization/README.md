# Bremo Arabic/English localization remediation

Audit and local verification: 2026-10-11. Implementation is in the shared checkout; no deployment, commit or legal publication was performed. Existing Arabic legal wording remains unchanged.

The changes connect native application language to API responses, managed content, validation, notifications and public pages; add complete bilingual controlled catalogs and native plurals; repair legal HTML rendering; and invalidate stale localized state. Missing managed translations fail explicitly. Personal names and genuine user content are preserved.

Available verification passed: **293 backend tests / 4,407 assertions**, **127 Android app/design tests**, **40 Swift Core tests**, the Android debug build, **8 new legal-image comparisons**, and all three catalog/source gates. The final copy-only backend follow-up passed **39 tests / 2,375 assertions**; those tests are a subset, not additional coverage to add to the full-suite total. See exact commands and limits below.

**Production acceptance remains open.** No approved English legal documents were found. Four complete English drafts are marked **Pending Legal Approval**, outside serving/publication paths. iOS UI compilation and runtime checks, real-device language switching, live foreground/background push delivery and the complete visual review remain release gates. The normal local web host also needs a PHP runtime compatible with the installed dependencies; real HTTP checks used a temporary PHP 8.4 server.

## Deliverables

1. [Localization audit](audit.md): paths, defects, causes, severity, corrections and evidence.
2. [Translation coverage](coverage.md): catalog counts, script checks, exceptions and runtime limits.
3. [RTL/LTR compliance](rtl-ltr.md): direction, metadata, legal typography and visual coverage.
4. [Backend localization](backend.md): API contract, schema, managed content, compatibility and rollback.
5. [Automated test results](test-results.md): exact counts, commands and unavailable checks.
6. [Remaining risks](risks-acceptance.md): legal, native, device and environment blockers.
7. [Final acceptance checklist](risks-acceptance.md#acceptance-checklist): evidence and outstanding release gates.

Supporting artifacts: [screen/component/API inventory](inventory.md), [bilingual terminology glossary](glossary.md), [keys without direct static references](static-reference-gaps.md), and [machine-readable test summary](test-summary.json).

Legal review drafts: [Terms](legal-drafts/terms.md), [Privacy](legal-drafts/privacy.md), [Cancellation](legal-drafts/cancellation-policy.md), [FAQ](legal-drafts/faq.md). Each includes the exact Arabic source, its SHA-256 hash and a complete English draft preserving its structure. Drafts must receive legal approval before entering the publication workflow.
