package com.zhuhuibin.hypermusiccover.duo;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArtworkTrackIdentityTest {
    @Test public void saltSungLinesDoNotChangeSongIdentity() {
        assertEquals("Song", ArtworkTrackIdentity.title("com.salt.music", "First line", "Artist - Song"));
        assertEquals("Song", ArtworkTrackIdentity.title("com.salt.music", "Next line", "Artist - Song"));
        assertEquals("New song", ArtworkTrackIdentity.title("com.salt.music", "Next line", "Artist - New song"));
    }

    @Test public void otherPlayersKeepTheirPublishedTitle() {
        assertEquals("Title", ArtworkTrackIdentity.title("com.google.android.apps.youtube.music", "Title", "Artist - Song"));
    }

    @Test public void malformedSaltArtistFallsBackToTitle() {
        assertEquals("Title", ArtworkTrackIdentity.title("com.salt.music", "Title", null));
        assertEquals("Title", ArtworkTrackIdentity.title("com.salt.music", "Title", "Artist"));
        assertEquals("Title", ArtworkTrackIdentity.title("com.salt.music", "Title", "Artist - "));
    }

    @Test public void songNamesCanContainDashes() {
        assertEquals("Song - Live", ArtworkTrackIdentity.title("com.salt.music", "Line", "Artist - Song - Live"));
    }
    @Test public void spotifyArtistReorderingDoesNotChangeArtworkIdentity() {
        assertTrue(ArtworkTrackIdentity.sameArtists("com.spotify.music", "Synthion, HYPERNIGHT", "HYPERNIGHT, Synthion"));
        assertFalse(ArtworkTrackIdentity.sameArtists("com.spotify.music", "Synthion", "Other"));
        assertFalse(ArtworkTrackIdentity.sameArtists("other.player", "A, B", "B, A"));
    }

    @Test public void firstEntryUsesTheCurrentNativeSessionEvenWhenTheOldArtworkKeyWasEmpty() {
        String session = "com.netease.cloudmusic|天下|张杰|明天过后";
        assertEquals(session, ArtworkTrackIdentity.entryKey("", session));
        assertEquals(session, ArtworkTrackIdentity.entryKey("com.netease.cloudmusic|old|张杰", session));
        assertEquals("天下", ArtworkTrackIdentity.title("com.netease.cloudmusic", "天下", "张杰"));
    }

    @Test public void aBoundNativeCardNeverBorrowsAnotherPlayersArtworkIdentity() {
        String card = "com.netease.cloudmusic|天下|张杰";
        assertEquals(card, ArtworkTrackIdentity.entryKey(card, "tv.danmaku.bili|video|uploader|album"));
        assertEquals(card, ArtworkTrackIdentity.entryKey(card, ""));
    }

    @Test public void missingSongMetadataUsesOnlyAValidNativeCardIdentity() {
        String card = "com.netease.cloudmusic|天下|张杰";
        assertEquals(card, ArtworkTrackIdentity.entryKey(card, "com.netease.cloudmusic|null|张杰|明天过后"));
        assertEquals("", ArtworkTrackIdentity.entryKey("", "com.netease.cloudmusic"));
        assertEquals("", ArtworkTrackIdentity.entryKey(null, "com.netease.cloudmusic||张杰|明天过后"));
        assertEquals("", ArtworkTrackIdentity.entryKey("com.netease.cloudmusic|null|张杰", ""));
    }
}
