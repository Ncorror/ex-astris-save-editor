# Ex Astris Save Editor v1.8.0

## Highlights

- **No game files inside the app.** The Arknights skin switches no longer ship the four character map bundles. The editor now patches the map files already installed with the game, directly on your phone.
- **Same result as before, byte for byte.** Enabling a skin produces exactly the known working Stage 127/128 files, and disabling it restores exactly the original game file (identical SHA-256). The editor uses a port of the same LZ4HC level 12 compressor the game's bundles are built with.
- **Recognized by structure, not by a fixed version.** A map file whose structure does not match (another character's file, a damaged file, a changed format) is shown as unrecognized and cannot be switched.
- Before every switch the editor still makes a verified backup, writes atomically and reads the file back.

## Updating from v1.7.0

- `v1.8.0` is signed with the same release key and installs over `v1.7.0` as a normal update.
- Skins that are already switched on with v1.7.0 are recognized as Arknights skins. Nothing needs to be redone.

## Checked

- Enable and disable were checked in game on a device for both Hime and MsBlack.
- Unit tests cover the LZ4/LZ4Lit codec (including a reference vector), the UnityFS rebuild and the skin patch on synthetic bundles.

See [`docs/PATCH_METHOD_HIME_MSBLACK.md`](https://github.com/Ncorror/ex-astris-save-editor/blob/main/docs/PATCH_METHOD_HIME_MSBLACK.md) for how the patch works.

## Release metadata

```text
Version: 1.8.0
Version code: 25
Catalog entries: 160
Item icons: 160
Build variant: release
```
