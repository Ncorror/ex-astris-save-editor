# v1.6.5 validation

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

Expected metadata:

- Version name: 1.6.5
- Version code: 20
- Catalog entries: 160
- Item icons: 160
- Add flow: searchable catalog
- Existing bulk safety: unchanged

Pre-release phone checks:

1. Open a known-good save.
2. Tap `+`; with the current 82-item save the catalog should initially expose 78 remaining known entries.
3. Search for at least one Russian name and one English/alternate name.
4. Add one ordinary item with quantity 1 and one protected item with a small test quantity.
5. Confirm both appear correctly and the protected one still shows the lock.
6. Undo/discard or restore the test save as needed.
7. Save once and reopen the save to confirm persistence.

Release checks:

- Debug CI on `main` must pass before tagging.
- Tag must be exactly `v1.6.5`.
- Release workflow must report `Build variant: release` and `Signing: yes`.
- `release-signature.log` must exist.

First public release install:

- The old debug-signed app cannot be upgraded in place by the release-signed APK. Copy any editor-local backups you need, uninstall the debug editor, then install the release APK.
