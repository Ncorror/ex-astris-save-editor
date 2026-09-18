# Changelog

## 1.5.0

- Added a persistent unsaved-change bar on all pages.
- Added Save-tab pending-change badge.
- Added one-step Undo before a successful write.
- Added explicit Snackbar after edits when autosave is off.
- Added Save / Continue without saving / Cancel protection before close, save switching, manual file selection, backend switching and backup restore.
- Added a complete last-saved byte snapshot for real rollback when discarding pending edits.
- Kept autosave optional and off by default.
- Updated all project documentation for the current Root + Shizuku + icons + save-guard state.

## 1.4.4

- Added the remaining Astrite (`10000`) and Doron (`10001`) HUD/resource icons from supplied game screenshots.

## 1.4.3

- Re-cropped and re-aligned the supplied item icons from in-game screenshots.
- Improved icon scale and card presentation.
- Confirmed a clean CI build after warning cleanup.

## 1.4.2

- Migrated deprecated Activity Result calls.
- Removed Kotlin warnings.
- Stopped unnecessary zstd `.so` strip warnings.
- Updated GitHub Actions to Node-24-compatible action majors.

## 1.4.0–1.4.1

- Added item descriptions / alternate names / knowledge status.
- Added flexible bulk Set / Add / Subtract operations and arbitrary values.
- Added search across item metadata.
- Added first real item icon integration.

## 1.3.x

- Added Root backend and unified access modes.
- Fixed Root reconnect / service UID verification.
- Simplified access settings.
- Fixed clipped quantity rendering.

## 1.2.x and earlier

- Added Shizuku support, Android/data access, dark UI, CI artifacts, parser/editor foundation and early UI refinements.
