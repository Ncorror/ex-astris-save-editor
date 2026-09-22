# Ex Astris Save Editor v1.7.0

## Highlights

- New **Skins** tab in the bottom navigation with independent Arknights map switches for Hime and MsBlack (Root or Shizuku required).
- Skin switches check the known SHA-256 of the game bundle, refuse unknown or game-updated files, save a verified backup before replacement, write atomically and read the installed file back.
- Save parsing rejects malformed or oversized trailers, module ranges and decompressed modules before editing.
- The assembled save is validated before writing, and writes are refused if the save on disk changed since it was opened.
- A verified local backup is required before every write of an existing save, including manual document access.
- Save parser regression tests now run in the Android CI build.

## Notes

- Fully close Ex Astris before switching skins. A game update may replace the modified bundles.
- The skin switches are independent of save-file `style` or notification flags.
- The signed release was checked on a device: the Skins tab, both switches (including MsBlack in game) and save writes work.
- See [`docs/PATCH_METHOD_HIME_MSBLACK.md`](docs/PATCH_METHOD_HIME_MSBLACK.md) and [`docs/COLLAB_SKIN_AUDIT.md`](docs/COLLAB_SKIN_AUDIT.md) for details.

## Updating

`v1.7.0` is signed with the same release key as `v1.6.5`, so it installs over a release-signed `v1.6.5` as a normal update.

Installations of older debug-signed CI builds must be uninstalled first; see [`RELEASE.md`](RELEASE.md#4-first-release-signed-install).

## Release metadata

```text
Version: 1.7.0
Version code: 24
Catalog entries: 160
Item icons: 160
Build variant: release
```
