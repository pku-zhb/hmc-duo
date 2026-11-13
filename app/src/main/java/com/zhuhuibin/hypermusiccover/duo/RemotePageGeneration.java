package com.zhuhuibin.hypermusiccover.duo;

/** A reply belongs to one prepared remote page, never the next page after release. */
final class RemotePageGeneration {
    private long generation;
    private boolean active;
    long begin() { active = true; return ++generation; }
    void retire() { active = false; ++generation; }
    long current() { return generation; }
    boolean accepts(long candidate) { return active && candidate == generation; }
}
