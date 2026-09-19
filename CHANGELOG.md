## 1.6.5

- Replaced manual ID entry in the add-item flow with a searchable catalog.
- Catalog rows show icon, localized name, alternate name, category, short description and protected-item lock state.
- Items already present in the opened save are hidden from the add catalog.
- Selecting an item opens a quantity sheet with +/- controls and quick values before insertion.
- Added release signing support through GitHub Actions secrets.
- Tagged builds now create a signed release APK, verify it with `apksigner`, and publish a GitHub Release.

## 1.6.4

- Audited item categories against the extracted game tables and in-game terminology.
- Moved all 12 loot packs into Consumables; they remain protected from bulk edits.
- Replaced the misleading Curiosities category with Valuables for the four Doron sell-items.
- Moved Laylah Kernel (9910007) from Quest items to Materials while keeping it bulk-protected.
- Renamed the technical Tritris category to Laylah-Keys for diagrams and passage keys.
- Preserved all 160 item icons and the existing 43 editable / 117 protected bulk-safety split.

## 1.6.3

- Polished the inventory UI for the expanded 160-item catalog without changing save parsing/writing.
- Added automatic centering and edge fading to the horizontally scrollable category filters.
- Added the built-in clear action to name search.
- Added a lock indicator for items protected from bulk edits, both in the list and the edit sheet.
- Disable the Bulk action when the currently visible result set contains no bulk-editable items.
- Reworked quick quantity presets into four equal-width buttons so `999 999` no longer clips on narrow screens.
- Improved long-name handling and description readability in item cards/sheets.
- Strip residual lightweight HTML/keyword markup before catalog text is displayed.
- Updated item knowledge wording to reflect the game-resource-derived catalog.

## 1.6.2

- Replaced old visual-guess English item names with proper game terminology across the 160-entry catalog.
- Added polished English descriptions for previously generic catalog records.
- Corrected Amber, Entropith, recipe, sellable-item, Laylah-Key and quest-item terminology.
- Marked IDs `500014` and `500015` as protected internal hybrid records: they are present in the table dump but are not among the 14 normally obtainable Entropiths and have no dedicated sprite in the supplied `item.ab`.
- Removed simple `<i>` markup from plain-text item descriptions.

## 1.6.1

- Split the old `Other` bucket into dedicated catalog filters: Recipes, Tritris, Quest items, Curiosities and Packs.
- Preserved the existing protected bulk-editing policy: only currency, consumables and materials are eligible for mass quantity changes.
- Restored the extracted source category for all 160 catalog records instead of flattening 101 records into `other`.
- Updated fallback descriptions and category labels in Russian and English.
- Bulk-action UI now recognizes every protected category when choosing the visible-only default.

## 1.6.0

- Replaced the entire item icon set with the newly extracted PNG sprites from `item.ab`.
- Expanded `assets/items.json` from 82 to 160 catalog entries using the extracted game tables.
- Added names and descriptions in Russian and English for the expanded catalog.
- Added 78 new drawable item resources and removed the older mixed icon set.
- IDs `500014` and `500015` currently use provisional text and a new-style placeholder icon because their exact localized sprite/name pair was not present in the supplied resources.

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
