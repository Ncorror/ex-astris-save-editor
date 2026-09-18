# Warning cleanup — v1.4.2

> Historical build-cleanup note. The cleaned warning state is retained in v1.5.0.


Cleaned warnings from the successful v1.4.1 build:

- Renamed `onCreate` parameter to `savedInstanceState`.
- Removed unused `ready` parameter from the Shizuku UI helper.
- Replaced deprecated `startActivityForResult`/`onActivityResult` with Activity Result APIs (`OpenDocument` and `CreateDocument`).
- Marked zstd-jni native libraries with `keepDebugSymbols` so AGP does not attempt to strip prebuilt `.so` files.
- Updated GitHub Actions to current Node 24 compatible major versions: checkout v7, setup-java v6, setup-gradle v6, upload-artifact v7, action-gh-release v3.

Functional Root/Shizuku/save logic and item icon behavior were intentionally left unchanged.
