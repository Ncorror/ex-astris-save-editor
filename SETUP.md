# Setup and update workflow (Termux)

Repository:

```text
https://github.com/Ncorror/ex-astris-save-editor.git
```

Local checkout used by the project workflow:

```text
~/ex-astris-save-editor/apk
```

All `git` and `gh` commands below must be run inside this directory; outside it they fail with `not a git repository`.

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
mkdir -p ~/ex-astris-save-editor
git clone https://github.com/Ncorror/ex-astris-save-editor.git ~/ex-astris-save-editor/apk
cd ~/ex-astris-save-editor/apk
```

The checkout root contains `app/`, `build.gradle`, `settings.gradle` and `.github/`.

## Update the checkout

Get the latest `main`:

```bash
cd ~/ex-astris-save-editor/apk
git checkout main
git pull --ff-only
```

## Apply an update archive

The `ex-astris-save-editor-main.zip` update archive has one top-level `ex-astris-save-editor-main/` directory.

```bash
rm -rf ~/exa-update
mkdir -p ~/exa-update

unzip -o /storage/emulated/0/Download/<update-archive>.zip \
  -d ~/exa-update

cd ~/ex-astris-save-editor/apk
cp -a ~/exa-update/ex-astris-save-editor-main/. ./

git status
git diff --stat
```

Copying an archive does not remove files that the update deleted; check `git status` and remove them with `git rm` if needed.

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

Download the normal build artifact (APK, `build.log`, `build-info.txt`):

```bash
gh run download -n ex-astris-save-editor-build
```

Download verification data (build log, test results and reports):

```bash
gh run download -n ex-astris-save-editor-verification
```

`build-info.txt` must match the expected metadata in [`TESTING.md`](TESTING.md).

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

Use File mode when privileged access is unavailable. The Android document picker can open a save and export a copy, but automatic `Android/data` discovery and the skin switches require Root or Shizuku.

## Troubleshooting GitHub connectivity

If GitHub commands return `Could not resolve host: github.com`:

```bash
ping -c 1 1.1.1.1
getent hosts github.com
```

If IP connectivity works but DNS lookup fails, restore Android network / Private DNS / VPN connectivity and retry the **same** push. A network error does not mean the local commit must be recreated.

## Release publication

The signing and tag procedure is maintained in [`RELEASE.md`](RELEASE.md).
