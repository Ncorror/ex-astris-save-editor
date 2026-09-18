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
