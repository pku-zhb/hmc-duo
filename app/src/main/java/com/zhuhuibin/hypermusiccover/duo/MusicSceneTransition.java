package com.zhuhuibin.hypermusiccover.duo;

/** Latest scene intent owns both card exclusivity and the eventual main-thread commit. */
final class MusicSceneTransition {
    interface Target {
        void collapseNonMusic();
        void commit(boolean musicScene);
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
        if (current(request)) target.commit(musicScene);
    }
}
