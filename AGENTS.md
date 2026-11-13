# hmc-duo

This repository owns `hmc-duo`, package and namespace
`com.zhuhuibin.hypermusiccover.duo`. Upstream is attribution and reference,
not the destination for project links, updates or bug reports.

- Complete and validate one coherent milestone, commit it with an imperative
  `feat:`, `fix:`, `refactor:`, `docs:`, `test:` or `chore:` subject, then push.
  Do not accumulate unrelated work until the end.
- Preserve upstream copyright, licence notices and retained trial APKs. Remove
  retired code only after checking its callers.
- Archive releases without sending a Slack attachment unless the owner asks.
  When requested, use `cc-connect send --file` to the current conversation and
  report delivery only after the CLI confirms success. Use the owner's configured
  version archive, preserve versioned filenames and verify archive copies against
  the retained APK under `releases/local/`.
- Also deliver each APK to the phone's system Download directory (`/sdcard/Download`)
  when the device is reachable, retaining its versioned filename and verifying the
  copied hash. If unavailable, report that delivery is pending. File delivery does
  not authorize installation or scope reload; obtain installation confirmation.
- Use `tools/build.sh` for local Gradle tasks. Builds, test reports and temporary
  files belong on the mounted Harbor DevCache volume. Never create a missing mount.
- Keep signing material outside source control and synced storage. A debug-signed
  APK is not a verified release upgrade.
- Separate device reports, source reasoning and host test results. Passing JVM
  tests or installed hooks do not establish correct phone behavior.
- End each release with a short acceptance list for that release's changes only.
  Do not ask the owner to repeat previously accepted features unless a specific
  regression or changed dependency makes a targeted check necessary.
- Explain the core bugs' causes and remedies in each report, distinguishing
  confirmed causes, plausible contributors and behavior still awaiting acceptance.
- Source work and host builds do not authorize phone installation, restarts or
  live setting changes. Follow the conversation's current device authorization.
- Prefer logs, state and frame timing for authorized phone work. Avoid screenshots
  when possible, and keep GUI screenshots out of the main conversation when possible.
- Never reboot the whole phone. Restart only relevant scopes when needed. Use
  ordinary replacement for updates, never a reboot-dependent staged install.
  Module code must not restart scopes automatically on package replacement.
- Duo owns its content and capsule geometry through explicit dp parameters and
  the window size; do not derive it from MiDuo widths or animated shortcut bounds.
  Duo also owns the native shortcut column and its animation origins. Keep visual
  bounds, touch hit testing and gesture direction in the same coordinate space;
  retain the OEM's secure launch/cancel path. Do not require a MiDuo module.
- The vertical capsule is a summary. Full text stays in the expanded native card.
  Preserve grapheme clusters and script shaping.

Retired implementation and historical diagnostic documents are kept in a verified
external development archive. Do not restore archived UI, source or dependencies
without an explicit new request. Preserve copyright, licences and source attribution.
Public documentation describes the active scope and separates host checks from
device acceptance; omit local device identifiers and diagnostic dumps.

New APK filenames use the short project name and ordinary X.Y.Z version, without preview labels or source IDs. Keep source identity inside the APK and local receipts. Preserve historical APKs and names.
Run `python3 tools/archive-apk.py` after a clean committed, pushed build to verify and retain the ordinary signed APK. Formal production signing remains a separate configured path.
