package com.zhuhuibin.hypermusiccover.duo;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/** The reference clock uses ASCII vector digits, an unpadded hour and two-digit minutes. */
final class RailStatusTime {
    static String format(long now, TimeZone zone, boolean twentyFourHour) {
        Calendar clock = Calendar.getInstance(zone, Locale.ROOT);
        clock.setTimeInMillis(now);
        int hour = clock.get(twentyFourHour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!twentyFourHour && hour == 0) hour = 12;
        int minute = clock.get(Calendar.MINUTE);
        return hour + ":" + (minute < 10 ? "0" : "") + minute;
    }

    static long nextMinuteDelay(long now) {
        return 60_000L - Math.floorMod(now, 60_000L) + 20L;
    }

    private RailStatusTime() {}
}
