# Ex Astris Save Editor

Android save editor for **Ex Astris** with direct save access through Root or Shizuku, plus a Storage Access Framework fallback.

Version `1.7.0` includes a dedicated **Skins** tab in the bottom navigation with separate Arknights switches for Hime and MsBlack. These switches replace only the two verified character map AssetBundles under the game's `Android/data/.../files/Download/ab` tree. Root or Shizuku access is required; the manual file picker remains a save-file tool. The editor refuses an unknown bundle hash, saves a verified backup before each replacement, and reads the installed file again after an atomic write. The signed `1.7.0` release, including the Skins tab and both switches, was checked on a device by the user.

The skin switches work independently of save-file `style` or notification flags. Ex Astris must be fully closed before switching, and a game update may replace the modified bundles. See [`docs/PATCH_METHOD_HIME_MSBLACK.md`](docs/PATCH_METHOD_HIME_MSBLACK.md) for the exact file changes and [`docs/COLLAB_SKIN_AUDIT.md`](docs/COLLAB_SKIN_AUDIT.md) for the evidence and remaining work.

**Current version:** `1.7.0` (`versionCode 24`)

## Status

The save-editing features in the previous `1.6.5` build were tested on a real Android device with the game's live save format. The signed `1.7.0` release was checked on a device by the user, including the Skins tab, the Hime and MsBlack switches and the hardened save writes. The signed release pipelines for `1.6.5` and `1.7.0` completed successfully and verified their release APKs with Android `apksigner`.

Verified project state:

- save discovery and editing under Ex Astris `Android/data`;
- Root backend verified with APatch / KernelPatch;
- Shizuku backend verified on-device;
- manual file picker/export fallback;
- 160 catalog entries and 160 item icon resources;
- searchable add-item catalog;
- Hime and MsBlack Arknights skin switches on the Skins tab;
- protected bulk-edit rules (`43` editable / `117` protected catalog entries);
- automatic/manual editor backups;
- unsaved-change guard and one-step Undo;
- signed tag builds for GitHub Releases.

The Root implementation uses libsu and is designed to work with standard `su` providers such as Magisk, KernelSU / KernelSU Next and APatch. Only APatch / KernelPatch has been explicitly device-tested in this project so far.

## Features

### Save access

- **Auto** — prefers an already connected Root backend, then Shizuku.
- **Root** — libsu `RootService`, requiring UID 0.
- **Shizuku** — Shizuku `UserService`.
- **File** — Android document picker fallback.

The editor searches the Ex Astris package tree only:

```text
/storage/emulated/*/Android/data/com.gryphline.exastris.gp
```

A typical main-save path is:

```text
/storage/emulated/0/Android/data/com.gryphline.exastris.gp/files/<profile-id>/Save/SaveFile0.save
```

The profile directory is discovered automatically. `SaveFile0.save` is preferred when several save slots are found, while manual slot selection remains available.

### Inventory editing

- quantity editing for existing entries;
- searchable add-item catalog instead of raw ID entry;
- Russian and English item-name search;
- category filters;
- real game icons resolved by numeric item ID;
- bilingual descriptions;
- per-item protection marker;
- delete, Undo and manual Save;
- bulk **Set / Add / Subtract** with arbitrary numeric values and quick presets.

The add catalog hides items already present in the opened save. Selecting an entry opens a quantity confirmation sheet before the in-memory save is changed.

### Bulk-edit safety

Bulk operations use explicit catalog metadata. The current catalog contains:

```text
43  bulk-editable entries
117 protected entries
```

Bulk editing is intentionally conservative. Protected entries can still be edited individually, but they are skipped by mass operations.

### Unsaved-change protection

Manual saving is the default because Ex Astris should be fully closed while its save is overwritten.

When an edit is pending, the app provides:

- a persistent unsaved-change bar;
- a Save-tab badge;
- one-step Undo;
- a Snackbar with a direct Save action;
- Save / discard / cancel protection before leaving, changing save, changing backend or restoring a backup.

“Continue without saving” restores the last saved in-memory snapshot instead of merely hiding the warning.

### Backups

Automatic backup on opening a save is enabled by default and can be disabled in Settings. Every write of an existing save still requires a verified local backup; if backup creation fails or the game changed the file after it was opened, the write stops. Manual backups can also be created and restored from inside the app.

Editor-local backups use the app-specific external files directory (`getExternalFilesDir("backups")`). They are separate from the Ex Astris game save directory.

## Catalog

Catalog metadata is stored in:

```text
app/src/main/assets/items.json
```

Icons are stored in:

```text
app/src/main/res/drawable-nodpi/item_<ID>.png
```

Current catalog categories:

| Category key | UI label | Entries |
| --- | --- | ---: |
| `currency` | Resources / Ресурсы | 2 |
| `consumable` | Consumables / Расходники | 23 |
| `material` | Materials / Материалы | 31 |
| `entropite` | Entropites / Энтропиты | 16 |
| `recipe` | Recipes / Рецепты | 25 |
| `laylah_key` | Laylah-Keys / Ключи Лайлы | 34 |
| `quest` | Quest items / Задания | 25 |
| `valuable` | Valuables / Ценности | 4 |

Unknown IDs that appear in a save remain visible through the fallback `Other / Прочее` category.

The current icon pack was extracted from the supplied game `item.ab` bundle. `158` catalog IDs have exact extracted sprite matches. IDs `500014` and `500015` remain protected internal hybrid records and use provisional catalog/icon handling because dedicated matching sprites were not present in the supplied bundle.

See [`docs/CATALOG.md`](docs/CATALOG.md) and [`ITEM_RESEARCH.md`](ITEM_RESEARCH.md) for the evidence and safety policy.

## Safe editing workflow

```text
Fully close Ex Astris
        ↓
Open SaveFile0.save
        ↓
Edit / add / delete
        ↓
Review the unsaved-change indicator
        ↓
Save
        ↓
Backup + staged write
        ↓
Reopen the game and verify
```

Do not blindly retry a failed write. Keep the backup and inspect the reported error first.

## Build and CI

GitHub Actions is defined in:

```text
.github/workflows/build.yml
```

A normal push to `main` builds a **debug** APK and verification artifacts. A tag matching `v<versionName>` builds a **signed release** APK, verifies it with `apksigner`, and publishes a GitHub Release only if build and signature verification both succeed.

Useful commands:

```bash
gh run watch
gh run download -n ex-astris-save-editor-build
gh run download -n ex-astris-save-editor-verification
```

The build artifact includes the APK, `build.log` and `build-info.txt`. Tagged release builds also include `release-signature.log`.

See [`SETUP.md`](SETUP.md) for the Termux workflow and [`RELEASE.md`](RELEASE.md) for signing and publication.

## Security notes

- Never commit the release keystore or signing passwords.
- Keep at least one offline backup of the release `.jks` file.
- Root/Shizuku services reject paths outside the Ex Astris package tree.
- Privileged writes are staged before replacing the original file.
- Fully close Ex Astris before writing a save.
- Do not assume an AutoSave slot is interchangeable with the main save.

## Documentation

Current documentation:

- [`README.md`](README.md) — current product overview and project status.
- [`SETUP.md`](SETUP.md) — Termux setup, project updates and CI artifacts.
- [`TESTING.md`](TESTING.md) — current regression and release checklist.
- [`RELEASE.md`](RELEASE.md) — release signing and GitHub Release process.
- [`CHANGELOG.md`](CHANGELOG.md) — release history.
- [`ITEM_RESEARCH.md`](ITEM_RESEARCH.md) — evidence policy for item metadata and edit safety.
- [`RESEARCH_SOURCES.md`](RESEARCH_SOURCES.md) — external and extracted-data research sources.
- [`docs/CATALOG.md`](docs/CATALOG.md) — current catalog schema, categories and special cases.
- [`RELEASE_NOTES_v1.7.0.md`](RELEASE_NOTES_v1.7.0.md) — notes for the latest signed release.
- [`RELEASE_NOTES_v1.6.5.md`](RELEASE_NOTES_v1.6.5.md) — notes for the previous signed release.

Historical implementation/validation notes have been moved to [`docs/history/`](docs/history/README.md). They document older project states and should not be used as current setup instructions.

## Disclaimer

This is an unofficial community tool and is not affiliated with or endorsed by GRYPHLINE or the developers/publishers of Ex Astris. Keep backups and use save editing at your own risk.
