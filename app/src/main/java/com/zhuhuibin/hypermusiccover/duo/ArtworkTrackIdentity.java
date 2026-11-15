package com.zhuhuibin.hypermusiccover.duo;

/** Use stable song metadata when a player publishes the sung line as TITLE. */
final class ArtworkTrackIdentity {
    private ArtworkTrackIdentity() {}

    static String title(String packageName, String title, String artist) {
        if ("com.salt.music".equals(packageName) && artist != null) {
            int dash = artist.indexOf(" - ");
            if (dash >= 0 && dash + 3 < artist.length()) {
                String song = artist.substring(dash + 3).trim();
                if (!song.isEmpty()) return song;
            }
        }
        return title;
    }

    /** The bound session may be newer than its native header, but never belongs to another app. */
    static String entryKey(String nativeCardKey, String boundSessionKey) {
        String[] card = fields(nativeCardKey), session = fields(boundSessionKey);
        if (valid(session) && (card[0].isEmpty() || card[0].equals(session[0]))) {
            return boundSessionKey;
        }
        return valid(card) ? nativeCardKey : "";
    }

    private static String[] fields(String key) {
        String[] value = key == null ? new String[0] : key.split("\\|", 3);
        return new String[]{value.length > 0 ? value[0] : "", value.length > 1 ? value[1] : ""};
    }

    private static boolean valid(String[] fields) {
        return !fields[0].isEmpty() && !fields[1].isEmpty() && !"null".equals(fields[1]);
    }

    static boolean sameArtists(String pkg, String a, String b) {
        if (!"com.spotify.music".equals(pkg)) return false;
        java.util.Set<String> first = artists(a);
        return !first.isEmpty() && first.equals(artists(b));
    }

    private static java.util.Set<String> artists(String value) {
        java.util.Set<String> result = new java.util.HashSet<>();
        for (String artist : value.split(",")) {
            String name = artist.trim();
            if (!name.isEmpty()) result.add(name);
        }
        return result;
    }
}
