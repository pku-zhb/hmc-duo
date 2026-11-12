# HyperMusicCover Duo

This repository owns `hypermusiccover-duo`, package and namespace
`com.zhuhuibin.hypermusiccover.duo`. Upstream is attribution and reference,
not the destination for project links, updates or bug reports.

- Complete and validate one coherent milestone, commit it with an imperative
  `feat:`, `fix:`, `refactor:`, `docs:`, `test:` or `chore:` subject, then push.
  Do not accumulate unrelated work until the end.
- Preserve upstream copyright, licence notices and retained trial APKs. Remove
  retired code only after checking its callers.
- Archive previews without sending a Slack attachment unless the owner asks.
  When requested, use `cc-connect send --file` to the current conversation and
  report delivery only after the CLI confirms success. The version archive is
  `~/Harbor Drive/02 Personal Files/Miduo/`. Preserve versioned filenames and verify
  archive copies against the retained APK under `releases/local/`.
- Use `tools/build.sh` for local Gradle tasks. Builds, test reports and temporary
  files belong on the mounted Harbor DevCache volume. Never create a missing mount.
- Keep signing material outside source control and synced storage. A debug-signed
  APK is not a verified release upgrade.
- Separate device reports, source reasoning and host test results. Passing JVM
  tests or installed hooks do not establish correct phone behavior.
- End each preview with a short acceptance list for that preview's changes only.
  Do not ask the owner to repeat previously accepted features unless a specific
  regression or changed dependency makes a targeted check necessary.
- Explain the core bugs' causes and remedies in each report, distinguishing
  confirmed causes, plausible contributors and behavior still awaiting acceptance.
- Source work and host builds do not authorize phone installation, restarts or
  live setting changes. Follow the conversation's current device authorization.
- The owner granted continuing phone access for this investigation on 2026-11-12,
  including relevant installation, interaction and scope restarts, until explicitly
  revoked. Prefer logs, state and frame timing; avoid screenshots when possible,
  and keep GUI screenshots out of the main conversation when possible.
- Never reboot the whole phone. Restart only relevant scopes when needed. Use
  ordinary replacement for preview updates, never a reboot-dependent staged install.
  Module code must not restart scopes automatically on package replacement.
- Duo owns its content and capsule geometry through explicit dp parameters and
  the window size; do not derive it from MiDuo widths or animated shortcut bounds.
  Duo also owns the native shortcut column and its animation origins. Keep visual
  bounds, touch hit testing and gesture direction in the same coordinate space;
  retain the OEM's secure launch/cancel path. Do not require a MiDuo module.
- The vertical capsule is a summary. Full text stays in the expanded native card.
  Preserve grapheme clusters and script shaping.

The `hyper-canvas` submodule is an optional upstream dependency. Its provider
currently trusts the Enhanced package; keep its UI disabled until host identity
and signing are migrated together.
