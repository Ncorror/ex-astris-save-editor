# Ex Astris Save Editor

Android editor for Ex Astris local save files.

## What it does

- Connects to **Shizuku / Sui** and opens Ex Astris files under `Android/data` without root-only app permissions.
- Automatically searches the game directory `com.gryphline.exastris.gp` for `SaveFile0.save` and other `.save` files.
- Reads/writes the save through a Shizuku **UserService** running as shell/root.
- Restricts the privileged file service to the Ex Astris package directory only.
- Keeps manual Android file-picker open/export as a fallback.
- Parses the zstd module container and edits backpack item counts.
- Provides material/consumable bulk actions and add-by-ID.
- Creates a backup of the original file in the editor's app-specific backup directory when a save is opened.
- Uses a redesigned **dark Material 3 UI** with Shizuku status, save status, search, category filters and inventory cards.

## Shizuku setup

1. Install Shizuku.
2. On Android 11+, start Shizuku using Wireless debugging (or use root/Sui).
3. Open Ex Astris Save Editor.
4. Tap **Connect / Grant** and approve the permission in Shizuku.
5. Fully close Ex Astris before writing its save.
6. Tap **Open Ex Astris save**, edit items, then **Save changes**.

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

The repository includes a GitHub Actions workflow. Push to `main`, then:

```bash
gh run watch
gh run download -n ex-astris-save-editor
```

The artifact is named `ex-astris-save-editor`.

## Safety notes

- Keep the game completely closed while saving changes.
- Keep cloud/local backups of important progress.
- The Shizuku file service refuses paths outside Ex Astris' own `Android/data/com.gryphline.exastris.gp` directory.

## Build artifacts and logs

GitHub Actions stores the successful build as the `ex-astris-save-editor` artifact. It contains:

- the debug APK;
- `build.log` with the full Gradle output;
- `build-info.txt` with the commit, run URL, Java/Gradle versions, and build time.

If the build fails, the workflow still uploads `ex-astris-save-editor-build-logs` with `build.log` and `build-info.txt` so the failure can be diagnosed without downloading the complete GitHub Actions log archive.

For tag builds (`v*`), the APK, `build.log`, and `build-info.txt` are also attached to the GitHub Release as separate files.
