# Exa Save Editor

An Android app for reading and editing **Ex Astris** local save files.

Ex Astris is a single-player offline RPG by Hypergryph / Nous Wave. This tool only touches
your own save file on your own device: it lists what is in your backpack, changes item
counts, and writes the file back.

## What it does

- Reads the local `SaveFile0.save` and `AutoSaveFileN.save`
- Lists every backpack entry with counts and human-readable names from a catalog
- Edits counts, one item at a time or in bulk by category
- Adds and removes entries by item id
- Writes the file back, preserving the container layout byte for byte where nothing changed

## Getting the APK without Android Studio

1. Push this repository to your own GitHub account.
2. Open the **Actions** tab. The build starts on every push; you can also pick
   *build apk* on the left and press **Run workflow**.
3. When the run finishes, download the **ex-astris-save-editor** artifact from the bottom of the
   run page. It contains `app-debug.apk`.
4. Install it — you will need to allow installation from unknown sources.

Pushing a tag like `v1.0` additionally creates a GitHub Release with the APK attached.

The APK is signed with a debug key. That is fine for personal use and not suitable for
distribution through any store.

## Using the app

The game stores saves at:

```
/storage/emulated/0/Android/data/com.gryphline.exastris.gp/files/<account id>/Save/SaveFile0.save
```

The middle folder is the account id, so it differs per profile.

Since Android 11 the system file picker refuses to open anything inside `Android/data`, and
that restriction applies to every third-party app. The workflow is therefore:

1. Copy `SaveFile0.save` to `Download` with a file manager.
2. Open that copy in the app, edit, press **Save**.
3. **Fully close the game** — swipe it away from recents, do not just minimise it.
4. Copy the file back into the game folder.

Step 3 matters: a running game rewrites the save from memory and silently discards the edits.

Before the first edit the app copies the original file into its own sandbox at
`Android/data/com.exa.save/files/SaveFile0.<timestamp>.bak`.

## Save format

Everything below was worked out by inspecting real save files and verified against the
running game.

```
[module 0][module 1]...[SaveFileTail][30-byte trailer]
```

- Every module is compressed as a **separate zstd frame**. A typical save holds 36 to 58 of
  them; the exact number depends on which scenes were loaded when the game saved.
- `SaveFileTail` sits at the end uncompressed. It is an Odin Serializer stream holding
  `key`, `length` and `fileOffset` for every module, and those offsets point at
  **compressed** bytes.
- Trailer, exactly 30 bytes:

| Offset | Type | Meaning |
|---|---|---|
| 0 | u64 | save timestamp, unix seconds |
| 8 | u64 | `SaveFileTail` length |
| 16 | u32 | constant 4, looks like a format version |
| 20 | u8 | constant 1 |
| 21 | u8 | compression flag: 1 = modules compressed, 0 = stored raw |
| 22 | u64 | `dataLen` = compressed modules + tail length |

Invariant: `dataLen + 30 == file size`. Both compression modes load fine in the game.

Module payloads are **Odin Serializer** (Sirenix) binary streams with UTF-16LE strings.
The backpack lives in `GameLogic.BackpackModuleSaveData` as a
`Dictionary<uint, BackpackItemState>`, where the value struct is just `ItemId` and `Num`.

Array layout:

```
06 <u64 entry count>
<entry 0>     198 bytes, declares the element type by name
<entry 1..n>  75 bytes each
07 05 05
```

Offsets inside a 75-byte entry: key at 12, reference node id at 31, item id at 53,
count at 69.

### Two traps

- **Do not merge modules into one zstd frame.** The file still decompresses, but every
  `fileOffset` in the tail becomes wrong. In game this looks like the loading animation
  starting and immediately dropping you back to the save list.
- **Do not find entry boundaries by scanning for bytes.** `0x07` and `0x04 0x2e` occur
  inside values as well — give an item a count of 7 and a naive scan breaks. Walk the
  structure field by field instead.

## Localization

The UI ships in English and Russian and follows the system language: `res/values/strings.xml`
and `res/values-ru/strings.xml`. The item catalog carries both `name` and `name_en` for every
entry, and category keys are language neutral (`material`, `consumable`, ...), so adding
another language means one more `values-xx` folder plus one more name field.

## Repository layout

```
app/src/main/java/com/exa/save/SaveFile.kt      container parsing, editing, rebuilding
app/src/main/java/com/exa/save/MainActivity.kt  UI: list, edit dialogs, bulk actions
app/src/main/assets/items.json                  item catalog: id -> name, name_en, category
app/src/main/res/values/strings.xml             English UI strings
app/src/main/res/values-ru/strings.xml          Russian UI strings
.github/workflows/build.yml                     CI build, artifact and tagged release
```

## Disclaimer

Not affiliated with Hypergryph or Nous Wave. Ex Astris is a paid single-player game with no
multiplayer and no leaderboards; this is a personal tool for offline save files. Keep a
backup before editing anything.

MIT licensed, see `LICENSE`.
