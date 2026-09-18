# v1.4.0 development notes

> Historical v1.4 development notes. Current behavior is documented in `README.md` and `V1_5_0_SAVE_GUARD.md`.


## UX changes

- Item edit sheet now has an alternate-language name, a short purpose/usage description, and an explicit knowledge-status label.
- Search matches numeric ID, Russian/English names and known descriptions.
- Quantity shortcuts are labeled as shortcuts; any value can still be typed directly.
- Bulk actions support:
  - all items or one category;
  - Set / Add / Subtract;
  - any typed value;
  - quick values 100 / 999 / 10,000 / 100,000 / 999,999;
  - optional restriction to the currently visible search/filter result;
  - preview of the number of affected entries;
  - optional confirmation.
- Pending edits now show the number of changed item entries.
- While edits are pending, the compact status strip exposes a one-tap Save action, so saving no longer requires navigating to the Save tab.
- Optional automatic save remains available in Settings; manual save remains the safer default.
- Save-file discovery is naturally ordered (`SaveFile0`, then `AutoSaveFile0`, `AutoSaveFile1`, ...).
- The last privileged save is reopened automatically, while tapping **Switch save** forces the slot picker so the user can still change slots.

## Item research

See `ITEM_RESEARCH.md`. Public item references expose names and effects but generally do not expose the internal numeric save IDs. The editor therefore distinguishes between sourced purpose text and still-preliminary ID/name mappings.

## Validation performed before packaging

- All Android XML resource files parse successfully.
- Both English and Russian string tables contain every `@string` / `R.string` reference found by the static check.
- `assets/items.json` parses successfully and contains 69 catalog entries.
- Kotlin parser check reports no syntax/parser errors. Android/AndroidX symbols cannot be type-resolved in this container because the Android SDK is not installed; GitHub Actions remains the final compile check.
