# Ex Astris Save Editor — build / update from Termux

Current development version: **1.5.2**.

Repository:

```text
https://github.com/Ncorror/ex-astris-save-editor.git
```

## Apply a project archive to the existing checkout

Example:

```bash
cd ~
rm -rf ~/exa-update
mkdir -p ~/exa-update

unzip -o /storage/emulated/0/Download/ex_astris_save_editor_v150_save_guard.zip \
  -d ~/exa-update

cd ~/ex-astris-save-editor/apk
cp -a ~/exa-update/. ./

git status
git add -A
git commit -m "Add persistent unsaved change protection"
git push origin main

gh run watch
```

Do not create a second commit if `git push` fails only because DNS/network is unavailable. The local commit already exists; restore connectivity and run:

```bash
git push origin main
```

## Download CI artifacts

```bash
gh run list
gh run watch

gh run download -n ex-astris-save-editor-debug
gh run download -n ex-astris-save-editor-verification
```

The debug artifact contains:

```text
ex-astris-save-editor.apk
build.log
build-info.txt
```

The verification artifact contains build diagnostics and reports when available.

## First publication only

```bash
cd ~/ex-astris-save-editor/apk

git init -b main
git add -A
git commit -m "Ex Astris save editor"

gh auth status || gh auth login

gh repo create ex-astris-save-editor \
  --public \
  --source=. \
  --remote=origin \
  --push \
  --description "Save file editor for Ex Astris with Root and Shizuku Android/data access"
```

## Release tag

For a v1.5.2 release:

```bash
git tag v1.5.2
git push origin v1.5.2
```

The tag workflow attaches the APK, `build.log`, and `build-info.txt` to the GitHub Release.

## Access testing on phone

### Root

Root is implemented with libsu and is intended to work with normal `su` providers. APatch / KernelPatch is already verified on a real device. Magisk, KernelSU and KernelSU Next should be tested separately before being documented as verified.

Expected state:

```text
Root connected
UID 0
Android/data available
```

### Shizuku

1. Start Shizuku.
2. Grant the app permission.
3. Select Shizuku or Auto.
4. Open the game save.
5. Verify file source says `Shizuku · Android/data`.
6. Edit one obvious value.
7. Fully close Ex Astris before writing.
8. Save and verify the changed value inside the game.

## Unsaved-change guard test

With autosave disabled:

1. Edit one item and tap Apply.
2. Confirm the persistent warning bar appears above the bottom navigation.
3. Confirm the Save tab gets a numeric badge.
4. Tap Undo and verify the value returns.
5. Edit again.
6. Try to switch save files; verify the Save / Continue without saving / Cancel dialog appears.
7. Repeat for backend switching and app Back.
8. Save and confirm the bar + badge disappear.

## Item icons

Place images in:

```text
app/src/main/res/drawable-nodpi/
```

Use:

```text
item_<ID>.png
```

or:

```text
item_<ID>.webp
```

No Kotlin edit is required; the adapter resolves resources by item ID.

## Network/DNS troubleshooting

If GitHub commands return `Could not resolve host: github.com`:

```bash
ping -c 1 1.1.1.1
getent hosts github.com
```

If IP connectivity works but the hostname does not resolve, restore Android network / Private DNS / VPN connectivity and retry the push. Do not re-copy the project and do not make another commit solely for this error.


> Superseded by v1.5.1 for bulk-safety/search/save-bar UI details. See `V1_5_1_BULK_SEARCH_UI.md`.


### v1.5.2 catalog check

After installing v1.5.2, verify that the new save-file IDs appear when present and that the 13 new icons render without replacing any older icon. Expected catalog metadata count: **82**.
