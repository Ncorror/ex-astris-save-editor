# Ex Astris Save Editor

Android editor for Ex Astris local save files.

## What it does

- Connects to **Shizuku / Sui** and opens Ex Astris files under `Android/data` without giving the normal app broad storage access.
- Searches `com.gryphline.exastris.gp` for `SaveFile0.save` and other `.save` files.
- Reads and writes through a Shizuku **UserService** running as shell/root.
- Restricts the privileged service to the Ex Astris package directory.
- Keeps the Android document picker as a fallback.
- Parses the zstd module container and edits backpack item counts.
- Provides search, category filters, add-by-ID, per-item editing and bulk presets.
- Creates local backups and can reload a backup into the editor.
- Uses a dark Material 3 interface split into **Items / Save / Settings** so the item list stays uncluttered.

## UI structure

### Items

The main page is intentionally compact:

- small Shizuku/save status strip;
- search field and category chips;
- item cards with an icon slot, name, category/ID and quantity;
- a floating add button;
- a bottom sheet for item quantities;
- a separate bottom sheet for bulk actions.

### Save

Contains file details, open/save/export controls and local backup controls. The long `content://` or filesystem path is kept here instead of occupying the item list.

### Settings

Contains Shizuku status plus UI/safety preferences:

- show/hide numeric item IDs;
- compact or larger item icons;
- confirmation for bulk actions;
- automatic backup when a save is opened.

## Adding item icons

The list is already wired for icons. Put image resources in:

```text
app/src/main/res/drawable-nodpi/
```

Use the item ID in the resource name:

```text
item_10000.webp
item_10001.webp
item_11001.webp
```

PNG also works. Android resource filenames must stay lowercase and use only letters, digits and underscores. If an icon is missing, the app shows the built-in placeholder automatically.

## Shizuku setup

1. Install Shizuku.
2. On Android 11+, start Shizuku using Wireless debugging (or use root/Sui).
3. Open Ex Astris Save Editor.
4. Tap **Connect / Grant** and approve the permission in Shizuku.
5. Fully close Ex Astris before writing its save.
6. Open the save, edit items, then use the **Save** tab to write changes.

The Android package currently targeted is:

```text
com.gryphline.exastris.gp
```

Typical local save location is below:

```text
/storage/emulated/0/Android/data/com.gryphline.exastris.gp/files/<account-or-random-id>/save/
```

The exact subdirectory is discovered automatically.

## Build

The repository includes GitHub Actions. Push to `main`, then:

```bash
gh run watch
```

Successful runs publish:

```text
ex-astris-save-editor-debug
  ex-astris-save-editor.apk
  build.log
  build-info.txt

ex-astris-save-editor-verification
  build.log
  build-info.txt
  reports/       (when available)
  test-results/  (when available)
```

Download the APK artifact with:

```bash
gh run download -n ex-astris-save-editor-debug
```

## Safety notes

- Keep the game completely closed while saving changes.
- Keep backups of important progress.
- Automatic backups are enabled by default and can be disabled in Settings.
- The Shizuku service refuses paths outside Ex Astris' own `Android/data/com.gryphline.exastris.gp` directory.
