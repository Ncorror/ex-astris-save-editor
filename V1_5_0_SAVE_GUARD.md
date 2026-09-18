# v1.5.0 — persistent unsaved-change protection

## Problem

Before v1.5, manual editing had a valid but easy-to-forget two-step flow:

1. Apply an edit in the item sheet.
2. Navigate to Save and write the file.

A user could correctly change values in memory, forget the second step, and later assume the game file had already changed.

## Solution

v1.5.0 makes pending writes visible globally instead of only on the Save page.

### Persistent guard bar

While the in-memory editor differs from the last successfully written state, a card appears immediately above the bottom navigation on every page. It shows:

- pending item count;
- active save filename;
- Undo;
- Save.

### Save-tab badge

The Save navigation item receives a numeric badge while edits are pending.

### Post-edit notice

After Apply, Add, Delete, Restore, or a bulk mutation, a Snackbar explicitly states that the editor state changed but the `.save` file has not yet been written. The Snackbar includes a Save action.

### One-step Undo

Before each supported mutation, the editor captures a complete in-memory save snapshot. Until the next successful write, the user can undo the most recent mutation. The implementation restores a `SaveFile` snapshot instead of attempting to reverse individual byte edits.

### Destructive-navigation guard

Pending edits trigger a three-choice dialog before:

- closing the activity with Back;
- switching to another save slot;
- opening a manual file;
- switching the access backend;
- loading a backup.

Choices:

- **Save and continue** — write first, continue only after success.
- **Continue without saving** — reconstruct the last saved snapshot in memory, then continue.
- **Cancel** — stay in the current editor state.

### Baseline snapshot

The editor keeps both:

- `baselineItems` for fast changed-item counts;
- `baselineSnapshot` for a complete rollback to the last opened/successfully saved file state.

A successful write refreshes both baselines. A failed write does not clear pending-change state.

## Autosave

Autosave remains optional and disabled by default. When enabled, the same mutation path marks the editor dirty and immediately writes the save. Manual mode remains recommended for safety because Ex Astris should be fully closed while the file is being overwritten.

## Non-goals

- This is not a multi-level undo history.
- The guard does not attempt to save when Android force-kills the process.
- The guard does not make it safe to edit while Ex Astris is actively writing its own save.


> Superseded by v1.5.1 for bulk-safety/search/save-bar UI details. See `V1_5_1_BULK_SEARCH_UI.md`.
