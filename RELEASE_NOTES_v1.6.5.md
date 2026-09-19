# Ex Astris Save Editor v1.6.5

## Highlights

- New searchable add-item catalog replaces manual ID entry.
- Add catalog shows game icons, localized/alternate names, category, short description and protected-item lock state.
- Items already present in the save are automatically hidden.
- Selecting an item opens a quantity confirmation sheet with +/- and quick presets.
- Full 160-item catalog and 160 game icons retained.
- Root, Shizuku, save parsing/writing and bulk-safety logic are unchanged from v1.6.4.

## Release infrastructure

- Public tag builds now use `assembleRelease`.
- Release signing comes from encrypted GitHub Actions secrets.
- The release APK is verified with `apksigner` before publication.
- GitHub Release assets include the versioned APK, build log, build metadata and signature-verification log.

## First public-release install

Previous CI APKs were debug-signed. Android cannot upgrade a debug-signed installation to this release-signed APK in place. Preserve any editor-local backups you need, uninstall the old debug editor, then install the signed v1.6.5 APK.
