# Backend localization and migration report

Status: implemented and tested locally; no production deployment or legal publication.

## Locale contract

`app/Http/Middleware/ResolveLocale.php` negotiates supported Arabic/English regional tags from `Accept-Language`. Without that header it uses the authenticated Sanctum user's preference, then Arabic. Unsupported-only preferences select Arabic, the existing source language. `Content-Language` and `Vary: Accept-Language` accompany normal responses. The application locale is restored after each request. Public legal, account-link, app-link and visit-sharing pages also accept `?locale=ar|en`; API query parameters cannot override the header.

Android's resource/AppCompat language and iOS's supported system app language supply the HTTP header. `PATCH /api/v1/me` accepts and validates `locale`; login, native session startup, and device registration synchronize preferences. Each push token has its own locale. Email uses the user's preference, independently of the sender or queue process locale. The last device to synchronize determines the account-level email preference; push remains per device.

`app/Support/Localization.php` centralizes semantic message keys, enums, administrator-managed fields, and public design strings. Missing keys/parameters fail explicitly. English managed content uses `name_en` / `label_en`, or an exact source fingerprint from the reviewed launch catalog in `config/localization.php`. A changed/new managed label without translation returns `409 LOCALIZATION_UNAVAILABLE`; it does not return Arabic as English. Geographic names are proper-name exceptions. Personal names, request descriptions, chat messages, reviews, address labels, biographies and free-text administrator reasons remain as authored.

Stable business codes, IDs, action codes, error envelopes, timestamps and numeric amounts keep their existing contracts. Translations affect display fields. Legal payloads add locale metadata and keep locale in web links. API labels remain authoritative; neither mobile app contains a business-choice translation map.

`EnsureIdempotency` preserves a successful mutation's original cache entry. A replay in another language returns a localized `409 IDEMPOTENCY_LOCALE_MISMATCH`, instructing refresh, without repeating the mutation or returning a cached response in another language. User IDs are resolved with the Sanctum guard before constructing the cache key. Existing cache entries are treated as Arabic. GET/display caches must include locale wherever new caches are introduced.

## Additive schema change and deployment plan

Migration: `database/migrations/2026_10_11_192422_add_localization_fields.php`.

| Table | Added field | Existing data handling |
| --- | --- | --- |
| categories, problem_types, cities, areas | nullable `name_en` | Existing `name` remains untouched |
| support_reasons | nullable `label_en` | Existing `label` remains untouched |
| legal_page_versions | indexed `locale`, default `ar` | Existing body, status, approval and version unchanged |
| users, device_tokens | `locale`, default `ar` | Existing records retain Arabic until preference synchronization |

The Filament catalog, geography, support-reason and legal-version forms expose the corresponding English fields/document locale. Creating an English legal version still creates a draft requiring legal review. No translation is automatically approved. Version numbering and the existing global terms-acceptance mechanism remain unchanged; publishing a Terms version in either language follows the existing super-admin workflow and creates a new acceptance version.

Before deployment, back up these tables and record migration state. Apply this additive migration before deploying code that selects the new fields; then rebuild configuration caches and restart queue workers with the matching catalog. Check both languages and all managed catalog labels. Approve and publish each legal locale separately through the existing authorized admin workflow. **The review drafts in this directory must never be bulk imported as published records.**

Preferred rollback: revert the application release and retain the added columns, preserving translations and preferences. If schema rollback is required, first export every new field and legal translation, stop workers/writes, and run this migration's `down()` only after confirming it is the intended latest migration. `down()` drops new localization fields and therefore loses their data; it never rewrites Arabic bodies or original name fields. Do not roll back unrelated migrations. Restore exported fields if reapplying later.

The isolated test database has been recreated by automated tests; the additive migration was also applied once to the local application database after checking `APP_ENV=local` and reviewing the rollback plan. The four stored Arabic body hashes and draft approval flags were verified unchanged afterwards. Local application migration status and any local application apply are recorded in `test-results.md`. No production data has been changed.

## Notifications and legal content

New notification records contain event/template keys and structured parameters, with legacy Arabic fields retained for older consumers. In-app history renders in the current request language; push renders using the recipient token language at job execution. Authentication and order mail use recipient language. Older Arabic-only history keeps its original database text; English presents an explicitly unavailable historical translation message and a flag, retaining the original deep link. It does not invent historical event details.

Legal versions are selected by locale, published status and effective date. Drafts never enter the mobile/public payloads. `SafeHtml` retains headings, paragraphs, lists, emphasis and safe links while removing scripts, remote embeds, forms, styles, event attributes and unsafe protocols. Stored Arabic bodies are never rewritten by sanitization. Android renders formatted spans and preserves ordered numbering; iOS uses a non-editable, non-scrolling attributed text view with directional paragraphs and Dynamic Type.

Connected database discovery found four Arabic v1 drafts requiring legal review (Terms, Privacy, Cancellation, FAQ), matching `LegalPagesSeeder`; no approved English versions. The legacy `terms_versions` mechanism was also inspected. Separate complete English review drafts include byte-preserved Arabic originals and source SHA-256 hashes. Legal approval remains a release blocker for affected content.
