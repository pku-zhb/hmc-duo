package com.zhuhuibin.hypermusiccover.duo;

import java.time.Instant;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;

public class RailStatusTimeTest {
    @Test public void referenceHoursRemainUnpaddedAndMinutesStayPadded() {
        long now = Instant.parse("2026-11-12T09:04:00Z").toEpochMilli();
        assertEquals("9:04", RailStatusTime.format(now, TimeZone.getTimeZone("UTC"), true));
        assertEquals("9:04", RailStatusTime.format(now, TimeZone.getTimeZone("UTC"), false));
    }

    @Test public void formatChangesWithinAMinuteAndHandlesMidnightAndNoon() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        long midnight = Instant.parse("2026-11-12T00:02:30Z").toEpochMilli();
        long noon = Instant.parse("2026-11-12T12:02:30Z").toEpochMilli();
        long afternoon = Instant.parse("2026-11-12T13:02:30Z").toEpochMilli();
        assertEquals("0:02", RailStatusTime.format(midnight, utc, true));
        assertEquals("12:02", RailStatusTime.format(midnight, utc, false));
        assertEquals("12:02", RailStatusTime.format(noon, utc, false));
        assertEquals("1:02", RailStatusTime.format(afternoon, utc, false));
        assertEquals("13:02", RailStatusTime.format(afternoon, utc, true));
    }

    @Test public void timeZoneChangesRecomputeTheSameInstant() {
        long now = Instant.parse("2026-11-12T00:15:00Z").toEpochMilli();
        assertEquals("0:15", RailStatusTime.format(now, TimeZone.getTimeZone("UTC"), true));
        assertEquals("8:15", RailStatusTime.format(now, TimeZone.getTimeZone("Asia/Singapore"), true));
        assertEquals("5:45", RailStatusTime.format(now, TimeZone.getTimeZone("Asia/Kolkata"), true));
    }

    @Test public void resumedClockUsesCurrentTimeAndNextMinuteBoundary() {
        long now = Instant.parse("2026-11-12T08:59:59.999Z").toEpochMilli();
        assertEquals(21L, RailStatusTime.nextMinuteDelay(now));
        assertEquals("9:04", RailStatusTime.format(now + 240_001, TimeZone.getTimeZone("UTC"), true));
        assertEquals(60_020L, RailStatusTime.nextMinuteDelay(now + 240_001));
    }
}
