# v1.6.5 release CI fix

The first tagged v1.6.5 run successfully completed `assembleRelease` and loaded all four signing secrets, but failed while verifying the APK because `apksigner` was not available through the runner's shell `PATH`.

This patch:

- resolves `apksigner` from `$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner`;
- falls back to the newest executable `apksigner` found below `$ANDROID_SDK_ROOT/build-tools`;
- records the actual verifier path/version in the job log;
- preserves verification artifacts even when signature verification fails;
- blocks GitHub Release publication unless both the Gradle release build and APK signature verification succeed;
- makes the final job status fail when release signature verification fails.

No application source, catalog data, icons, signing keys, passwords, or version numbers are changed by this patch.
