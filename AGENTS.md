# Contributor and agent guidelines

This file is public. Keep it free of private information.
If `AGENTS.local.md` exists in the repository root, read it too. It is ignored by Git
and holds machine-specific instructions (local paths, device-test workflow, archive
locations). Never commit it.

## Project

- `hmc-duo` provides lock-screen music, lyrics and a vertical notification rail for Xiaomi
  foldables on HyperOS. Package and namespace: `com.zhuhuibin.hypermusiccover.duo`.
  Upstream projects are attribution and reference, not the destination for links,
  updates or bug reports.
- Preserve upstream copyright, licence and notice files. Remove retired code only after
  checking its callers.
- Duo owns its content and capsule geometry through explicit dp parameters and the window
  size. Keep visual bounds, touch hit testing and gesture direction in one coordinate space,
  and retain the system's secure launch/cancel path. Do not require the MiDuo module.
- The vertical capsule is a summary; full text stays in the expanded native card.
  Preserve grapheme clusters and script shaping.
- Never use the legacy mutating `PROBE op=cover` entry to select a test page; it can leave an
  unguarded view in the shared keyguard/shade background. Use the normal native player UI.
- Build and test with `tools/build.sh` (see the Build section of `README.md`).
  Environment variables: `DEV_VOLUME` (required), `DEV_TOOLS_DIR`, `HMC_CACHE_ROOT`,
  `APK_ARCHIVE_DIR`, `JAVA_HOME`, `ANDROID_HOME`.
- Keep signing material outside source control. A debug-signed APK is not a verified
  release. Official builds follow `docs/releasing.md`.
- Keep device reports, source reasoning and host test results separate in documentation.
  Passing JVM tests does not establish on-device behavior. Public docs omit device
  identifiers and diagnostic dumps.

## Repository hygiene (every commit)

1. **No machine-specific details.** Never commit absolute paths, user names, host names,
   drive or volume names, or personal folders. Use the documented environment variables.
2. **No process records.** Keep logs, approval or handover notes, review transcripts,
   AI prompts, session or memory notes, device dumps, screenshots and local APK receipts
   out of the repository. Store them elsewhere or in an ignored local file.
3. **Write for outside readers.** Commit subjects and documents are technical and neutral
   (`feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`, imperative mood). No workflow
   chatter about who asked for or approved what.
4. **Docs describe the code as it is,** not the history of how or when it was made.
   Put rationale and constraints in `design/` and `docs/`, not step-by-step work logs or
   per-iteration validation diaries.
5. **No secrets.** No keys, tokens, passwords, keystores or personal data, in files or in messages.
6. **Self-check, then proceed.** Before each commit, run `tools/check-hygiene.sh` and read
   `git diff --cached` against these rules. Fix anything that violates them, then commit and
   push without asking for confirmation on routine hygiene. Enable the hook once per clone:
   `git config core.hooksPath tools/hooks`.
