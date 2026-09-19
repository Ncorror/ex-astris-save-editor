# Release process

The repository builds debug APKs on `main`. A tag matching `v<versionName>` builds a signed release APK and publishes a GitHub Release.

## 1. Create the signing key once

Run in Termux (keep this file backed up somewhere private). If `keytool` is missing, install Java first with `pkg install openjdk-17`:

```bash
mkdir -p ~/.keystores
keytool -genkeypair -v \
  -keystore ~/.keystores/ex-astris-save-editor-release.jks \
  -alias exastris \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Do not add the `.jks` file to Git. If the signing key is lost, future APK updates cannot be signed with the same identity.

## 2. Add GitHub Actions secrets once

From the repository directory in Termux:

```bash
base64 -w 0 ~/.keystores/ex-astris-save-editor-release.jks | gh secret set ANDROID_KEYSTORE_BASE64
gh secret set ANDROID_KEYSTORE_PASSWORD
gh secret set ANDROID_KEY_ALIAS --body "exastris"
gh secret set ANDROID_KEY_PASSWORD

gh secret list
```

The two password commands prompt securely. Use the same key password you entered when creating the keystore unless you intentionally configured a different key password.

Required secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## 3. Validate the release candidate

Before tagging, push `main`, wait for the debug CI build, install that APK, and verify the release checklist in `V1_6_5_VALIDATION.md`.


## Important: debug APK vs release APK

Existing CI/debug APKs are signed with Android's debug key. The public release is signed with your new private release key, so Android will not install it as an update over the old debug-signed app with the same package name. Before the first public release install:

1. copy any editor-local backups you want to keep out of the app-specific `Android/data/com.exa.save` folder;
2. make sure the real Ex Astris save itself is backed up;
3. uninstall the old debug build of Ex Astris Save Editor;
4. install the signed GitHub Release APK.

This affects only the editor app signature. It does not change the Ex Astris game package or its save path.

## 4. Publish v1.6.5

The tag must exactly match `versionName` from `app/build.gradle`.

```bash
cd ~/ex-astris-save-editor/apk

git pull --ff-only
git status

git tag -a v1.6.5 -m "Ex Astris Save Editor v1.6.5"
git push origin v1.6.5

gh run watch
```

The tag workflow will:

1. reject a tag that does not match `versionName`;
2. decode the signing key only inside the GitHub Actions runner;
3. run `assembleRelease`;
4. verify the APK signature with `apksigner`;
5. publish `ex-astris-save-editor-v1.6.5.apk` to GitHub Releases;
6. attach `build-info.txt`, `build.log`, and `release-signature.log`.

Inspect the release:

```bash
gh release view v1.6.5
gh release download v1.6.5
```

## Security notes

- Never commit the keystore or passwords.
- Keep an offline backup of the `.jks` file and remember its passwords.
- Do not publish an unsigned release APK.
- A normal `main` build continues to use the Android debug signing key and is only for testing.
