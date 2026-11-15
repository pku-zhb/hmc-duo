package com.zhuhuibin.hypermusiccover.duo;

/** Bounded UI-thread evidence from existing layout/height callbacks; never schedules work. */
final class CompactPlayerFrameHistory {
    static final int CAPACITY = 256;
    static final int LAYOUT = 1, TARGET = 2, HEIGHT = 3, FULL_AOD = 4, COMPLETE = 5, RELEASE = 6;
    static final int EVENT = 0, REQUEST = 1, TARGET_HEIGHT = 2, ACTUAL_HEIGHT = 3,
            CARD_HEIGHT = 4, CARD_MEASURED = 5, CARD_LAYOUT = 6, BACKGROUND_TOP = 7,
            BACKGROUND_HEIGHT = 8, BACKGROUND_LAYOUT = 9, TITLE_TOP = 10,
            ART_MARGIN = 11, TITLE_MARGIN = 12, ACTION_MARGIN = 13, SEEK_VISIBILITY = 14,
            SEEK_ALPHA = 15, BACKGROUND_BIAS = 16, NORMAL_HEIGHT = 17, AOD_HEIGHT = 18,
            ARTWORK_HEIGHT = 19, FLAGS = 20;
    private static final int COLUMNS = 21;
    private static final String[] EVENTS = {"unknown", "layout", "target", "height", "fullAod", "complete", "release"};
    private static final String HEADER = "compactFrames=v1(uptime,event,request,target,actual,card,measured,cardLp,bgTop,bg,bgLp,titleTop,artMargin,titleMargin,actionMargin,seekVisibility,seekAlpha,bias,normal,aod,artwork,flags)";
    private final int[][] rows = new int[CAPACITY][COLUMNS];
    private final long[] times = new long[CAPACITY];
    private final int[] sample = new int[COLUMNS];
    private int next, size;

    int[] sample() { return sample; }
    int size() { return size; }

    void append(long uptime) {
        int event = sample[EVENT];
        if (size > 0 && event != TARGET && event != FULL_AOD && event != COMPLETE && event != RELEASE) {
            int[] previous = rows[(next + CAPACITY - 1) % CAPACITY];
            boolean same = true;
            // A height callback followed by pre-draw must not fill the ring with an
            // unchanged spring-animation tail just because the event names alternate.
            for (int i = 1; i < COLUMNS; i++) if (sample[i] != previous[i]) { same = false; break; }
            if (same) return;
        }
        System.arraycopy(sample, 0, rows[next], 0, COLUMNS);
        times[next] = uptime;
        next = (next + 1) % CAPACITY;
        size = Math.min(CAPACITY, size + 1);
    }

    String describe() {
        StringBuilder out = new StringBuilder(HEADER).append('[');
        for (int i = 0; i < size; i++) {
            int index = (next + CAPACITY - size + i) % CAPACITY;
            if (i > 0) out.append(';');
            out.append(times[index]);
            int[] row = rows[index];
            for (int column = 0; column < COLUMNS; column++) {
                out.append(',');
                if (column == EVENT) out.append(row[column] >= 0 && row[column] < EVENTS.length ? EVENTS[row[column]] : "unknown");
                else if (column == SEEK_ALPHA || column == BACKGROUND_BIAS)
                    out.append(Float.intBitsToFloat(row[column]));
                else out.append(row[column]);
            }
        }
        return out.append(']').toString();
    }
}
