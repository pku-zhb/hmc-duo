package com.os4.musiccover;

import org.junit.Test;
import static org.junit.Assert.*;

public class VerticalPillTextTest {
    @Test public void titleAndArtistBecomeTwoUprightColumns() {
        assertEquals("燕\n归\n巢", VerticalPillText.column("燕归巢", 7));
        assertEquals("许\n嵩", VerticalPillText.column("许嵩", 7));
    }
    @Test public void truncationReservesTheLastRowForAnEllipsis() {
        assertEquals("这\n是\n…", VerticalPillText.column("这是一首很长的歌曲", 3));
        assertEquals("…", VerticalPillText.column("很长", 1));
        assertEquals("", VerticalPillText.column("很长", 0));
    }
    @Test public void graphemeClustersAreNotSplitAcrossRows() {
        assertEquals("😀\n好", VerticalPillText.column("😀好", 3));
        assertEquals("e\u0301\n好", VerticalPillText.column("e\u0301好", 3));
        assertEquals("👨‍👩‍👧\n好", VerticalPillText.column("👨‍👩‍👧好", 3));
    }
    @Test public void emptyAndMultilineMetadataCannotCreateUnboundedColumns() {
        assertEquals("", VerticalPillText.column(null, 8));
        assertEquals("", VerticalPillText.column("  ", 8));
        assertEquals("A\n \nB", VerticalPillText.column("A\nB", 3));
    }
}
