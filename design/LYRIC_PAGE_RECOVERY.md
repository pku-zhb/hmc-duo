# Lyric page recovery

The track's parsed lyric cache and its displayed lines are separate. In
particular, the BPM companion can suppress displayed lines while the artwork
page is selected, without deleting the cached source. The old same-track
`onTrack` path checked only for a new session payload. Returning to lyrics, or
restoring that page on another lock session, therefore left displayed lines
empty even though availability still found the cached lyric.

An explicit lyric-page selection now restores that track's cached lines before
waiting for any optional provider upgrade. The same entry can retry a previous
empty lookup when no display or cache exists. Attempts are at least 15 seconds
apart; ordinary metadata updates and render frames never trigger that retry.
An early empty result does not count as completion while later fallback sources
are still running. That pending gate expires at the existing 25-second lookup
watchdog budget, so a lost completion callback cannot permanently block entry.

A source-worker exception now posts an empty result and a finished callback on
main instead of leaving the page loading until its watchdog. Extended lookup
also accepts the temporary missing-controller state without dereferencing it to
test the Spotify package name. Real empty or instrumental results remain
unavailable; the page never manufactures lyrics or spins an automatic retry loop.

Six focused JVM cases cover cache restoration on same-track re-entry, restoration
during enrichment, bounded retry after a completed miss, in-flight exclusion,
missing tracks and partial-result versus source-completion ordering. A focused
production Java compile checks the integration. No phone was connected, read,
installed or reloaded during this change. These source defects are confirmed;
they do not establish that every reported absent lyric shares the same cause.
Later owner acceptance should use a track with known lyrics across repeated
artwork/lyrics switches and lock sessions, plus a genuinely lyricless track.
