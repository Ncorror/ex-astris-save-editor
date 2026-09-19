# Ex Astris Save Editor v1.6.5

## Highlights

- Searchable add-item catalog replaces manual raw-ID entry.
- Add catalog shows game icon, localized/alternate name, category, short description and protected-item lock state.
- Items already present in the opened save are hidden automatically.
- Selecting an item opens quantity confirmation with +/- controls and quick values.
- Full 160-entry catalog and 160 item images retained.
- Current category audit and 43 editable / 117 protected bulk-safety policy retained.
- Root, Shizuku and manual file access remain available.
- Unsaved-change guard, Undo and editor backups remain enabled.

## Release infrastructure

- `main` builds remain debug-signed test builds.
- `v<versionName>` tags use `assembleRelease`.
- Release signing key is supplied through encrypted GitHub Actions secrets.
- Android `apksigner` verification must succeed before publication.
- GitHub Release assets include the versioned APK, build log, build metadata and signature-verification log.

## First release-signed install

Older CI APKs were debug-signed. Android cannot install the first release-signed APK as an in-place update over a debug-signed installation with the same package name.

Before switching to the release build:

1. preserve any editor-local backups you need;
2. keep a backup of the real Ex Astris save;
3. uninstall the old **Ex Astris Save Editor** debug build;
4. install the signed `v1.6.5` APK.

Future versions signed with the same release keystore can update the release-signed installation normally.

## Verified release metadata

```text
Version: 1.6.5
Version code: 20
Catalog entries: 160
Item icons: 160
Build variant: release
Signing: yes
APK signature verification: passed (v2)
```
