# Release identity and verification

The application ID is `com.zhuhuibin.hypermusiccover.duo`. Official release assets
are named `hmc-duo-X.Y.Z.apk` and belong to this repository. The
in-app updater only selects the asset matching the release tag, and checks the
package and certificate against the installed application before handing the APK
to Android's installer. Prereleases are not offered by the stable updater.

## Current signing state

Existing previews use the development certificate. Production key provisioning
is deferred by the maintainer. Do not describe these previews as officially signed
releases, publish them as stable, or enable their stable update feed.

`mcOfficialRelease=true` requires explicit signing inputs and never falls back to
the debug key. It enables the stable update feed only in that build. Ordinary local
builds remain installable development artifacts with updates disabled.

## Prepare an official release

Keep the keystore and passwords outside the repository and synced storage. The
maintainer's credential authority is Bitwarden; inject `SIGNING_STORE_FILE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` and the public
`SIGNING_CERT_SHA256` through the credential runtime. Never put their values in a
command, repository, workflow log or plaintext environment file.

On the maintainer's Mac, with `JAVA_HOME` and `ANDROID_HOME` configured:

```sh
tools/release.sh 0.3.0
```

The script runs host tests, creates a release build, checks ZIP integrity,
16 KiB alignment, Android signatures, package ID, version and the approved
certificate, then retains the versioned APK and SHA-256 under `releases/local/`.
It does not publish a release. On another host use the documented Gradle command
and run `tools/verify-release.py` with the same identity arguments before archival.

Stable version codes use `(major * 10000 + minor * 100 + patch) * 1000`; minor and
patch stay below 100. The version code must exceed all prior distributed builds.

## Migrate the preview certificate

Before shipping the first official update, retain and securely archive the exact
key that signed the distributed previews. Create an Android signing lineage that
attests to the new production key, and supply its path as `SIGNING_LINEAGE_FILE`.
The release script embeds that lineage using `apksigner`, targeting Android 13+
rotation. The app itself requires Android 15+.

Test replacement on a device that has the old preview installed: settings, LSPosed
scope and the package identity must survive `adb install -r`. Recheck the installed
certificate and loaded module after a scope restart. Never uninstall to disguise
an upgrade failure, and never reboot the whole phone for this check.

Key rotation and verification follow the Android tools'
[apksigner documentation](https://developer.android.com/tools/apksigner) and
[signature lineage design](https://source.android.com/docs/security/features/apksigning/v3-1).
This migration is not verified until the real production key and device test exist.

## Publish after acceptance

1. Build from a clean, reviewed commit; verify the APK and host tests.
2. Validate only the changed phone behavior and record any limitations.
3. Tag that commit with its exact stable version.
4. Create a draft GitHub release with the matching APK, hash and concise change notes.
5. Publish after acceptance. Keep source and corresponding artifact version aligned.

`tools/ci-check.yml` is the ready-to-enable GitHub Actions workflow. Copy it to
`.github/workflows/check.yml` with an account authorized to manage workflows. It
builds and tests development artifacts without signing secrets or release-publishing
permission. The template is not an active hosted check until enabled. The repository has no automatic scope-restart behavior
on package replacement.
