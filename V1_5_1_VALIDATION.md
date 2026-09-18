# v1.5.1 validation notes

Static validation performed before packaging:

- `items.json` parses successfully: 69 catalog entries.
- 22 catalog entries are explicitly protected with `bulk_editable: false` (5 Entropiths + 17 Other/unique entries).
- 47 catalog entries are bulk-editable by default (currency, consumables, materials).
- All Android XML resource files parse successfully.
- RU and EN string resource key sets match exactly.
- Removed ViewBinding references (`entropiteButton`, `otherButton`, `saveGuardSubtitle`) are no longer referenced in Kotlin.
- Kotlin brace/parenthesis/bracket balance is clean.
- A parser-level `kotlinc` pass found no targeted syntax/stale-binding errors; Android references remain unresolved outside an Android classpath as expected.
- Version is `1.5.1`, versionCode `12`.
- GitHub Actions `build-info.txt` metadata was updated for bulk safety, compact save guard and name-only search.

Final Android compilation must still be confirmed by GitHub Actions.
