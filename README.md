# Ex Astris Save Editor

[![Latest release](https://img.shields.io/github/v/release/Ncorror/ex-astris-save-editor?label=release)](https://github.com/Ncorror/ex-astris-save-editor/releases/latest)
[![Build](https://github.com/Ncorror/ex-astris-save-editor/actions/workflows/build.yml/badge.svg)](https://github.com/Ncorror/ex-astris-save-editor/actions/workflows/build.yml)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Unofficial Android save editor for **Ex Astris**. Edit your inventory, keep verified backups of your save, and switch Hime and MsBlack to their Arknights collaboration skins.

## Features

- **Direct save access** with Root or Shizuku. The save is found automatically. Without either, pick the file manually.
- **Inventory editing.** Change quantities, delete items, or add new ones from a searchable catalog of 160 items with the game's own icons.
- **Bulk Set / Add / Subtract** for many items at once. Unique and quest items are protected and never changed in bulk.
- **Safe writes.** A verified backup is made before every save, corrupt files are refused, and the result is checked after writing.
- **Undo and unsaved-change warnings,** so nothing is lost or written by accident.
- **Arknights skins** for Hime and MsBlack, switched on and off independently (Root or Shizuku). The editor patches the game's own files on your phone and does not include any game files.
- **English and Russian** interface and item descriptions.

## Download

Download the APK from the [latest release](https://github.com/Ncorror/ex-astris-save-editor/releases/latest). Requires Android 8.0 or newer.

## How to use

1. Fully close Ex Astris.
2. Open the editor and choose an access mode in **Settings → Access**: Auto, Root, Shizuku or File.
3. On the **Save** tab, open `SaveFile0.save`.
4. Edit items on the **Items** tab, then tap **Save**.
5. Start the game and check the result.

**Switching skins:** close the game, open the **Skins** tab and tap **Enable** or **Disable** for Hime or MsBlack. The change takes effect after the game restarts.

The save is usually located at:

```text
/storage/emulated/0/Android/data/com.gryphline.exastris.gp/files/<profile-id>/Save/SaveFile0.save
```

## Safety

- Always close the game before saving or switching skins.
- Backups can be restored from the **Save** tab (**Restore backup**).
- The editor only works inside the Ex Astris data folder and refuses unknown or damaged files instead of guessing.
- A game update may undo the skin change. If that happens, switch it on again.

## Building

GitHub Actions builds every push to `main`: it runs the tests and produces a debug APK. Tags `v<version>` produce signed releases. To build locally you need JDK 17 and Android SDK 34:

```bash
gradle testDebugUnitTest assembleDebug
```

## Documentation

- [SETUP.md](SETUP.md): Termux workflow and CI artifacts
- [TESTING.md](TESTING.md): test and release checklist
- [RELEASE.md](RELEASE.md): signing and publishing releases
- [CHANGELOG.md](CHANGELOG.md): version history
- [ITEM_RESEARCH.md](ITEM_RESEARCH.md) and [docs/CATALOG.md](docs/CATALOG.md): item data, sources and bulk-edit safety
- [docs/PATCH_METHOD_HIME_MSBLACK.md](docs/PATCH_METHOD_HIME_MSBLACK.md) and [docs/COLLAB_SKIN_AUDIT.md](docs/COLLAB_SKIN_AUDIT.md): how the skin files are modified and verified (in Russian)

## Disclaimer

This is an unofficial fan-made tool. It is not affiliated with or endorsed by GRYPHLINE, Hypergryph or the developers of Ex Astris. Editing saves is at your own risk. Keep backups.

## License

The source code is released under the [MIT License](LICENSE).

The item icons (`app/src/main/res/drawable-nodpi/item_*.png`) were extracted from Ex Astris, and item names and descriptions in `items.json` are partly based on the game's text. Ex Astris, its characters, artwork and text are the property of GRYPHLINE and their respective owners. They are not covered by the MIT License and are used only to identify items in this fan-made tool. Arknights is a trademark of Hypergryph.
