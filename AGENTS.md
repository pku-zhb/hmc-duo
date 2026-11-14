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
  copied hash. If unavailable, report that delivery is pending. Installation and
  scope reload follow the current standing phone authorization below.
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

## Personal APK archive retention

Keep at most the latest five versions per module, including the current version,
in `~/Harbor Drive/02 Personal Files/Miduo/`. Group historical and short-name
aliases as one module; sort by semantic version, not filename text or mtime.
Retire matching old version-specific test notes with their APKs. Keep third-party
inputs and the repository's `releases/local/` packages/receipts outside this cap.
Use Obsidian's local vault trash, never permanent deletion. The CLI delete command
currently selects system trash; for explicit local trash use the official CLI
`eval` entry with `app.vault.trash(file, false)`. Verify source absence and retained
file hashes; on interruption inspect the actual result before retrying.

## Latest owner delivery preferences

Archive APKs locally and copy to the reachable phone Download directory. Do not
send APK attachments through cc-connect. Do not create Markdown notes in the
personal Miduo archive; keep acceptance work in the single rolling document below.
The owner has granted standing phone authorization until explicitly withdrawn.
The root agent may install archived, verified ordinary replacement APKs, restart
only the relevant scopes, and perform functional tests using background APIs or
minimal necessary GUI interaction. Save and restore any temporary test settings.
Never reboot the whole phone. Prefer logs, state and bounded traces; avoid
screenshots and keep GUI screenshots out of the main conversation when possible.
Ask the owner only for required personal authentication or repeated visual
judgment that background evidence cannot establish. Coordinate all device work
through the root agent so parallel agents never contend for the phone.

## Single rolling acceptance document

Maintain all future acceptance work in the one existing document:
`~/Harbor Drive/00 Temp/2026-11-14 - hc-lite 0.1.7 与 hmc-duo 0.2.6 本轮验收.md`.
Update that same file in place, keeping the current pending checks, confirmed
results and installation state together. Keep using this exact path across future
versions; do not create per-version acceptance files or parallel checklists.
Do not recreate older acceptance documents that the owner has deleted. Do not
write Markdown into the personal Miduo APK archive. The APK retention rule never
retires or renames this rolling document even when its filename contains older
versions. Device work follows the standing authorization above.

## Independent visual review

The owner authorizes screenshots and screen recordings for visual investigation.
For a requested visual review, delegate capture and inspection to a fresh agent
without inherited conversation history. Give it the test protocol, expected
interaction contract and device constraints, without feeding it old diagnoses.
The coordinating root should receive text findings rather than view the captures.
That reviewer may hold the exclusive phone-operation lease; root and other
agents must not contend for the device. Preserve temporary settings, playback
state and the owner's floating-window task. Never reboot or bypass authentication.

Review whole transitions first, then slow down or compare selected frames around
visible discontinuities; exhaustive frame-by-frame review is not required. Check
capture fidelity, especially AOD and glass, before treating video artifacts as bugs.
Keep raw captures and technical evidence in owning DevCache, outside synced Drive
and Git. Use the existing rolling document for the readable findings. Separate
visual observations, user reports, source-confirmed defects and hypotheses.
When the owner requests research before fixes, agree on that report before app
implementation. Successful tests or settled endpoint diagnostics do not establish
visual smoothness, readable contrast or good layout composition.

Never use the legacy mutating `PROBE op=cover` entry to select a test page. Its
direct `attachCover` route can create an unguarded raw ImageView in the shared
keyguard/shade background while `cover=false`; its matching off operation does
not reliably remove that view on still wallpaper. A review triggered this spill
and required a SystemUI scope reload to recover. Select pages through normal
native player/capsule UI instead. If a host is missing, restore the native music
card through normal interaction rather than forcing a diagnostic overlay.
