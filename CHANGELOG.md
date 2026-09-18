# Changelog

## 1.5.3

- Completed Russian and English names/descriptions for all 82 catalog entries.
- Kept Japanese/Chinese research terminology out of the app UI; it is used only as source material.
- Added precise sourced effects for Vitality Amber, Vitaflow Amber and Entropith Triode.
- Added sourced purpose descriptions for crafting materials, cooking ingredients, Astrite, Doron and Laylah Kernel.
- Added bilingual conservative descriptions for IDs whose exact official name/effect is not yet mapped.
- Incorporated live-game safety findings into descriptions for stack-safe materials/consumables and protected unique/Entropith/vendor items.
- Preserved bulk-edit protections from v1.5.1/v1.5.2.

## 1.5.2

- Added 13 newly discovered item IDs from the user-provided catalog snapshot.
- Added the corresponding 13 128x128 item icons.
- Preserved all existing 69 catalog entries and their image files unchanged.
- New Entropith/Other IDs are protected from bulk quantity editing.
- Catalog now contains 82 entries with 82 matching icons.


## 1.5.1
- Exclude protected/non-stack-safe items from bulk quantity operations.
- Add `bulk_editable` catalog metadata; current Entropith and Other/unique entries are protected.
- Show changed vs protected/skipped counts in the bulk preview.
- Remove Entropith/Other choices from the bulk target selector.
- Search by item name only; ID/description/category are no longer search keys.
- Replace the large warning save panel with a compact persistent one-line save guard.
- Replace the large numeric Save-tab badge with a dot badge.
- Remove the duplicate top-card Save action while access is connected.

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
