package com.zhuhuibin.hypermusiccover.duo;

import java.util.Collections;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class MediaTextBoundsPolicyTest {
    private final MediaTextBoundsPolicy.Bounds title = new MediaTextBoundsPolicy.Bounds(100, 10, 400, 40);
    private final MediaTextBoundsPolicy.Bounds output = new MediaTextBoundsPolicy.Bounds(340, 5, 400, 45);
    @Test public void sameViewChangingToALongTitleKeepsTheOutputBoundsReserved() {
        MediaTextBoundsPolicy.Insets first = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.singletonList(output));
        MediaTextBoundsPolicy.Insets next = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.singletonList(output));
        assertEquals(50, first.left);
        assertEquals(60, next.right);
        assertEquals(first.left, next.left);
        assertEquals(340, 400 - next.right);
    }
    @Test public void newlyVisibleControlReservesItsMeasuredWidthAndHidingRestoresIt() {
        MediaTextBoundsPolicy.Insets hidden = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.emptyList());
        MediaTextBoundsPolicy.Insets visible = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.singletonList(output));
        MediaTextBoundsPolicy.Insets hiddenAgain = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.emptyList());
        assertEquals(6, hidden.right);
        assertEquals(60, visible.right);
        assertEquals(hidden.right, hiddenAgain.right);
    }
    @Test public void transportControlsBelowTheTextDoNotCreateADeadRightHandGap() {
        MediaTextBoundsPolicy.Bounds action = new MediaTextBoundsPolicy.Bounds(300, 60, 360, 100);
        MediaTextBoundsPolicy.Insets result = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.singletonList(action));
        assertEquals(6, result.right);
    }
    @Test public void outputWidthAndArtistRowAreMeasuredIndependently() {
        MediaTextBoundsPolicy.Bounds artist = new MediaTextBoundsPolicy.Bounds(100, 42, 400, 65);
        MediaTextBoundsPolicy.Bounds titleOnly = new MediaTextBoundsPolicy.Bounds(280, 5, 400, 40);
        assertEquals(120, MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Collections.singletonList(titleOnly)).right);
        assertEquals(6, MediaTextBoundsPolicy.reserve(artist, 4, 6, 46, Collections.singletonList(titleOnly)).right);
    }
    @Test public void multipleNativeActionsUseTheirActualEdgesWithoutAssumingButtonOrder() {
        MediaTextBoundsPolicy.Bounds custom = new MediaTextBoundsPolicy.Bounds(300, 10, 330, 40);
        MediaTextBoundsPolicy.Insets result = MediaTextBoundsPolicy.reserve(title, 4, 6, 46, Arrays.asList(output, custom));
        assertEquals(100, result.right);
    }
    @Test public void narrowLayoutsNeverCreateANegativeTextViewport() {
        MediaTextBoundsPolicy.Bounds narrow = new MediaTextBoundsPolicy.Bounds(100, 10, 140, 40);
        MediaTextBoundsPolicy.Insets result = MediaTextBoundsPolicy.reserve(narrow, 4, 6, 46, Collections.singletonList(output));
        assertEquals(40, result.left + result.right);
    }
}
