# Ex Astris Save Editor

Android editor for Ex Astris local save files.

## What it does

- Supports **Root (libsu)**, **Shizuku / Sui**, and the Android document picker as three access backends.
- Searches `com.gryphline.exastris.gp` for `SaveFile0.save` and other `.save` files.
- Uses a libsu **RootService** when SuperUser is selected and a Shizuku **UserService** for shell/root access.
- Restricts the privileged service to the Ex Astris package directory.
- Automatic mode can use Root first, Shizuku second, with manual file selection always available.
- Parses the zstd module container and edits backpack item counts.
- Provides search, category filters, add-by-ID, per-item editing and bulk presets.
- Creates local backups and can reload a backup into the editor.
- Uses a dark Material 3 interface split into **Items / Save / Settings** so the item list stays uncluttered.

## UI structure

### Items

The main page is intentionally compact:

- small active-access/save status strip;
- search field and category chips;
- item cards with an icon slot, name, category/ID and quantity;
- a floating add button;
- a bottom sheet for item quantities;
- a separate bottom sheet for bulk actions.

### Save

Contains file details, open/save/export controls and local backup controls. The long `content://` or filesystem path is kept here instead of occupying the item list.

### Settings

Contains access mode, Root/Shizuku diagnostics plus UI/safety preferences:

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


## Access modes

The Settings page now offers four modes:

- **Auto** — use a connected Root service first, then Shizuku; manual selection remains the fallback.
- **Root** — request SuperUser and bind a libsu 6.0.0 RootService.
- **Shizuku** — use the existing Shizuku 13.1.5 UserService.
- **Manual** — never request privileged access; use Android's document picker.

Both privileged backends expose the same restricted file API and refuse paths outside:

```text
/storage/emulated/*/Android/data/com.gryphline.exastris.gp
```

Privileged writes are staged into a temporary file, fsynced, and then renamed over the original. The editor also keeps local backup copies.

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
- Root and Shizuku services refuse paths outside Ex Astris' own `Android/data/com.gryphline.exastris.gp` directory.

## 1.3
- Added Root access with libsu RootService, access mode selection, diagnostics, and staged privileged writes.
- Kept the 1.2.1 stable quantity column fix.


## v1.3.2

- Root reconnect now refreshes a stale non-root libsu shell after SuperUser is enabled externally.
- Root service connection is verified to run as UID 0.
- Settings show only the selected access backend; Auto uses a compact combined diagnostic card.
- Item quantity rendering fix from v1.3.1 is retained.
