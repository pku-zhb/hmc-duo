# Media card customization

Adapted for HyperMusicCover Duo through HyperMusicCover-Enhanced from [juren233/HyperLyrics-Enhanced](https://github.com/juren233/HyperLyrics-Enhanced), revision `912df58`.

The background renderers, crossfade, soft-flow shader and palette extractor retain their upstream algorithms. The notification/island hook adapter, settings bridge, persistence and lifecycle integration are specific to this fork. No lyric hooks, audio capture, wake locks or screen keep-alive logic are imported.

Upstream project license: GPL-3.0, retained in `HyperLyrics-Enhanced-LICENSE.txt`. Files with explicit upstream Apache-2.0 notices (including the color extractor) retain their copyright and are attributed here: Copyright 2026 Proify, Tomakino, juren233. The Apache-2.0 license is retained in `Apache-2.0-LICENSE.txt`.

The active customization profile covers notification and lock-screen media cards.
Defaults preserve the native appearance. Animation stops when hidden or the display
is off; original material, outlines, visibility and foreground colors are restored
when customization is disabled.

Expanded Hyper Island cards retain HyperOS's native profile. Imported settings do
not activate a separate island theme. This compatibility path does not import the
reference module's notification-glass hook stack. Native capsule glass and the
lock-screen artwork background are separate from this media-card renderer.
