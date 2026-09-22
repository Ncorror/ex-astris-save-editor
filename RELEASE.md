# Signed release process

Normal pushes to `main` build a **debug** APK. A Git tag matching `v<versionName>` builds a **signed release** APK, verifies it with Android `apksigner`, and publishes a GitHub Release only after both build and signature verification succeed.

Current development version (not yet validated for a signed release):

```text
versionName 1.7.0-dev3
versionCode 23
```

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

From the repository directory:

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

## 3. Validate `main` before tagging

Push the final commit and wait for CI:

```bash
git push origin main
gh run watch
```

Download/check the verification artifact and complete [`TESTING.md`](TESTING.md), especially the add-item flow, save write and bulk-safety checks.

## 4. First release-signed install

Android treats the old CI/debug APK and the new release APK as different signers even though the package name is the same. The first release-signed APK therefore cannot be installed over an older debug-signed installation.

Before uninstalling the debug build:

1. preserve any editor-local backups you need;
2. keep a separate backup of the real Ex Astris save;
3. uninstall only **Ex Astris Save Editor**, not the Ex Astris game;
4. install the signed GitHub Release APK.

Future releases signed with the same `.jks` can update this release-signed installation normally.

## 5. Create the release tag

Read the version directly from Gradle so the tag cannot drift from the project version:

```bash
cd ~/ex-astris-save-editor/apk

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

## 6. What the tag workflow verifies

The tagged build:

1. validates `v<versionName>`;
2. requires all four signing secrets;
3. decodes the keystore only inside the GitHub runner;
4. runs `assembleRelease`;
5. locates Android SDK `apksigner` explicitly;
6. runs `apksigner verify --verbose --print-certs`;
7. prepares build metadata and signature log;
8. publishes a GitHub Release only if build **and** signature verification succeeded.

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

## Security rules

- Never commit or upload the keystore anywhere public.
- Never paste signing passwords into issue/commit/release text.
- Keep an offline keystore backup.
- Do not publish an unsigned APK as an official release.
- Do not bypass a failed `apksigner` verification.
- A `main` debug APK is for testing, not the public signed release.
