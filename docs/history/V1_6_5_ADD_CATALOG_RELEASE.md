# v1.6.5 — Add catalog + release candidate

> **Historical snapshot.** This file describes an earlier project state. For current behavior and instructions, see [`README.md`](../../README.md), [`SETUP.md`](../../SETUP.md), [`TESTING.md`](../../TESTING.md) and [`RELEASE.md`](../../RELEASE.md).

## Add-item UX

The old free-form `ID=quantity` text field has been removed from the normal UI. The `+` action now opens a searchable catalog generated from the same 160-entry data used by the inventory screen.

The catalog:

- hides items already present in the current save;
- searches localized and alternate-language names;
- shows icon, name, category, description and protected status;
- keeps protected/unique items individually addable while preserving bulk-edit protection;
- asks for quantity on a dedicated second sheet before insertion.

## Release preparation

GitHub Actions now distinguishes development and release builds:

- branch / pull-request builds: `assembleDebug`;
- `v*` tags: signing secrets are mandatory, `assembleRelease` is used, signature verification runs via `apksigner`, and the versioned APK is attached to a GitHub Release.

See `RELEASE.md` for the one-time signing setup and tag commands.
