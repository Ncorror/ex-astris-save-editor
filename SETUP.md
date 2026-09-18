# Publish and build from Termux

If the repository already exists locally, copy the updated project over it and push:

```bash
cd ~/ex-astris-save-editor/apk

git add -A
git commit -m "Add Root access and unified Android data backends"
git push

gh run watch
```

After a successful run:

```bash
gh run download -n ex-astris-save-editor-debug
```

The artifact contains:

```text
ex-astris-save-editor.apk
build.log
build-info.txt
```

The second Actions artifact is:

```text
ex-astris-save-editor-verification
```

It keeps build diagnostics and reports.

## First publication

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

## Item icons

Put item art in:

```text
app/src/main/res/drawable-nodpi/
```

Name each image after its item ID:

```text
item_10000.webp
item_11001.webp
item_200010.webp
```

No Kotlin changes are required for new icons; the adapter resolves them automatically.

## On the phone

Choose an access mode in Settings. Root mode uses the device SuperUser manager; Shizuku mode uses Shizuku (Wireless debugging on Android 11+ for non-root setups). Manual mode needs neither.

Always fully close Ex Astris before overwriting its save.

## Release assets

For a GitHub Release with the APK and build files listed separately:

```bash
git tag v1.3.0
git push origin v1.3.0
```

The tag workflow attaches the APK, `build.log`, and `build-info.txt` to the Release.
