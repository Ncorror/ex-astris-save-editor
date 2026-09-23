# Signed release process

Normal pushes to `main` build a **debug** APK. A Git tag matching `v<versionName>` builds a **signed release** APK, verifies it with Android `apksigner`, and publishes a GitHub Release only after both build and signature verification succeed.

Run all commands from the local checkout (`~/ex-astris-save-editor/apk`, see [`SETUP.md`](SETUP.md)).

## 1. Create the signing key once

If Java is not installed in Termux:

```bash
pkg install openjdk-17 coreutils
```

Create the release keystore:

```bash
mkdir -p ~/.keystores

keytool -genkeypair -v \
  -keystore ~/.keystores/ex-astris-save-editor-release.jks \
  -alias exastris \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

The certificate identity fields are descriptive; they do not have to match a company registration. Keep the keystore password private.

**Never commit the `.jks` file.** Keep at least one offline backup. Losing this key prevents future APKs from being signed with the same app identity.

## 2. Configure GitHub Actions secrets once

```bash
cd ~/ex-astris-save-editor/apk

base64 -w 0 ~/.keystores/ex-astris-save-editor-release.jks \
  | gh secret set ANDROID_KEYSTORE_BASE64

gh secret set ANDROID_KEYSTORE_PASSWORD
gh secret set ANDROID_KEY_ALIAS --body "exastris"
gh secret set ANDROID_KEY_PASSWORD

gh secret list
```

Required secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The password commands prompt without storing the values in shell history. If the key password is the same as the keystore password, enter the same value for both secrets.

## 3. Prepare the release commit

1. In `app/build.gradle`, set the final `versionName` (no `-dev` suffix) and increase `versionCode`.
2. In [`CHANGELOG.md`](CHANGELOG.md), collect the development entries under a `## <versionName>` heading.
3. Add `RELEASE_NOTES_v<versionName>.md` at the repository root. It becomes the GitHub Release description, so use full `https://github.com/...` URLs for links; relative links do not work there. Remove the previous version's notes file.
4. Update the version in [`README.md`](README.md) and the expected metadata in [`TESTING.md`](TESTING.md).

## 4. Validate `main` before tagging

Push the release commit and wait for CI:

```bash
git push origin main
gh run watch
```

Download/check the verification artifact and complete [`TESTING.md`](TESTING.md), especially the add-item flow, save write, bulk-safety and skin checks.

## 5. Create the release tag

Read the version directly from Gradle so the tag cannot drift from the project version:

```bash
cd ~/ex-astris-save-editor/apk

git checkout main
git pull --ff-only
git status

VERSION="$(sed -n 's/.*versionName[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle | head -n 1)"
TAG="v${VERSION}"

echo "$TAG"
git tag -a "$TAG" -m "Ex Astris Save Editor $TAG"
git push origin "$TAG"

gh run watch
```

The workflow rejects a tag that does not exactly match `versionName`.

If a tag must be recreated because the workflow file itself was fixed **before the public release is considered final**, delete and recreate only that tag intentionally:

```bash
git tag -d "$TAG"
git push origin ":refs/tags/$TAG"
git tag -a "$TAG" -m "Ex Astris Save Editor $TAG"
git push origin "$TAG"
```

Do not routinely rewrite already published release tags.

If pushing the tag did not start a build (this happened once right after the repository was recreated), start it manually for the tag instead of recreating it:

```bash
gh workflow run build.yml --ref "$TAG"
gh run watch
```

A manual run publishes the release and sets its description the same way as a tag push.

## 6. What the tag workflow does

The tagged build:

1. validates `v<versionName>`;
2. requires all four signing secrets;
3. decodes the keystore only inside the GitHub runner;
4. runs the unit tests and `assembleRelease`;
5. locates Android SDK `apksigner` explicitly;
6. runs `apksigner verify --verbose --print-certs`;
7. prepares build metadata and signature log;
8. publishes a GitHub Release only if build **and** signature verification succeeded;
9. sets the release description from `RELEASE_NOTES_v<versionName>.md` when that file exists (also for manual `gh workflow run` builds of a tag).

A successful `main` build also re-syncs the description of an existing `v<versionName>` release from that file, so later edits to the notes reach the GitHub Release after a push to `main`.

A valid APK Signature Scheme **v2** result is sufficient for the current Android target. The workflow does not require v1/v3/v4 to be true.

## 7. Inspect/download the release

```bash
VERSION="$(sed -n 's/.*versionName[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle | head -n 1)"
TAG="v${VERSION}"

gh release view "$TAG"
gh release view "$TAG" --web
gh release download "$TAG"
```

Expected release assets:

```text
ex-astris-save-editor-v<version>.apk
build.log
build-info.txt
release-signature.log
```

## Upgrading from a debug build

Releases since `v1.6.5` are signed with the same release key and install over each other as normal updates.

Android treats CI debug APKs and release APKs as different signers, so a release APK cannot be installed over a debug-signed installation. Before switching:

1. preserve any editor-local backups you need;
2. keep a separate backup of the real Ex Astris save;
3. uninstall only **Ex Astris Save Editor**, not the Ex Astris game;
4. install the signed GitHub Release APK.

## Security rules

- Never commit or upload the keystore anywhere public.
- Never paste signing passwords into issue/commit/release text.
- Keep an offline keystore backup.
- Do not publish an unsigned APK as an official release.
- Do not bypass a failed `apksigner` verification.
- A `main` debug APK is for testing, not the public signed release.
