# Setup and update workflow (Termux)

This file describes the repository workflow for Ex Astris Save Editor `1.7.0-dev3`. The relocated skin tab is pending an Android build and UI check.

Repository:

```text
https://github.com/Ncorror/ex-astris-save-editor.git
```

Expected checkout used by the project workflow:

```text
~/ex-astris-save-editor
```

## Requirements

Typical Termux packages:

```bash
pkg update
pkg install git gh unzip openjdk-17 coreutils
```

Authenticate GitHub CLI once if needed:

```bash
gh auth status || gh auth login
```

## Clone the repository

For a fresh checkout:

```bash
cd ~
git clone https://github.com/Ncorror/ex-astris-save-editor.git
cd ~/ex-astris-save-editor
```

Use the repository root, which contains `app/`, `build.gradle`, `settings.gradle` and `.github/`.

## Apply an update archive

The `ex-astris-save-editor-main.zip` update archive has one top-level `ex-astris-save-editor-main/` directory.

Example:

```bash
cd ~
rm -rf ~/exa-update
mkdir -p ~/exa-update

unzip -o /storage/emulated/0/Download/<update-archive>.zip \
  -d ~/exa-update

cd ~/ex-astris-save-editor
cp -a ~/exa-update/ex-astris-save-editor-main/. ./

git status
git diff --stat
```

Before committing, verify the version when the update is supposed to change it:

```bash
grep -E 'versionCode|versionName' app/build.gradle
```

Then commit and push:

```bash
git add -A
git commit -m "Describe the change"
git push origin main

gh run watch
```

If `git status` reports `working tree clean`, the same files are already present in the checkout. Do not create an empty commit just to trigger CI; use `gh workflow run` only when a manual run is actually needed.

## CI artifacts

List/watch recent runs:

```bash
gh run list --limit 10
gh run watch
```

Download the normal build artifact:

```bash
gh run download -n ex-astris-save-editor-build
```

Download verification data:

```bash
gh run download -n ex-astris-save-editor-verification
```

Expected metadata for the current version:

```text
Version: 1.7.0-dev3
Version code: 22
Catalog entries: 160
Item icons: 160
Add item flow: searchable catalog
```

A `main` build is a **debug** build. Public release tags use the private release signing key; see [`RELEASE.md`](RELEASE.md).

## Access testing on a phone

### Root

1. Grant root permission to the editor.
2. Select Root or Auto.
3. Confirm the service reports UID 0.
4. Open the discovered Ex Astris save.
5. Fully close Ex Astris before writing.
6. Edit one obvious value, Save, then verify it in game.

Current real-device verification: APatch / KernelPatch. The libsu implementation is intended to be provider-agnostic, but Magisk, KernelSU and KernelSU Next should be treated as unverified until tested on-device.

### Shizuku

1. Start Shizuku.
2. Grant permission to the editor.
3. Select Shizuku or Auto.
4. Open the discovered save.
5. Confirm the source indicates Shizuku / `Android/data`.
6. Fully close Ex Astris.
7. Make a small edit, Save, then verify it in game.

### Manual file mode

Use File mode when privileged access is unavailable. The Android document picker can open a save and export a copy, but direct automatic `Android/data` discovery is provided by Root/Shizuku.

## Troubleshooting GitHub connectivity

If GitHub commands return `Could not resolve host: github.com`:

```bash
ping -c 1 1.1.1.1
getent hosts github.com
```

If IP connectivity works but DNS lookup fails, restore Android network / Private DNS / VPN connectivity and retry the **same** push. A network error does not mean the local commit must be recreated.

## First repository publication (historical/optional)

Only use this section for a repository that has not yet been created on GitHub:

```bash
cd ~/ex-astris-save-editor

git init -b main
git add -A
git commit -m "Initial Ex Astris Save Editor import"

gh repo create ex-astris-save-editor \
  --public \
  --source=. \
  --remote=origin \
  --push \
  --description "Save file editor for Ex Astris with Root and Shizuku Android/data access"
```

## Release publication

Do not create release tags from this file. The complete, current signing/tag procedure is maintained in [`RELEASE.md`](RELEASE.md).
