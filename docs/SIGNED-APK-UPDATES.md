# Signed test APK updates

Starting with **0.3.0 / versionCode 10077**, Nth Desktop 3D uses one permanent,
dedicated test-update signing key. The package remains `com.nth.desktop3d`.
The key is for these sideloaded test builds; Play Store signing is a separate
future setup owned by the project owner.

Expected certificate SHA-256:

`0b0a2218cf5127eccdec21eac1eb0b1a3f5b069fdb80d0f17f97aed9f6218eb4`

The exact value is pinned in `docs/test-update-certificate.sha256`. The release
packager rejects another certificate, a debuggable APK, an unexpected package,
or a version that disagrees with the committed source.

## Keep the private recovery backup

The owner recovery file is **Nth-Desktop-3D-Private-Signing-Backup-20261113.zip**.
It contains the JKS, private `signing.json`, a public certificate and recovery
instructions. Keep a private copy. It must never be committed to Git, attached
to a GitHub issue, or included in a public APK/download archive. Losing the key
prevents future updates to this signing identity. Future development sessions
must retrieve this existing backup rather than create another key.

## Build the next update locally

1. Restore that backup outside the repository.
2. Increase both values in `version.properties` before a new release. Codes must
   always rise; after 10077, use at least 10078. Local and CI builds read the same
   file, independently of workflow run numbers.
3. Install Java 17/21, Android SDK platform 35 and build tools 35.0.0, and set
   `ANDROID_HOME`.
4. From the repository, run:

```sh
python3 tools/build-signed-apk.py --signing-config /private/location/signing.json --previous-apk /private/location/Nth-Desktop-3D-0.3.0.apk
```

The optional `--previous-apk` also checks the prior signature/package and requires
a strictly higher version code. The script builds the release, runs release lint
and unit tests, verifies the signature and metadata, and writes the named APK,
checksum and verification JSON under `dist/`. Only those public outputs should
be shared. Keep the exact tested source commit on `main`.

Alternatively, after the normal **Android build** workflow succeeds, download
that commit's **unsigned-release** artifact and use the same source checkout:

```sh
python3 tools/build-signed-apk.py --signing-config /private/location/signing.json --unsigned-apk /path/to/app-release-unsigned.apk --previous-apk /path/to/previous.apk
```

This uses Android build tools to align/sign the CI-built release, then performs
the same identity/version/signature checks. The CI workflow already builds,
lints and runs unit tests on the release source. Do not install the unsigned
artifact or substitute the debug artifact. This route needs only SDK build
tools locally and does not require GitHub signing secrets.

## Optional GitHub Release automation

The GitHub connector cannot configure Actions secrets. An owner with permission
can configure these repository secrets using the **same backed-up key**:

| Secret | Private backup value |
| --- | --- |
| `NTH_RELEASE_KEYSTORE_BASE64` | One-line base64 of `nth-desktop-3d-test.jks` |
| `NTH_RELEASE_STORE_PASSWORD` | `store_password` in signing.json |
| `NTH_RELEASE_KEY_ALIAS` | `key_alias` in signing.json |
| `NTH_RELEASE_KEY_PASSWORD` | `key_password` in signing.json |

Do not paste those values in chat or logs. The recovery backup includes a local
`configure-github-secrets.py` helper that uses an already authenticated `gh`
installation and passes secrets through stdin.

After the four secrets are configured and the next version is committed, run
Actions → **Signed APK release** → **Run workflow** on `main`. It uses the same
packager and targets the release tag at the exact build commit. A rerun of an
already published version does not overwrite its existing release.
The normal Android build remains a separate debug build and emulator check.

## One-time transition from old builds

Earlier APKs used development/debug certificates. The latest checked CI APK was
0.2.76/code 10076 with certificate
`fcc7e535b7e2505fab677eb85e33a8f17b8258bd8d1ac5887d7a9e061437c696`.
The first supplied development build used yet another certificate, recorded in
the historical handoff. Neither certificate matches the permanent test key.

Android will therefore reject installing 0.3.0 over those builds. A one-time
uninstall/reinstall is necessary when moving from either old certificate.
Uninstalling removes this launcher's private settings, shortcuts and widget
bindings; record the layout first. This source does not currently offer a full
`.nthdesktop` export/import. Keep the previous APK if you need to return to it.
Do not clear data or uninstall merely to apply a later update signed with the
permanent test key: install the new APK and choose **Update**.

Later updates preserve app data when the package and certificate match and the
version code increases. Device behavior still needs testing on the owner's phone.
