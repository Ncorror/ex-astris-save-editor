# v1.5.2 validation notes

Static packaging checks completed before delivery:

- `assets/items.json` parses successfully: **82 entries**.
- `drawable-nodpi/item_*.png`: **82 files**.
- Exactly 13 IDs were added relative to v1.5.1.
- Each added ID has a matching icon from the supplied `item_icons(1).zip`.
- Existing 69 item records were preserved rather than replaced by the rough catalog snapshot.
- Existing icon files were not overwritten.
- New Entropith/Other entries have `bulk_editable: false`.
- Android resource XML parses successfully.
- Version is `1.5.2`, versionCode `13`.

The final Android/Gradle compile is still verified by GitHub Actions.
