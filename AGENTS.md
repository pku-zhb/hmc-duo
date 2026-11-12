# HyperMusicCover Duo

This repository owns `hypermusiccover-duo`, package and namespace
`com.zhuhuibin.hypermusiccover.duo`. Upstream is attribution and reference,
not the destination for project links, updates or bug reports.

- Complete and validate one coherent milestone, commit it with an imperative
  `feat:`, `fix:`, `refactor:`, `docs:`, `test:` or `chore:` subject, then push.
  Do not accumulate unrelated work until the end.
- Preserve upstream copyright, licence notices and retained trial APKs. Remove
  retired code only after checking its callers.
- Deliver preview APK attachments with `cc-connect send --file` to the current
  conversation. Report delivery only after the CLI confirms success.
  The default user-facing version archive is
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
- Source work and host builds do not authorize phone installation, restarts or
  live setting changes. Follow the conversation's current device authorization.
- Ask the owner and receive explicit confirmation immediately before every APK
  installation. Approval for a previous preview does not authorize another install.
  If the owner is busy or asks to wait, finish source/build/commit preparation and
  leave the verified APK ready until a new confirmation is received.
- The owner performs all phone UI interaction, LSPosed switching and restarts.
  Prepare and install authorized preview APKs in the background, then provide
  concrete manual test steps and expected observations. Do not launch activities,
  automate gestures, capture the UI or restart phone processes to verify a build.
  Read-only ADB diagnostics may support the owner's reports.
  Use ordinary replacement for authorized preview updates. Do not choose a staged,
  reboot-dependent install unless the owner asks for one. Leave explicit scope
  restarts to the owner; module code must not restart scopes on package replacement.
- MiDuo owns native shortcuts. Keep visual bounds, touch hit testing, gesture
  direction and animation origins in the same coordinate space.
- The vertical capsule is a summary. Full text stays in the expanded native card.
  Preserve grapheme clusters and script shaping.

The `hyper-canvas` submodule is an optional upstream dependency. Its provider
currently trusts the Enhanced package; keep its UI disabled until host identity
and signing are migrated together.
