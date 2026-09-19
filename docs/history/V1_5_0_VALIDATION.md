# v1.5.0 validation notes

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

Static validation performed before packaging:

- Android XML files parse successfully.
- English and Russian string tables contain the same 174 string keys.
- No duplicate string resource names were found.
- Every `R.string.*` / `@string/*` reference found by the static scan exists in the base string table.
- `assets/items.json` parses successfully and contains 69 entries.
- New ActivityMain ViewBinding IDs (`saveGuardBar`, `saveGuardTitle`, `saveGuardSubtitle`, `undoChangeButton`, `saveNowButton`) all exist in `activity_main.xml`.
- Kotlin compiler parsing produced no syntax/`expecting` errors in the modified sources. Full Android type resolution is not possible in this container because the Android SDK/dependency classpath is not installed.
- Version was bumped to 1.5.0 / versionCode 11.
- GitHub Actions `build-info.txt` version was updated to 1.5.0 and now records the persistent unsaved-change guard.

Functional regression expectations:

- Root / Shizuku file services and `SaveFile` parser/writer were not structurally rewritten by this patch.
- Existing automatic backup and atomic privileged-write path remain in place.
- Final Android compilation and device behavior must still be confirmed by GitHub Actions and the real-device checklist in `TESTING.md`.


> Superseded by v1.5.1 for bulk-safety/search/save-bar UI details. See `V1_5_1_BULK_SEARCH_UI.md`.
