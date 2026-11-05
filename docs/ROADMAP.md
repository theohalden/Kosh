# Kosh engineering roadmap

The native application lives in `apps/android`; the Tinward marketing site is in `apps/web`. This roadmap covers Android functionality.

This roadmap tracks the implementation on `main` and its import-integrity fixes. It distinguishes existing code from work still required.

## Existing implementation

- [x] Native Kotlin and Jetpack Compose app structure.
- [x] Room storage, Hilt injection, and Flow-based UI data.
- [x] Historical SMS scanning and live SMS receiver.
- [x] HDFC, Kotak, Slice, and generic UPI SMS parsers.
- [x] Keyword categories and remembered merchant-category mappings.
- [x] Overview, Trail, Insights, Review, and transaction detail screens.
- [x] Experimental Kotak CSV, HDFC XLS, and Slice XLSX statement parsers.
- [x] Unit test source for parsers, financial calculations, and selected mapping behavior.

These checkboxes mean code exists; they do not imply current build validation or production readiness.

## 1. Establish a trustworthy baseline

- [x] Add CI for unit tests, lint, and the Android debug build.
- [x] Add Room database tests for statement replacement, rollback, and cancellation.
- [x] Make statement replacement and reconciliation atomic across all imported accounts.
- [x] Scope replacement to bank plus account suffix; preserve unknown-bank rows.
- [x] Prevent statement rows with missing or repeated references from overwriting one another.
- [ ] Model full account identity to distinguish accounts at the same bank with identical suffixes.
- [ ] Harden malformed-file handling, XML parsing, decompression limits, and UI error recovery.

## 2. Make financial totals dependable

- [x] Retain original observations and conservatively link SMS/statement sources across repeated imports.
- [ ] Resolve ambiguous source matches and broaden notification coverage.
- [ ] Add an explicit owned-account model before treating heuristically paired entries as self-transfers.
- [ ] Distinguish spending, income, EMI, investments, refunds, and credit-card settlements.
- [ ] Improve merchant normalization without overwriting user corrections.
- [ ] Verify date ranges, time zones, rounding, and statement balances with synthetic fixtures.

## 3. Protect local data

- [x] Replace destructive migration fallback with an explicit schema 7-to-8 migration, validated on a disposable database copy.
- [ ] Define upgrade paths for older schemas.
- [ ] Define export, backup, restore, and deletion behavior.
- [ ] Evaluate database encryption and key management.
- [ ] Audit logs, dependencies, permissions, and source-text retention.

## 4. Polish and release

- [ ] Verify fresh installation, permission denial/revocation, and live capture on real devices.
- [ ] Check accessibility, large fonts, empty states, and light/dark themes.
- [ ] Add screenshots captured from the actual app.
- [ ] Select a project license.
- [ ] Define release signing, distribution, and versioning.
- [ ] Publish a release only after its build and validation evidence are available.
