package com.zhuhuibin.hypermusiccover.duo;

import java.util.ArrayList;
import java.util.Collection;

/** Independent cards coexist; a gesture closes one non-music card before the music. */
final class RailDismissOrder {
    private final ArrayList<String> opened = new ArrayList<>();
    void opened(String key) { opened.remove(key); opened.add(key); }
    void closed(String key) { opened.remove(key); }
    /** A music close is pending only for the live animation, never for a remembered key. */
    String next(Collection<String> available, String music, Collection<String> closing, boolean musicClosing) {
        ArrayList<String> candidates = new ArrayList<>();
        for (String key : available) {
            if (key.equals(music) ? !musicClosing : !closing.contains(key)) candidates.add(key);
        }
        return next(candidates, music);
    }

    String next(Collection<String> available, String music) {
        opened.removeIf(key -> !available.contains(key));
        for (int i = opened.size() - 1; i >= 0; i--)
            if (!opened.get(i).equals(music)) return opened.get(i);
        for (String key : available) if (!key.equals(music)) return key;
        return available.contains(music) ? music : null;
    }
}
