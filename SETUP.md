# Publish and build from Termux

Assuming the project is in `~/ex-astris-save-editor/apk`:

```bash
cd ~/ex-astris-save-editor/apk

git init -b main
git add -A
git commit -m "Add Shizuku Android/data access and dark UI"

gh auth status || gh auth login

gh repo create ex-astris-save-editor \
  --public \
  --source=. \
  --remote=origin \
  --push \
  --description "Save file editor for Ex Astris with Shizuku Android/data access"

gh repo edit \
  --add-topic android \
  --add-topic kotlin \
  --add-topic save-editor \
  --add-topic ex-astris \
  --add-topic shizuku \
  --add-topic zstd \
  --add-topic reverse-engineering \
  --add-topic file-format \
  --add-topic game-tools

gh run watch
gh run download -n ex-astris-save-editor
```

If the repository already exists, commit and push only:

```bash
cd ~/ex-astris-save-editor/apk
git add -A
git commit -m "Add Shizuku Android/data access and dark UI"
git push
gh run watch
```

## On the phone

Install and start Shizuku first. On Android 11+ the non-root method uses Wireless debugging. Then open the editor, grant Shizuku permission, and use **Open Ex Astris save**.

Always fully close Ex Astris before overwriting the save.

## Build logs

On a successful GitHub Actions run, download the APK and its build logs together:

```bash
gh run download -n ex-astris-save-editor
```

The downloaded artifact contains the APK, `build.log`, and `build-info.txt`.

If the build fails, download the diagnostic artifact instead:

```bash
gh run download -n ex-astris-save-editor-build-logs
```

For GitHub Release assets listed separately under the APK, push a version tag:

```bash
git tag v1.1.1
git push origin v1.1.1
```

The tag workflow attaches the APK, `build.log`, and `build-info.txt` as separate Release files.
