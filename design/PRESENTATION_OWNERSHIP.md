# Presentation ownership

The 0.2.8 consolidation addresses duplicate lifecycle decisions rather than
lengthening animation waits. `CoverBackdrop.Snapshot` captures page intention,
notification compaction, navigation, native surface kind, window, geometry
revision and source generation once per update. Clock and background consumers
share that input; clock phase does not decide whether a background exists.

## Submission and restoration

- Awake state and native-state cache invalidation precede wake consumers.
- ART/LYRICS changes with the same composition retain their source. Actual window,
  composition or configured-background changes still rebuild geometry as needed.
- Glyph drawing publishes measurements; it never re-enters the whole clock pose
  writer after a leaf has already drawn.
- Requested replacement, measured geometry and committed pose are distinct.
  A temporary measurement miss can retain a valid same-window/revision pose;
  changed windows, navigation or destination cannot borrow an unrelated cache.
- Clock/date and stack translations use last-write leases. They never restore
  fixed zero/unit values over a later native/rail write, including equal-value
  native setter events or a subpixel desired value that was never applied.
- A full-AOD/static or awake/mesh background gains sampling ownership only after
  the matching source has actually drawn. During a same-source handoff, the valid
  outgoing display list remains until the receiver presents; no timer extends
  invalid pixels. Hidden, navigation, changed source/window or unlock withdraw it.
- R8-safe type markers identify owned material targets. Namespace strings are
  not runtime identities. Native cards never become relay destinations.

## Native boundaries

The OEM still owns font shaping, secure authentication, parent AOD zoom, ordinary
AOD, native card widths and card input. Combined artwork/lyrics translates the
whole native stack without moving a child beyond its parent's input/clip frame.
Only compact height changes its verified row geometry; 100% remains native.
Temporary test configuration is saved and restored. Tests never reboot the phone.

## Background verification matrix

| Scene | Expected background | Sampling owner |
| --- | --- | --- |
| Desktop / unlocked | Native | Native |
| Lit lockscreen artwork or lyrics with selected backdrop | Current source | Matching drawn local source |
| Notification compacts artwork | Native | Native |
| Reopen artwork / lyrics | Selected backdrop | Local only after current presentation |
| Full-screen AOD | Static selected backdrop | Matching static presentation |
| Ordinary/custom AOD | OEM | Native |
| Awake/full-AOD handoff, unchanged source | Same selected backdrop | Valid local ownership until receiver presents |
| Unlock / bouncer / changed window or source | Native or newly selected scene | Old lease withdrawn |
| Navigation exclusive | Navigation | No stale cover/map borrowing |
| Inner single pages | Native-width cards, safe-window centered content | Current source/window |
| Inner combined page | Native-width left column and right lyrics | Same source/layout generation |

The existing numeric probes expose scene epoch, target renderer, actual owner,
source generation, committed generation, pose leases and native control bounds.
They establish lifecycle and geometry invariants, not subjective GPU visual parity.
