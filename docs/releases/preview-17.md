# Preview 17

This iteration reduces HMC-side overhead during AOD transitions and native island
animation. This document covers the HMC side; the companion Lite fix is
Preview 5. Phone settings are not changed.

## Findings

The owner reproduced two AOD entry/wake cycles and a slow native-island animation
during a 90-second, 199 Hz SystemUI CPU sample of the Preview 16 / Lite Preview 3
pair. All three Android animation scales were 1.0. The sample contained 7,151
records. It exposed a repeated unsupported-method lookup in Lite's ordinary-row
role classification, including exception stack construction. HMC's extra palette
replays and global source classification add work on the same material path.
These are distinct contributors; no isolated post-fix device comparison has been
performed. Sampled child percentages overlap and are not animation duration or
independent time budgets.

H1's current-material handoff was reviewed separately. No per-frame material
rebind loop was found: deferred AOD requests wait for lifecycle events, and
foreground updates have a re-entry guard. It is not changed in this iteration.

## HMC changes

- Recognize the native player background at its explicit binder boundary. Global
  material hooks use registered identities instead of repeatedly inspecting
  resource names and walking view ancestry. Keep native recipe recording
  independent of the follow-final-material setting.
- Coalesce legacy palette clear/add activity rather than replaying a complete
  followed palette after each individual operation. The verified three-pair AOD
  recipe makes two clears and three adds; one owned transaction now restores once
  instead of five complete palette replays. Preserve the real committed
  source and registered HMC destination boundary.
- Skip notification scene/role checks when no HMC transition owns any property.
  Reject AOD, screen-off and keyguard exit using cached flags before querying
  keyguard/bouncer state. Check native row identity before reflected role getters,
  short-circuit those getters, and skip an absent late-row callback immediately.

## Acceptance boundary

This is a source and host-validated performance candidate. Installation, scope
reload and observed improvement require separate owner authorization. The owner subsequently assigned both projects to this executor. Lite Preview 5
now caches positive/missing accessors and reuses per-call decisions, with live
post-native validation. The handoff records the verified ROM and caller evidence
for reuse; completed reverse engineering need not be repeated. The separate
unified-material-editor worktree is excluded from this performance pair.

Only repeat AOD entry/wake smoothness, native-island smoothness, and one
follow-material AOD transition to ensure batching does not leave a partial palette
or lose the glass surface. Existing unrelated feature checks are not required.

## Host validation

All 967 tests passed across 156 suites with no failures, errors or skipped tests.
Debug, Release and Release vital lint completed successfully. The 14 added
regressions cover source validation/reparenting, complete and nested palette
transactions, cancellation and short-circuited notification reads. These checks
do not establish post-install device performance.

## Fixed candidate artifacts

- HMC `0.2.0-preview.17` / `20017`, source
  `396d866ffb5cb33f7d4dc184fcd2b41cd614f86d`, SHA-256
  `83772d7e33e2c7212188eb845fa3783838ebaf0d73b3daf53455b4e772dea691`.
- Lite `0.1.0-preview5` / `5`, source
  `109e4024cdfaae4dbf573ebd5a58ed6f4a9315d0`, SHA-256
  `a7dbe951c89c848ff25ea0687a2b31f8054e1d99bc0228ce103ca0d1a673522c`.
- ZIP, alignment, package/version and signing checks passed. The retained
  development signing identity is unchanged. Both have versioned local and
  personal archive copies. Installation and scope reload remain owner-controlled.
- Lite passed all 96 tests in 18 suites and Debug/R8 Release/vital lint.
  Host checks are separate from observed device smoothness.
