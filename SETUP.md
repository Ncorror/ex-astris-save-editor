# Publishing this repository from Termux

```bash
pkg install git gh

git config --global user.name  "your-name"
git config --global user.email "you@example.com"

cd /path/to/this/folder
git init -b main
git add -A
git commit -m "Ex Astris save editor: format parser, editor UI, CI build"

gh repo create ex-astris-save-editor \
  --public --source=. --remote=origin --push \
  --description "Save file editor for Ex Astris: parses the zstd module container, edits backpack items, builds an APK in CI"

gh repo edit --add-topic android,kotlin,save-editor,ex-astris,zstd,odin-serializer,reverse-engineering,file-format,game-tools
```

Watch the build and pull the APK straight to the phone:

```bash
gh run watch
gh run download -n ex-astris-save-editor
```

Cut a release with the APK attached:

```bash
git tag v1.0
git push origin v1.0
```

Later changes:

```bash
git add -A && git commit -m "what changed" && git push
```
