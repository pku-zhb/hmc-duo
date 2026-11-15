package com.zhuhuibin.hypermusiccover.duo;

/** Latest scene intent owns both card exclusivity and the eventual main-thread commit. */
final class MusicSceneTransition {
    interface Target {
        void collapseNonMusic();
        void commit(boolean musicScene);
        void pushArtwork(boolean musicScene);
    }

    private long generation;
    private boolean wanted;

    synchronized long request(boolean musicScene) {
        wanted = musicScene;
        return ++generation;
    }

    synchronized boolean wanted() { return wanted; }

    private synchronized boolean current(long request) { return request == generation; }

    void apply(long request, Target target) {
        if (!current(request)) return;
        boolean musicScene = wanted();
        if (musicScene) target.collapseNonMusic();
        // Collapsing a native card can synchronously deliver a newer presentation action.
        if (!current(request)) return;
        target.commit(musicScene);
        // Committing the native pose can synchronously request another scene as well. Only
        // its latest owner may dispatch work after the mode and track identity are published.
        if (current(request)) target.pushArtwork(musicScene);
    }
}
