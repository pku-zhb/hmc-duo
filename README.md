<p align="center"><strong>English</strong> · <a href="README.zh-CN.md">简体中文</a></p>

# hypermusiccover-duo

Lock-screen music, lyrics and an independent vertical notification rail for Xiaomi
foldable phones on HyperOS. Maintained by [pku-zhb](https://github.com/pku-zhb).

Adapted from [HyperMusicCover-Enhanced](https://github.com/Lewewe/HyperMusicCover-Enhanced)
and [HyperMusicCover](https://github.com/zyl6932/HyperMusicCover).
Device coverage is focused on the maintainer's foldable phone; compatibility with
other devices is not established.

## Features

- Square or full-screen artwork, synchronized and word-timed lyrics, lyric effects
  and optional translation. Foreground artwork and background are separate settings:
  native wallpaper, artwork background or animated background, with blur and dimming controls.
- Duo-owned card geometry and a right-hand flashlight/camera column. Enlarged touch
  targets use the same coordinates as the animations; the system retains secure launch
  and cancellation. MiDuo and MiDuo Notification are not required.
- A display-only battery/network cluster, an independent lock-screen status-bar
  switch, and a compact time/date row for outer-screen artwork and lyrics. The time
  can sit on either side of the date. Ordinary AOD retains the native presentation;
  the compact row is also available in full-screen AOD.
- HyperOS and ColorOS vertical capsule styles. Tap a capsule or bubble to open its
  card; swipe to switch. HyperOS stops at the first/last item, follows the drag with
  a spring, and joins/splits native-material bubbles. More than three items shorten
  the capsule; if circles still cannot fit, the top items are hidden but remain
  available through content expansion.
- Swipes in empty content space expand music, live cards, then notifications, or
  collapse them. When everything is collapsed, the whole left content area is
  available. Visible cards retain native scrolling, dismissal, taps, long presses
  and player controls. After all content is open, upward swipes can unlock.
- The native artwork slot cycles through artwork, lyrics and return-to-clock
  actions without moving the title or adding another control. Inner landscape adds
  an artwork-and-lyrics page to this cycle.
- Per-category initial collapse settings or restoration of the last lock-screen
  state; separate inner-landscape settings, bounded native card widths and a combined
  artwork/lyrics layout.
- Native glass materials and independent lock-screen notification visibility and
  retention options. Disable equivalent HyperLight options when using Duo's controls.
- Settings export/import, notification media-card customization and a Duo-only
  release/update channel. Translation credentials remain outside exported backups.

This is a preview. Newly implemented behavior, inner-screen layouts and AOD
transitions require device acceptance; a successful build is not phone verification.
See [current status and acceptance scope](docs/fold-settings-roadmap.md).

## Install and migrate

Requires rooted HyperOS 4, Android 15+ and LSPosed with modern Xposed API 102.
Duo's package and namespace are `com.zhuhuibin.hypermusiccover.duo`.

1. Export settings from the previous app before removing it.
2. Install a Duo APK from [Releases](https://github.com/pku-zhb/hypermusiccover-duo/releases)
   when available, or build locally.
3. Enable Duo and its recommended scopes in LSPosed. Disable the previous music
   module and the MiDuo/MiDuo Notification SystemUI hooks for this independent layout.
4. Restart the relevant scopes when ready, then import settings.

Duo installs separately from Enhanced and the old Fold package; Android does not
transfer their app data. Old backups discard removed options and restore previously
inactive background switches to native wallpaper. App-only backups leave module
background settings untouched.

Package updates never restart scopes automatically. Different signing keys cannot
replace one another. Existing previews keep their current signing identity;
production signing is a separate setup step, not implied by a release build.
See [release procedure](docs/releasing.md).

## Build and check

Requires JDK 21+, Android SDK 37 and build tools 37.0.0.

```sh
git clone https://github.com/pku-zhb/hypermusiccover-duo.git
cd hypermusiccover-duo
```

For the maintainer's configured Mac environment, use:

```sh
tools/build.sh :app:testDebugUnitTest :app:assembleDebug
```

For another environment, point `JAVA_HOME` and `ANDROID_HOME` at installed tools,
choose an absolute build directory outside the source tree, and run:

```sh
export HMC_BUILD_ROOT=/absolute/path/to/duo-build
./gradlew -I tools/gradle-init.gradle :app:testDebugUnitTest :app:assembleDebug
```

The maintainer script keeps Gradle outputs and temporary files on the configured
external build volume and refuses to run if that volume is unavailable. Signing
credentials stay outside source control and synced storage. See the release
procedure for explicit official-build requirements.

## Feedback and attribution

Report bugs in [Duo Issues](https://github.com/pku-zhb/hypermusiccover-duo/issues)
with the device, OS/module versions, display orientation and reproduction steps.
Each preview lists only its changed behavior to verify.

Thanks to Lewewe, zyl6932, puhboo, juren233, btm_m / HyperChanger and the contributors
listed in the app's Credits. [AGPL-3.0](LICENSE), [NOTICE](NOTICE) and bundled
licences preserve the upstream notices. Donation controls await this project's
own payment assets; upstream payment destinations are not used.
