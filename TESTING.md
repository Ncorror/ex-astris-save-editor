# Regression and release checklist

Run the relevant sections before treating a new build as stable. For a public release, also complete the **Release gate** at the end.

Current expected metadata:

```text
versionName: 1.8.0-dev
versionCode: 25
catalog entries: 160
item icons: 160
bulk-editable catalog entries: 43
protected catalog entries: 117
```

## 1. Build / CI

- [ ] `assembleDebug` succeeds for a normal `main` build.
- [ ] Build artifact contains an APK, `build.log` and `build-info.txt`.
- [ ] Verification artifact contains `build.log` and `build-info.txt`.
- [ ] No new Kotlin/resource build errors appear.
- [ ] Version in Gradle and `build-info.txt` matches.
- [ ] Catalog count reports `160`.
- [ ] Item-icon count reports `160`.

## 2. Root backend

- [ ] Root permission can be requested.
- [ ] Root service reports UID 0.
- [ ] Save discovery under `Android/data/com.gryphline.exastris.gp` works.
- [ ] `SaveFile0.save` opens.
- [ ] A write succeeds with Ex Astris fully closed.
- [ ] The game sees the edited value after reopening.

Current real-device verification: APatch / KernelPatch.

## 3. Shizuku backend

- [ ] Shizuku permission is granted.
- [ ] Shizuku service connects.
- [ ] Save discovery works.
- [ ] `SaveFile0.save` opens.
- [ ] Backup creation works.
- [ ] A write succeeds with Ex Astris fully closed.
- [ ] The game sees the edited value after reopening.

Shizuku has been verified on a real device.

## 4. Manual file mode

- [ ] Android document picker opens.
- [ ] Selected save parses.
- [ ] Export copy works.
- [ ] Manual write path does not crash.

## 5. Inventory list and search

- [ ] Search matches Russian item names.
- [ ] Search also matches the alternate English name.
- [ ] Numeric ID alone does **not** act as a search key.
- [ ] Category filters work and the selected chip remains visible while scrolling.
- [ ] Search clear icon restores the current category result set.
- [ ] `item_<ID>.png` icons load correctly.
- [ ] Long names do not overlap count/icon UI.
- [ ] Quantity digits are not clipped.
- [ ] Protected items show the lock indicator.
- [ ] Descriptions do not expose raw `<i>`, `<color>` or `[keyword=...]` markup.

Current filter labels:

```text
All
Resources
Consumables
Materials
Entropites
Recipes
Laylah-Keys
Quest items
Valuables
Other (fallback for unknown IDs)
```

Russian UI uses the corresponding localized labels.

## 6. Add-item catalog

- [ ] Tap the floating `+` button; no raw-ID input is requested.
- [ ] Catalog shows only known entries not already present in the save.
- [ ] Search works by Russian and English/alternate name.
- [ ] Visible result count updates with search.
- [ ] Each row shows icon, category and short description.
- [ ] Protected entries show a lock but remain individually addable.
- [ ] Selecting an entry opens quantity confirmation.
- [ ] Quantity cannot be zero.
- [ ] `+` / `-` and quick quantity presets work.
- [ ] Adding an item marks the editor state dirty.
- [ ] The newly added item appears in the inventory.
- [ ] The same catalog ID cannot be inserted twice.

## 7. Individual edit / delete

- [ ] `+` and `-` work in the item editor.
- [ ] Arbitrary typed quantity works.
- [ ] Quick values fit on a narrow phone screen.
- [ ] Protected status is shown in the edit sheet where applicable.
- [ ] Individual editing remains available for protected items.
- [ ] Delete works and marks the state dirty.

## 8. Bulk actions

- [ ] Set works.
- [ ] Add works.
- [ ] Subtract clamps at 0.
- [ ] Arbitrary typed value works.
- [ ] Quick presets set the input; they are not hard limits.
- [ ] Current-search/filter-only works.
- [ ] Preview reports the affected count correctly.
- [ ] Protected/skipped count is correct.
- [ ] Confirmation preference works.
- [ ] Bulk action is disabled when the visible result set has no eligible items.
- [ ] Protected entries never change during a bulk operation.

Current catalog safety totals:

```text
43 editable
117 protected
```

Special regression points:

- [ ] `800000–800011` are shown under Consumables but remain protected.
- [ ] `9910007` Laylah Kernel is shown under Materials but remains protected.
- [ ] `600000–600003` are shown under Valuables.
- [ ] `970xxxx/971xxxx` are shown under Laylah-Keys.
- [ ] `500014` and `500015` remain protected internal records.

## 9. Unsaved-change protection

With autosave **OFF**:

- [ ] Apply/add/delete/bulk edit shows the global unsaved-change bar.
- [ ] Compact save strip reports pending changes.
- [ ] Save-tab badge appears.
- [ ] Snackbar explains that the file has not been written yet.
- [ ] Undo restores the previous in-memory state.
- [ ] Save clears pending indicators.
- [ ] Back with pending edits offers Save / continue without saving / cancel.
- [ ] Save switch with pending edits shows the same protection.
- [ ] Manual file selection with pending edits is protected.
- [ ] Backend switch with pending edits is protected.
- [ ] Backup restore with pending edits is protected.
- [ ] Continue without saving restores the last saved baseline.
- [ ] Failed write keeps pending indicators visible.

With autosave **ON**:

- [ ] A change triggers a write.
- [ ] Successful autosave clears pending state.
- [ ] Failure leaves pending state visible.

## 10. Backups

- [ ] Automatic backup on open works when enabled.
- [ ] Automatic backup on open can be disabled, but every write still creates a verified backup.
- [ ] Simulate an unavailable backup directory: Save must stop without changing the opened file.
- [ ] Change the save externally after opening it: Save must refuse the write and ask for reopening.
- [ ] Try malformed footer length, invalid module offsets and an oversized zstd module: reject without writing or exhausting memory.
- [ ] Manual backup works.
- [ ] Backup list is newest-first.
- [ ] Loading a backup creates a pending editor state until saved.

## 11. Catalog integrity

- [ ] `items.json` contains exactly 160 entries.
- [ ] `drawable-nodpi` contains exactly 160 `item_*.png` files.
- [ ] Russian locale shows Russian display names/descriptions.
- [ ] Non-Russian locale shows English display names/descriptions.
- [ ] No blank display name exists in the catalog.
- [ ] No blank description exists in the catalog.
- [ ] IDs `500014` and `500015` remain clearly marked as provisional/internal rather than presented as confirmed normal obtainable Entropiths.

## 12. Arknights skins

Run when the skin switches, their bundles or the privileged file service change. Test both Root and Shizuku on a device, starting from the game's original map files.

- [ ] Bottom navigation shows four labeled tabs: Items, Save, Skins and Settings (including Russian labels).
- [ ] Skin controls appear on the Skins tab only.
- [ ] Opening Skins refreshes both bundle states; returning to the foreground on Skins refreshes them again.
- [ ] Unsaved save edits still show the Save-tab badge, and switching tabs does not discard them.
- [ ] With the game fully closed, detect stock Hime and MsBlack independently.
- [ ] Enable Hime; the status shows the Arknights skin. Check scene, dialogue, battle, animation and effects in game.
- [ ] Disable Hime; the status shows the standard skin and the stock model returns in game.
- [ ] Enable MsBlack; check scene, dialogue and battle in game.
- [ ] Disable MsBlack; the stock model returns in game.
- [ ] Verify the other character remains unchanged throughout each switch.
- [ ] A file that is not a recognized character map (for example, the other character's file renamed) is shown as unrecognized and cannot be switched.
- [ ] Confirm a failed backup stops the write, an interrupted write preserves the old file, and a successful write can be read back.
- [ ] Confirm a still-running game is closed before replacing bundles.
- [ ] Verify backup restoration and behavior after a game content update.

## 13. Release gate

Before tagging (see [`RELEASE.md`](RELEASE.md)):

- [ ] `versionName` has no `-dev` suffix and `versionCode` was increased.
- [ ] `CHANGELOG.md` has a section for the version.
- [ ] `RELEASE_NOTES_v<versionName>.md` exists and uses full URLs for links.
- [ ] `main` CI is green.
- [ ] Phone smoke test passes on a known-good backup/save.
- [ ] Release keystore is backed up privately.
- [ ] Required GitHub Actions signing secrets exist.

After pushing tag `v<versionName>`:

- [ ] Tag exactly matches `versionName`.
- [ ] Workflow reports `Build variant: release` and `Signing: yes`.
- [ ] `assembleRelease` succeeds.
- [ ] `release-signature.log` exists and `apksigner verify` reports `Verifies` with a valid v2 signature.
- [ ] GitHub Release is published only after signature verification succeeds.
- [ ] Release contains the versioned APK, build log, build info and signature log.
- [ ] Release description matches `RELEASE_NOTES_v<versionName>.md`.
- [ ] The release APK installs over the previous release as an update.
