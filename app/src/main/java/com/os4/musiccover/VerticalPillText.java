package com.os4.musiccover;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;

/** Upright graphemes in a bounded top-to-bottom text column. */
final class VerticalPillText {
    private VerticalPillText() {}
    static String column(CharSequence text, int rows) {
        if (text == null || rows <= 0) return "";
        String value = text.toString().replace('\n', ' ').replace('\r', ' ').trim();
        BreakIterator breaks = BreakIterator.getCharacterInstance(Locale.ROOT);
        breaks.setText(value);
        ArrayList<String> glyphs = new ArrayList<>();
        int start = breaks.first();
        for (int end = breaks.next(); end != BreakIterator.DONE; start = end, end = breaks.next()) {
            glyphs.add(value.substring(start, end));
            if (glyphs.size() > rows) break;
        }
        if (glyphs.size() > rows) {
            glyphs.subList(rows, glyphs.size()).clear();
            glyphs.set(rows - 1, "…");
        }
        return String.join("\n", glyphs);
    }
}
