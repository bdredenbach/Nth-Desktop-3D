# Signed test APK updates

Starting with **0.3.2 / versionCode 10079**, Nth Desktop 3D uses the owner's
replacement **R2** test-update signing key, created on 2026-11-13 after the
original 0.3.0 key became unavailable. The owner authorized this transition
and the one-time uninstall/reinstall. The application package remains
`com.nth.desktop3d`. Play Store signing is a separate future setup.

Expected R2 certificate SHA-256:

`a78336d0c29334ecfa9d10bdcd13083179d2b31170f85bda0f5170305885ef01`

The public certificate and fingerprint are committed as
`docs/test-update-certificate.pem` and `docs/test-update-certificate.sha256`.
The release packager rejects another certificate, a debuggable APK, an
unexpected package, or a version that disagrees with version.properties.
The private keystore and passwords must never be committed.

## Keep the private phone backup

Keep **Nth-Desktop-3D-Private-Signing-Backup-20261113-R2.zip** on your phone.
It contains the encrypted JKS, private signing.json (including its recovery
passwords), the public certificate and recovery instructions. The owner-only
combined download **Nth-Desktop-3D-0.3.2-APK-and-Private-Key.zip** also contains
that backup alongside the installable APK. This combined ZIP is PRIVATE and
must never be published as a GitHub release or other public download.
Only the APK, public certificate and public verification files may be shared.

When a later chat needs the signing key, attach the R2 backup ZIP or the
combined private download. Restore and reuse that key rather than generating
another one. The APK and certificate fingerprint cannot recover a private key.
Keep the whole backup: the JKS alone does not include its recovery passwords.

## Build the next update

1. Restore the R2 backup outside the repository, keeping all its files together.
2. Increase both values in version.properties for a new release. After 0.3.2 /
   code 10079, use a higher version name and code of at least 10080. Local and
   CI builds read the same file, independently of workflow run numbers.
3. Install Java 17/21 and Android SDK build tools 35.0.0, then set ANDROID_HOME.
   A full local build also needs SDK platform 35 and Gradle dependencies.
4. To build from source, run:

```sh
python3 tools/build-signed-apk.py --signing-config /private/location/signing.json --previous-apk /private/location/Nth-Desktop-3D-0.3.2.apk
```

The optional --previous-apk verifies the same package/certificate and requires
a strictly higher version code. Use the APK from the same R2 backup generation.
Do not compare the first R2 build against the old 0.3.0 key as an in-place update.

The script builds the release, runs release lint and unit tests, verifies its
signature and metadata, and writes the APK, checksum and verification JSON to
dist/. Keep the exact tested application source on main.

Alternatively, after the Android build workflow succeeds, download its exact
unsigned-release artifact, retain the tested commit/artifact metadata, and run:

```sh
python3 tools/build-signed-apk.py --signing-config /private/location/signing.json --unsigned-apk /path/to/app-release-unsigned.apk --previous-apk /path/to/previous-R2.apk
```

This aligns and signs the CI release, then checks identity, version, signature
and release mode. It needs SDK build tools locally but no Gradle rebuild or
GitHub signing secrets. Do not install an unsigned artifact or substitute a
debug APK. When only signing documentation/certificate metadata changed after
the tested application commit, record both the application source commit and
the packaging commit in the verification JSON.

## Optional GitHub Release automation

The GitHub connector cannot configure Actions secrets. An owner can configure
these four secrets with the same R2 key:

| Secret | Private backup value |
| --- | --- |
| NTH_RELEASE_KEYSTORE_BASE64 | One-line base64 of nth-desktop-3d-test.jks |
| NTH_RELEASE_STORE_PASSWORD | store_password in signing.json |
| NTH_RELEASE_KEY_ALIAS | key_alias in signing.json |
| NTH_RELEASE_KEY_PASSWORD | key_password in signing.json |

Do not paste the values in chat or logs. Once configured, commit the next
version and run Actions → Signed APK release → Run workflow on main. It uses
the same packager and tags the exact build commit. An already published
version is not overwritten on rerun. Never upload the private signing backup
or combined owner download as a release artifact.

## One-time transition and future updates

The original 0.3.0 test certificate was:
`0b0a2218cf5127eccdec21eac1eb0b1a3f5b069fdb80d0f17f97aed9f6218eb4`.
Its private key is unavailable and it is superseded by R2. Earlier development
builds also used other certificates; the checked 0.2.76/code 10076 CI APK used:
`fcc7e535b7e2505fab677eb85e33a8f17b8258bd8d1ac5887d7a9e061437c696`.

Android cannot install R2 over those signatures. The owner authorized a
one-time uninstall/reinstall for 0.3.2. Uninstalling removes the old launcher's
settings, layout and widget bindings, so record any layout you want to recreate.
A full .nthdesktop export/import is not implemented in this variant.

After installing R2, keep the same package and key, increase the version code,
and install later APKs as ordinary updates. Do not uninstall or clear data for
normal updates. Physical-device testing remains with the owner.
