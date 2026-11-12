#!/usr/bin/env bash
# Build and verify a formal release. Publication is a separate, explicit operation.
set -euo pipefail
umask 077
project_root="$(cd "$(dirname "$0")/.." && pwd)"
version="${1:?Usage: tools/release.sh X.Y.Z}"
[[ "$version" =~ ^([0-9]{1,3})\.([0-9]{1,2})\.([0-9]{1,2})$ ]] || { echo 'Use a stable X.Y.Z version.' >&2; exit 1; }
major=$((10#${BASH_REMATCH[1]})); minor=$((10#${BASH_REMATCH[2]})); patch=$((10#${BASH_REMATCH[3]}))
version_code=$(((major * 10000 + minor * 100 + patch) * 1000))
((version_code > 0 && version_code < 2100000000)) || { echo 'Version code is outside Android limits.' >&2; exit 1; }
: "${SIGNING_STORE_FILE:?Supply the release keystore outside synced storage}"
: "${SIGNING_STORE_PASSWORD:?Supply through the credential runtime}"
: "${SIGNING_KEY_ALIAS:?Supply through the credential runtime}"
: "${SIGNING_KEY_PASSWORD:?Supply through the credential runtime}"
: "${SIGNING_CERT_SHA256:?Supply the approved public certificate fingerprint}"
: "${ANDROID_HOME:?Set the Android SDK location}"
cd "$project_root"
tools/build.sh :app:testDebugUnitTest :app:assembleRelease -PmcOfficialRelease=true \
    "-PmcVersionName=$version" "-PmcVersionCode=$version_code"
apk=/Volumes/Harbor/DevCache/hypermusiccover-duo/build/app/outputs/apk/release/app-release.apk
# Optional proof of rotation from an existing preview key. The lineage contains only public
# certificates and attestations. Its setup must be verified separately on a device.
if [[ -n "${SIGNING_LINEAGE_FILE:-}" ]]; then
    "$ANDROID_HOME/build-tools/37.0.0/apksigner" sign \
        --ks "$SIGNING_STORE_FILE" --ks-key-alias "$SIGNING_KEY_ALIAS" \
        --ks-pass env:SIGNING_STORE_PASSWORD --key-pass env:SIGNING_KEY_PASSWORD \
        --lineage "$SIGNING_LINEAGE_FILE" --rotation-min-sdk-version 33 "$apk"
fi
python3 tools/verify-release.py "$apk" --version "$version" --code "$version_code" \
    --certificate "$SIGNING_CERT_SHA256"
mkdir -p releases/local
destination="releases/local/hypermusiccover-duo-v$version.apk"
[[ ! -e "$destination" ]] || { echo 'Release already exists; use a new version.' >&2; exit 1; }
cp "$apk" "$destination"
shasum -a 256 "$destination" > "$destination.sha256"
echo "Release prepared at $destination. No remote release was published."
