# Regression testing checklist

Run this checklist before treating a new archive as stable.

## Build

- [ ] `assembleDebug` succeeds.
- [ ] APK artifact exists.
- [ ] verification artifact contains `build.log` and `build-info.txt`.
- [ ] no new Kotlin/resource warnings.
- [ ] version in APK/Gradle/build-info matches.

## Root

- [ ] Root permission can be requested.
- [ ] Root service reports UID 0.
- [ ] `Android/data/com.gryphline.exastris.gp` save discovery works.
- [ ] main save opens.
- [ ] write succeeds with game closed.
- [ ] game sees the edited value.

Current real-device verification: APatch / KernelPatch.

## Shizuku

- [ ] Shizuku permission is granted.
- [ ] Shizuku service connects.
- [ ] save discovery works.
- [ ] `SaveFile0.save` opens.
- [ ] backup works.
- [ ] write succeeds with game closed.
- [ ] game sees the edited value.

Shizuku has been verified on a real device.

## Manual file mode

- [ ] document picker opens.
- [ ] selected save parses.
- [ ] export copy works.
- [ ] manual write does not crash.

## Unsaved-change protection

With autosave OFF:

- [ ] Apply an item change -> global guard bar appears.
- [ ] compact save strip shows changed count.
- [ ] Save tab badge appears.
- [ ] Snackbar says the file is not written yet.
- [ ] Undo restores the previous value.
- [ ] Save clears the bar and badge.
- [ ] Back with pending edits shows 3-choice dialog.
- [ ] Switch save with pending edits shows 3-choice dialog.
- [ ] Manual file selection with pending edits shows 3-choice dialog.
- [ ] Access backend switch with pending edits shows 3-choice dialog.
- [ ] Backup restore with pending edits shows 3-choice dialog.
- [ ] Continue without saving actually restores the last saved baseline.
- [ ] failed write keeps the pending-change indicators visible.

With autosave ON:

- [ ] Apply triggers a write.
- [ ] successful autosave clears pending state.
- [ ] failure leaves pending state visible.

## Inventory editor

- [ ] search works by name and ID.
- [ ] category filters work.
- [ ] item icons load by `item_<ID>`.
- [ ] quantity digits are not clipped.
- [ ] plus/minus work.
- [ ] arbitrary typed quantity works.
- [ ] Add-by-ID works.
- [ ] Delete works.

## Bulk actions

- [ ] Set works.
- [ ] Add works.
- [ ] Subtract clamps at 0.
- [ ] arbitrary value works.
- [ ] quick presets only set the input; they are not hard limits.
- [ ] current-filter-only works.
- [ ] affected-entry preview is correct.
- [ ] confirmation toggle works.

## Backups

- [ ] automatic backup on open works when enabled.
- [ ] manual backup works.
- [ ] backup list sorts newest first.
- [ ] loaded backup becomes a pending editor change until saved.

## v1.5.1 regression checks

### Bulk safety
1. Open a save containing normal stackable items plus Entropith/unique items.
2. Run **Bulk -> All editable -> Set 1000**.
3. Confirm the preview reports editable items separately from protected/skipped items.
4. Confirm Entropiths and Other/unique items retain their previous quantities.
5. Repeat with Add and Subtract.
6. Filter to Entropiths or Other, open Bulk, and confirm the visible-only preview has zero editable targets and Apply is disabled.

### Search
1. Search a known item by its Russian name.
2. Search by its English/alternate name.
3. Type the numeric ID alone and confirm it does not produce an ID-based match.
4. Confirm multi-word name searches work regardless of extra spaces.

### Unsaved-change bar
1. Edit one item and Apply.
2. Confirm a compact one-line bar appears above navigation.
3. Confirm the Save tab shows only a small dot badge, not a large count bubble.
4. Confirm Undo restores the last editor state.
5. Confirm Save writes the file and hides the bar/badge.
6. Confirm the top access card does not show a duplicate Save button while Root/Shizuku is connected.


## v1.5.2 catalog regression checks

- Existing 69 item names/icons render exactly as before.
- New IDs `12005`, `12007`, `12008`, `12009`, `210012`, `500001`, `710002`, `710004`, `710005`, `720005`, `730002`, `800006`, `9710017` resolve to names and icons.
- Total catalog count is 82.
- Total `item_*.png` resource count is 82.
- New Entropith/Other IDs are skipped by bulk editing.
- Name-only search can find the new Russian names.


## v1.5.3 bilingual catalog regression

- [ ] Russian locale shows Russian item names and descriptions.
- [ ] English/non-Russian locale shows English item names and descriptions.
- [ ] Search finds items by both Russian and English names.
- [ ] All 82 catalog entries open without blank description text.
- [ ] Unverified Amber/Entropith/special-item entries do not claim a specific official effect.
- [ ] Entropith, relic/special and bottle-crate entries remain excluded from bulk quantity editing.
- [ ] Known stack-safe materials/consumables remain available for bulk editing.

## v1.6.1 category-filter regression

- [ ] The inventory filter row scrolls horizontally and exposes: All, Resources, Consumables, Materials, Entropiths, Recipes, Tritris, Quest items, Curiosities, Packs, Other.
- [ ] Selecting each filter only shows items from that category.
- [ ] Known catalog entries no longer fall into Other solely because the old UI lacked a dedicated category.
- [ ] Unknown save IDs still appear under Other.
- [ ] Bulk actions remain available only for Resources, Consumables and Materials.
- [ ] Entropiths, Recipes, Tritris, Quest items, Curiosities, Packs and Other are never mass-edited.
- [ ] Search by item name continues to work while a category filter is active.
