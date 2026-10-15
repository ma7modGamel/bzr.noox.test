# Translation coverage report

Coverage date: 2026-10-11. Translation-entry coverage is complete for the audited controlled catalogs. **Complete live rendered coverage is not yet certified.** Approval and device evidence are separate requirements.

| Dataset | Arabic | English | Result and limitation |
| --- | --- | --- | --- |
| Shared native UI source keys | 1,057 / 1,057 | 1,057 / 1,057 | 100% populated, matching keys and placeholders; no empty values |
| Native plural families | 22 / 22 | 22 / 22 | All 132 Arabic forms and 44 English forms present; platform resource generation and Arabic/English quantity tests pass on Android; iOS runtime pending |
| Backend semantic message/enum keys | 459 / 459 | 459 / 459 | 100% populated with matching placeholders; English controlled catalog has no Arabic script |
| Known legacy launch-label translations | 104 source fingerprints | 104 English labels | Compatibility translations for exact known source labels; changed/new managed content requires its English field |
| Connected local English catalog response | Original Arabic retained | 8 categories and their problem types returned in English | Real HTTP 200 response contains no Arabic script; proper-name/user-content exceptions are not counted as translation failures |
| Required legal documents | 4 Arabic source documents found | 4 complete English drafts prepared | 0 approved English versions found; connected Arabic sources also remain drafts requiring review. Draft completeness is not approval coverage |
| Shared screen/component inventory | 60 screen entries / 42 components | Same inventory | Includes gallery and 59 customer/provider screens; inventory is not a claim that all interactive states passed device QA |
| Static cross-platform references | 907 shared keys detected | 907 shared keys detected | Dynamic reducer/action/status keys require fixture/runtime coverage; see `static-reference-gaps.md` |

`tools/gen-design` rejects key differences, duplicate/conflicting flat catalog keys, empty values, placeholder mismatches, unexpected Arabic in English, and unexpected Latin words in Arabic. Specific exceptions in `design/localization-exceptions.json` document implementation-component names in the developer gallery, the product brand, example email address and phone-number placeholders. No customer screen/package is exempted from production-source scanning. Tests are excluded because fixture content intentionally includes both languages. Numeric Arabic characters are not mistaken for Arabic prose.

`tools/check-parity` validates screen/component inventories, references to string and plural resources, unknown literal Swift localization keys and prohibited native business-option maps. `tools/lint-design` checks direct user-facing literal call sites, Arabic prose and direction-dependent layout properties. The English detector is a heuristic over UI setters/constructors, not a parser proving that every possible string concatenation is localized. Runtime-generated keys and server-authored content still need the endpoint and device checks.

Genuine names, addresses, biographies, comments, messages and free-text reasons retain their source text. Geographic proper names may remain in Arabic in English mode unless an administrator supplies `name_en`; this is the explicit proper-name exception, not translation fallback for services or reasons. Technical codes/URLs/emails and the Bremo brand retain their representation.

Existing stored Arabic legal bodies and source wording were not edited. Newly translated legal documents are outside application resources and database publication paths, explicitly marked **Pending Legal Approval**.
