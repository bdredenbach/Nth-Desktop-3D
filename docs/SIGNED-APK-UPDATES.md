# Signed update-in-place APKs

The normal `Android build` workflow still builds debug APKs for development and emulator tests. Debug APKs from different runners may have different certificates, so do not use them for repeatable in-place updates.

## One-time setup (keep the keystore private)

On a trusted local machine with Java keytool:

```sh
keytool -genkeypair -v -keystore nth-desktop-3d-release.jks -alias nthdesktop3d -keyalg RSA -keysize 3072 -validity 10000
```

**Back up the JKS file and passwords securely.** Losing the signing key prevents future in-place updates to builds signed by that key. Never commit the keystore or passwords to this public repository, and never paste them into chat.

In repository Settings → Secrets and variables → Actions, create these repository secrets:

- `NTH_RELEASE_KEYSTORE_BASE64`: one-line base64 encoding of the entire JKS file (e.g. `base64 -w0 nth-desktop-3d-release.jks` on Linux; on macOS `base64 < nth-desktop-3d-release.jks | tr -d '\\n'`)
- `NTH_RELEASE_STORE_PASSWORD`: keystore password
- `NTH_RELEASE_KEY_ALIAS`: `nthdesktop3d` (or the alias chosen)
- `NTH_RELEASE_KEY_PASSWORD`: private key password

Then go to Actions → **Signed APK release** → **Run workflow**. The workflow verifies the signing configuration, builds and verifies a signed APK, and publishes it as a GitHub Release asset with a direct APK link.

`applicationId` remains `com.nth.desktop3d`. CI assigns a monotonically increasing `versionCode` based on the workflow run number (10000 + run number). Every new signed release uses the same certificate.

### First transition

An existing **debug-signed** Nth Desktop 3D cannot be replaced by a release signed with a different certificate. The first migration may require uninstalling the old app; export or back up launcher data first where supported. Uninstalling deletes private app data and can invalidate widget bindings. Subsequent APKs from the stable release workflow can install using Android's **Update** option, preserving app data and widget bindings as long as the package, certificate and version progression remain consistent.

### Safety

Do not generate a new keystore for each release. Never upload the keystore to public artifacts. If a release fails before publishing, inspect the workflow logs without exposing signing secrets.
