#!/usr/bin/env python3
"""Build and verify an APK using the owner's persistent, private test key."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def capture(args):
    return subprocess.check_output([str(a) for a in args], text=True, cwd=ROOT)


def inspect(apk, build_tools):
    certificate = capture([build_tools / 'apksigner', 'verify', '--verbose', '--print-certs', apk])
    fingerprints = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-f]{64})', certificate)
    if len(fingerprints) != 1:
        raise SystemExit('Expected exactly one verified APK signer.')
    badging = capture([build_tools / 'aapt', 'dump', 'badging', apk])
    package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    if not package:
        raise SystemExit('Cannot read APK package and version.')
    return {
        'applicationId': package[1], 'versionCode': int(package[2]),
        'versionName': package[3], 'certificateSha256': fingerprints[0],
        'debuggable': 'application-debuggable' in badging,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--signing-config', type=Path, help='Private JSON from the signing backup; never commit this file.')
    parser.add_argument('--gradle', default=str(ROOT / 'gradlew'))
    parser.add_argument('--init-script', type=Path)
    parser.add_argument('--verify-only', type=Path, help='Verify and package an already built release APK.')
    parser.add_argument('--unsigned-apk', type=Path, help='Sign the exact unsigned release artifact from CI instead of rebuilding locally.')
    parser.add_argument('--companion', action='store_true', help='Sign the optional debuggable Discover bridge, keeping the launcher release non-debuggable.')
    parser.add_argument('--previous-apk', type=Path, help='Also verify the prior signed APK has the same identity and a lower version code.')
    parser.add_argument('--output-dir', type=Path, default=ROOT / 'dist')
    args = parser.parse_args()
    if args.verify_only and args.unsigned_apk:
        parser.error('--verify-only and --unsigned-apk are mutually exclusive')
    version = dict(line.split('=', 1) for line in (ROOT / 'version.properties').read_text().splitlines() if line and not line.startswith('#'))
    env = os.environ.copy()
    if args.signing_config:
        config_path = args.signing_config.resolve()
        config = json.loads(config_path.read_text())
        store = Path(config['store_file'])
        if not store.is_absolute():
            store = config_path.parent / store
        env.update({
            'NTH_SIGNING_STORE_FILE': str(store),
            'NTH_SIGNING_STORE_PASSWORD': config['store_password'],
            'NTH_SIGNING_KEY_ALIAS': config['key_alias'],
            'NTH_SIGNING_KEY_PASSWORD': config['key_password'],
        })
    sdk = env.get('ANDROID_HOME') or env.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise SystemExit('Set ANDROID_HOME to the Android SDK directory first.')
    build_tools = Path(sdk) / 'build-tools' / '35.0.0'
    if not (build_tools / 'apksigner').is_file():
        raise SystemExit('Install Android SDK build-tools;35.0.0 first.')
    if not args.verify_only:
        required = ('NTH_SIGNING_STORE_FILE', 'NTH_SIGNING_STORE_PASSWORD', 'NTH_SIGNING_KEY_ALIAS', 'NTH_SIGNING_KEY_PASSWORD')
        if any(not env.get(key) for key in required):
            raise SystemExit('Restore the existing signing backup or set all four NTH_SIGNING variables. Do not generate another key.')
        if not Path(env['NTH_SIGNING_STORE_FILE']).is_file():
            raise SystemExit('Signing keystore file does not exist.')
        if args.unsigned_apk:
            work = ROOT / 'app/build/outputs/apk/offline-signed'
            work.mkdir(parents=True, exist_ok=True)
            aligned = work / 'app-aligned.apk'
            apk = work / 'app-release.apk'
            subprocess.run([str(build_tools / 'zipalign'), '-f', '-p', '4', str(args.unsigned_apk.resolve()), str(aligned)], check=True)
            subprocess.run([
                str(build_tools / 'apksigner'), 'sign', '--ks', env['NTH_SIGNING_STORE_FILE'],
                '--ks-key-alias', env['NTH_SIGNING_KEY_ALIAS'],
                '--ks-pass', 'env:NTH_SIGNING_STORE_PASSWORD', '--key-pass', 'env:NTH_SIGNING_KEY_PASSWORD',
                '--out', str(apk), str(aligned),
            ], env=env, check=True)
        else:
            command = [args.gradle, '--no-daemon', '--console=plain']
            if args.init_script:
                command += ['--init-script', str(args.init_script.resolve())]
            command += ['assembleRelease', 'lintRelease', 'testDebugUnitTest']
            subprocess.run(command, env=env, cwd=ROOT, check=True)
            apk = ROOT / 'app/build/outputs/apk/release/app-release.apk'
    else:
        apk = args.verify_only.resolve()
    metadata = inspect(apk, build_tools)
    expected = {
        'applicationId': 'com.nth.desktop3d.discover' if args.companion else 'com.nth.desktop3d', 'versionCode': int(version['versionCode']),
        'versionName': version['versionName'],
        'certificateSha256': (ROOT / 'docs/test-update-certificate.sha256').read_text().strip(),
        'debuggable': args.companion,
    }
    if metadata != expected:
        raise SystemExit('APK does not match the committed package, version, release mode and permanent test certificate.')
    if args.previous_apk:
        previous = inspect(args.previous_apk.resolve(), build_tools)
        if any(previous[key] != metadata[key] for key in ('applicationId', 'certificateSha256')):
            raise SystemExit('Previous APK uses a different package or certificate; this is a migration, not an in-place update.')
        if previous['versionCode'] >= metadata['versionCode']:
            raise SystemExit('Increase versionCode beyond the previous APK before publishing an update.')
    args.output_dir.mkdir(parents=True, exist_ok=True)
    name = f"{'Nth-Discover-Bridge' if args.companion else 'Nth-Desktop-3D'}-{metadata['versionName']}.apk"
    output = args.output_dir / name
    if output.resolve() != apk.resolve():
        shutil.copyfile(apk, output)
    metadata['apkSha256'] = hashlib.sha256(output.read_bytes()).hexdigest()
    metadata['sourceCommit'] = capture(['git', 'rev-parse', 'HEAD']).strip()
    metadata['sourceTreeDirty'] = bool(capture(['git', 'status', '--porcelain', '--untracked-files=no']).strip())
    (args.output_dir / (name + '.sha256')).write_text(f"{metadata['apkSha256']}  {name}\n")
    (args.output_dir / ('bridge-verification.json' if args.companion else 'build-verification.json')).write_text(json.dumps(metadata, indent=2) + '\n')
    print(f"Verified {name}: {metadata['applicationId']}, code {metadata['versionCode']}, permanent test certificate, expected release/companion mode.")
    if env.get('GITHUB_OUTPUT'):
        with open(env['GITHUB_OUTPUT'], 'a') as target:
            target.write(f"version={metadata['versionName']}\napk={output}\n")


if __name__ == '__main__':
    main()
