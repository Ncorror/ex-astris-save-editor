> Current development build: **1.6.4** — mobile UI polish for the 160-item catalog.

# Ex Astris Save Editor

Android save editor for **Ex Astris** with direct `Android/data` access through Root or Shizuku and a manual SAF fallback.

Current development version: **1.6.4**.

## Current status

Verified on a real device:

- Root access works with **APatch / KernelPatch**.
- Shizuku access works without using the Root backend.
- The editor finds and opens the real Ex Astris save under `Android/data`.
- Item edits are written back successfully and are visible in the game.
- Local backups can be created and loaded.
- The Android build is produced by GitHub Actions.

The Root implementation is intentionally provider-agnostic at the app level: it uses libsu and should be compatible with normal `su` providers such as Magisk, KernelSU / KernelSU Next and APatch. Only APatch has been verified in this project so far; other providers still need device testing.

## Main features

- Root (`libsu 6.0.0`) backend.
- Shizuku (`13.1.5`) backend.
- Android Storage Access Framework fallback.
- Automatic discovery of `SaveFile0.save` and `AutoSaveFile*.save`.
- zstd module parsing and backpack item editing.
- Per-item quantity editing.
- Add-by-ID.
- Bulk **Set / Add / Subtract** with arbitrary values and quick presets.
- Name search across Russian and English item names.
- Category filters.
- Real item icons loaded by numeric item ID.
- Item descriptions and a conservative knowledge-status marker.
- Automatic and manual local backups.
- Optional autosave.
- Persistent protection against forgotten unsaved changes.

## Unsaved-change protection (v1.5.0)

Manual saving remains the default because Ex Astris should be fully closed while its save file is being overwritten. To make manual saving hard to forget, v1.5.0 adds several independent signals:

1. A persistent **unsaved changes bar** appears above the bottom navigation on every page.
2. The bar shows the number of changed item entries and the active save filename.
3. The bar contains **Undo** and **Save** actions.
4. The **Save** tab receives a numeric badge while changes are pending.
5. The compact save strip also shows the changed-entry count.
6. After Apply / Add / Delete / Bulk edit, a Snackbar states that the editor state changed but the file has not been written yet, with a direct Save action.
7. Leaving the app, switching save files, choosing another file, changing access backend, or restoring a backup while edits are pending opens a three-way prompt:
   - Save and continue
   - Continue without saving
   - Cancel
8. “Continue without saving” restores the last saved in-memory snapshot before continuing; it does not merely hide the warning.
9. One-step Undo is available before the next successful file write.

After a successful write, the global bar and Save-tab badge disappear and the Save page reports that all changes are saved.

## UI structure

### Items

- compact active-backend/save strip;
- search;
- category chips;
- icon + item name + category/ID + quantity cards;
- floating Add button;
- per-item editor bottom sheet;
- bulk editor bottom sheet;
- persistent unsaved-change guard above the bottom navigation when required.

### Save

- active file name and source backend;
- save timestamp, module count and inventory count;
- filesystem/content URI location;
- saved/unsaved status;
- Save changes;
- Switch save;
- manual file selection;
- export copy;
- local backup management.

### Settings

Access modes:

- **Auto** — connected Root first, then Shizuku; manual file selection remains available.
- **Root** — libsu RootService.
- **Shizuku** — Shizuku UserService.
- **File** — Android document picker only.

Preferences:

- show/hide numeric item IDs;
- 52 dp / 64 dp item icons;
- confirm bulk actions;
- automatic backup;
- optional autosave.

## Root / Shizuku security boundary

Both privileged file backends expose the same restricted API. The service rejects paths outside Ex Astris' package directory:

```text
/storage/emulated/*/Android/data/com.gryphline.exastris.gp
```

Privileged writes are staged to a temporary file, flushed/fsynced, then committed over the original. Local backups are independent of the game directory.

## Save discovery

The editor searches below:

```text
/storage/emulated/0/Android/data/com.gryphline.exastris.gp/
```

A real tested path had this shape:

```text
/storage/emulated/0/Android/data/com.gryphline.exastris.gp/files/<profile-id>/Save/SaveFile0.save
```

The exact profile ID is discovered automatically.

When several saves are available, `SaveFile0.save` is preferred unless a previously selected path is still present. The picker can still be opened explicitly to select another save or an `AutoSaveFile*.save` slot.

## Editing workflow

Recommended safe workflow:

```text
Fully close Ex Astris
        ↓
Open SaveFile0.save
        ↓
Edit / Apply
        ↓
Persistent “unsaved” bar appears
        ↓
Optional Undo
        ↓
Save
        ↓
Backup + atomic write
        ↓
“All changes saved”
```

Autosave can be enabled in Settings, but is intentionally off by default.

## Bulk editing

Bulk actions support:

- all items or one category;
- Set / Add / Subtract;
- any typed numeric value;
- quick presets (shortcuts, not limits);
- current search/filter only;
- preview of the number of affected entries;
- optional confirmation.

## Item icons

Images are resolved automatically from:

```text
app/src/main/res/drawable-nodpi/
```

Naming:

```text
item_<ID>.png
item_<ID>.webp
```

Examples:

```text
item_10000.png
item_10001.png
item_11001.png
item_9710003.png
```

The current set includes the two currency/resource HUD icons (`10000` Astrite and `10001` Doron) plus the inventory-item crops collected from the supplied game screenshots. Missing future IDs fall back to the built-in placeholder.

See `ICON_CROPS.md`, `V1_4_3_ICON_POLISH.md`, and `V1_4_4_CURRENCY_ICONS.md` for the image-history notes.

## Item knowledge base

Catalog metadata lives in:

```text
app/src/main/assets/items.json
```

The editor intentionally separates public name/purpose evidence from internal numeric-ID mapping. Public references generally do not publish Ex Astris save IDs, so unknown mappings must not be invented.

See `ITEM_RESEARCH.md`.

### Languages and evidence

The item catalog presented by the app is intentionally **Russian + English only**. Japanese/Chinese references may be used during research, but their text is translated and is not shipped as an additional UI language. The app now ships a 160-entry catalog with Russian and English names/descriptions. The icon pack has been replaced with the newly extracted `item.ab` sprite set. 158 IDs have exact extracted icons; IDs `500014` and `500015` currently use temporary placeholder icons and provisional text because their exact localized sprite/name pair was not present in the supplied resources.

## Build

GitHub Actions is configured in `.github/workflows/build.yml`.

Push to `main` and watch the run:

```bash
gh run watch
```

Successful runs publish:

```text
ex-astris-save-editor-debug/
  ex-astris-save-editor.apk
  build.log
  build-info.txt

ex-astris-save-editor-verification/
  build.log
  build-info.txt
  reports/       # when generated
  test-results/  # when generated
```

Download the APK artifact:

```bash
gh run download -n ex-astris-save-editor-debug
```

## Safety notes

- Fully close Ex Astris before writing its save.
- Keep automatic backup enabled unless there is a specific reason not to.
- Do not treat AutoSave files as interchangeable with the main save without knowing what the game will load next.
- Root/Shizuku access is restricted to the Ex Astris `Android/data` tree.
- If a write fails, do not retry blindly; inspect the error and keep the backup.

## Documentation map

- `README.md` — current product overview.
- `SETUP.md` — Termux/GitHub build and update flow.
- `TESTING.md` — regression checklist.
- `CHANGELOG.md` — release history.
- `V1_5_0_SAVE_GUARD.md` — unsaved-change protection design.
- `ITEM_RESEARCH.md` — item-description research and ID caveats.
- `ICON_CROPS.md` — icon extraction conventions.
- `V1_4_3_ICON_POLISH.md` — full icon crop polish pass.
- `V1_4_4_CURRENCY_ICONS.md` — Astrite/Doron icon completion.
- `WARNING_CLEANUP.md` — build-warning cleanup history.
- `V1_4_NOTES.md` — v1.4 feature-development notes.


## v1.5.2 catalog expansion

The previous icon set has been fully replaced. v1.6.0 upgrades the editor from the older 82-entry catalog to the extracted **160-entry** in-game catalog. All `item_*.png` resources now come from the new `item.ab` atlas export, giving the app a single consistent icon style across the catalog.

New IDs: `12005`, `12007`, `12008`, `12009`, `210012`, `500001`, `710002`, `710004`, `710005`, `720005`, `730002`, `800006`, `9710017`.

New Entropith/Other entries are explicitly excluded from bulk quantity edits. Existing item metadata and existing image files were intentionally left untouched.

## v1.5.1 UX and bulk-safety update

- Bulk actions skip protected catalog entries (`bulk_editable: false`). Entropiths and unique/other items are protected by default.
- The bulk sheet shows how many items will change and how many protected items were skipped.
- Bulk categories are limited to editable groups: resources/currency, consumables and materials.
- Search matches item names only (localized name + alternate RU/EN name). Numeric IDs and descriptions no longer influence search results.
- The unsaved-change guard is now a compact single-line bar with Undo and a primary Save button.
- The Save tab uses a small dot badge instead of a large numeric badge; the exact changed count is shown in the persistent guard and save status.
- The compact access card no longer duplicates the Save action when access is already connected.

## v1.6.1 catalog filters

The 160-entry catalog now keeps its extracted source categories in the UI. In addition to resources, consumables, materials and Entropiths, the inventory can be filtered by Recipes, Tritris, Quest items, Curiosities and Packs. Unknown IDs still fall back to Other. Bulk quantity actions remain limited to resources, consumables and materials.


## v1.6.4 category audit

The catalog filters now follow the game's own item hierarchy more closely. Loot packs are shown under Consumables, the four Doron sell-items are shown as Valuables, Laylah Kernel is shown under Materials, and the old internal `Tritris` label is presented as Laylah-Keys. Bulk-edit protection is unchanged: packs and Laylah Kernel remain protected until their quantity behavior is explicitly verified.
