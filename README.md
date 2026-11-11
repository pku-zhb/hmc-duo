<p align="center"><strong>English</strong> · <a href="README.zh-CN.md">简体中文</a></p>

# hypermusiccover-duo

Lock-screen music, lyrics and a vertical notification rail for Xiaomi Fold with
MiDuo on HyperOS. Maintained by [pku-zhb](https://github.com/pku-zhb).

A dedicated adaptation of [HyperMusicCover-Enhanced](https://github.com/Lewewe/HyperMusicCover-Enhanced),
which builds on [HyperMusicCover](https://github.com/zyl6932/HyperMusicCover).
General device compatibility is not a goal.

## Current features

- Square artwork, synchronized/word-timed lyrics and lyric translation.
- A vertical capsule with ordered notification, live-card and music bubbles beside
  MiDuo's shortcut column; swipe to switch or tap a bubble to open its card.
- Upward swipes in the left clock/notification area expand notifications, live
  cards, then music; swipe-to-unlock follows when all available content is open.
- Select which categories collapse on lock, or keep the last lock-screen state.
  Inner landscape has separate category defaults and a cover/lyrics two-column layout.
- Native player, clock and notification reflow with AOD/wake handling.
- Native Soft Glass or the native player's final material, including HyperLight.
- Settings export/import and on-device diagnostic counters.

Full-screen cover/background switching, waveform and optional extension pages
remain unavailable pending compatibility work. Automatic updates are disabled;
published Duo builds use this repository's Releases, never the upstream feed.
See [implementation status](docs/fold-settings-roadmap.md) for device-confirmed
behavior and open issues. Host tests do not establish phone compatibility.

## Install and migrate

Requires rooted HyperOS 4, Android 15+, and LSPosed with modern Xposed API 102.
The vertical rail requires MiDuo's right-hand shortcut column.

Application ID and source namespace: `com.zhuhuibin.hypermusiccover.duo`.
Launcher name: `hypermusiccover-duo`. This installs separately from Enhanced and
`com.zhuhuibin.hypermusiccover.fold`; Android does not transfer their app data.

1. Export settings from the previous app before removing it.
2. Install a Duo APK from [Releases](https://github.com/pku-zhb/hypermusiccover-duo/releases)
   when published, or build locally.
3. Disable the previous music module in LSPosed, enable Duo and its recommended
   scopes. Do not enable both music modules together.
4. Restart scoped processes yourself and import settings. Removed options stay disabled.

Preview builds require device acceptance. This repository does not imply a stable
Duo release exists. APKs signed with different keys cannot update each other.
Updates no longer restart scoped processes automatically. The owner chooses when
to restart and load the new module code.

## Build and check

```sh
git clone --recurse-submodules https://github.com/pku-zhb/hypermusiccover-duo.git
cd hypermusiccover-duo
tools/build.sh :app:testDebugUnitTest :app:assembleDebug
```

Requires JDK 21+, Android SDK 37 and build tools 37.0.0. On this Mac the script
reuses the installed SDK/cache and writes output to
`/Volumes/Harbor/DevCache/hypermusiccover-duo/`. Set `JAVA_HOME`, `ANDROID_HOME`
and `GRADLE_USER_HOME` to use another installed toolchain. On other machines,
set `HMC_BUILD_ROOT` and pass `-I tools/gradle-init.gradle` to Gradle.

Release builds accept `SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`,
`SIGNING_KEY_ALIAS` and `SIGNING_KEY_PASSWORD`, or an ignored `keystore.properties`.
Keep credentials outside source control. Without a release key, the build uses
the debug key and is only a development artifact.

The optional `hyper-canvas` submodule is an upstream dependency. Its provider
currently trusts the Enhanced host package. Building it alone does not make it
Duo-compatible; its UI stays disabled until that contract is migrated.

## Development and feedback

Commit and push each validated milestone. Keep layout, native gestures, text and
release identity changes independently reviewable. Report bugs in
[Duo Issues](https://github.com/pku-zhb/hypermusiccover-duo/issues) with device,
OS/module version, display form and reproduction steps.

## Attribution and licence

- Lewewe: Enhanced upstream development.
- zyl6932: original HyperMusicCover.
- puhboo: lyric functionality and HyperCanvas rendering.
- juren233 / HyperLyrics-Enhanced: media-card customization foundations.
- btm_m / HyperChanger and the contributors in the app's Credits page.

[AGPL-3.0](LICENSE). [NOTICE](NOTICE) and bundled licence texts retain upstream
attribution. This project does not use the upstream donation channels.
