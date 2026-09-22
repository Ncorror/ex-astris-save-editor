# Changelog

All notable project changes are listed here in reverse chronological order.

## 1.7.0-dev3 (unreleased)

- Reject malformed or oversized save trailers, module ranges and decompressed modules before editing.
- Validate the assembled save before writing; refuse writes if the on-disk save changed since opening.
- Require a verified local backup before every write of an existing save, including manual document access.
- Add save parser regression tests to the Android CI build.

## 1.7.0-dev2 (unreleased)

- Moved the Arknights skin controls from the bottom of Save to their own labeled bottom navigation tab.
- Kept the same bundle checks, backups and independent Hime/MsBlack actions.

## 1.7.0-dev (unreleased)

- Added independent Hime and MsBlack Arknights map switches through Root/Shizuku.
- Verify known game bundle SHA-256 before replacing, require a verified backup, use atomic writing and verify the result.
- Documented Stage 126–128 evidence, device checks and remaining audit items in `docs/`.
- MsBlack's in-game test and the Android build/device test remain pending.

## 1.6.5

- Replaced manual raw-ID entry in the add flow with a searchable catalog.
- Add catalog shows game icon, localized name, alternate name, category, short description and protected-item lock state.
- Entries already present in the opened save are hidden from the add catalog.
- Selecting an entry opens quantity confirmation with +/- controls and quick values.
- Added release signing through GitHub Actions secrets.
- Tagged builds use `assembleRelease`, verify the APK with Android `apksigner`, and publish a GitHub Release only when signature verification succeeds.
- Fixed release verification to locate `apksigner` explicitly from the Android SDK instead of assuming it is in shell `PATH`.
- Preserved the 160-entry catalog, 160 icons, Root/Shizuku access and existing 43 editable / 117 protected bulk-safety policy.

## 1.6.4

- Audited item categories against extracted game tables and in-game terminology.
- Moved all 12 loot packs into Consumables while keeping them bulk-protected.
- Replaced the misleading Curiosities category with Valuables for the four Doron sell-items.
- Moved Laylah Kernel (`9910007`) from Quest items to Materials while keeping it bulk-protected.
- Renamed the internal Tritris label to Laylah-Keys for diagrams and passage keys.
- Preserved all 160 item icons and the 43 editable / 117 protected split.

## 1.6.3

- Polished the inventory UI for the expanded catalog without changing save parsing/writing.
- Added automatic centering/edge fading to horizontally scrollable category filters.
- Added the search clear action.
- Added lock indicators for protected items in the list and edit sheet.
- Disabled Bulk when the visible result set contains no eligible entries.
- Reworked quick quantity presets so large values fit on narrow screens.
- Improved long-name handling and description readability.
- Removed residual lightweight markup before catalog text is displayed.

## 1.6.2

- Replaced visual-guess English item names with game terminology across the expanded catalog where evidence was available.
- Added polished English descriptions for previously generic records.
- Corrected Amber, Entropith, recipe, sell-item, Laylah-Key and quest terminology.
- Kept IDs `500014` and `500015` as protected internal hybrid records because the supplied game bundle did not contain dedicated matching sprites.

## 1.6.1

- Split the old Other bucket into dedicated source categories for the expanded 160-entry catalog.
- Preserved conservative bulk-edit safety.
- Added category-aware fallback descriptions and localized labels.

> Category names from this release were later corrected by the v1.6.4 audit; use current documentation for the present taxonomy.

## 1.6.0

- Replaced the old mixed icon set with sprites extracted from the supplied `item.ab` bundle.
- Expanded `items.json` from 82 to 160 catalog entries using extracted game tables.
- Added Russian/English names and descriptions for the expanded catalog.
- Added 78 new item drawable resources.

## 1.5.3

- Completed Russian and English names/descriptions for the then-current 82-entry catalog.
- Added sourced effects/purpose descriptions where evidence existed.
- Preserved conservative handling for uncertain mappings.

## 1.5.2

- Added 13 newly discovered IDs and matching icons, growing the catalog from 69 to 82 entries.

## 1.5.1

- Added protected-item bulk safety and protected/skipped preview counts.
- Limited bulk target groups to editable categories.
- Changed inventory search to item names only.
- Compacted the persistent save guard and Save-tab badge.

## 1.5.0

- Added persistent unsaved-change protection.
- Added one-step Undo before a successful write.
- Added Save / discard / cancel protection before navigation/state-changing operations.
- Added real rollback to the last saved in-memory snapshot.

## 1.4.4

- Added Astrite (`10000`) and Doron (`10001`) resource icons.

## 1.4.3

- Re-cropped/re-aligned the first supplied item icon set.
- Improved icon scale and card presentation.

## 1.4.2

- Migrated deprecated Activity Result calls.
- Cleaned Kotlin/build warnings and updated CI actions.

## 1.4.0–1.4.1

- Added item descriptions, early bulk editing/search and first real item icons.

## 1.3.x

- Added Root backend and unified access modes.
- Fixed Root reconnect/service UID verification.
- Fixed clipped quantity rendering.

## 1.2.x and earlier

- Added Shizuku support, `Android/data` access, dark UI, CI artifacts and the initial parser/editor foundation.
