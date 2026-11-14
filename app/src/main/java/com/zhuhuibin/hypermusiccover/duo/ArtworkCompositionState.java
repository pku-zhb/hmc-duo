package com.zhuhuibin.hypermusiccover.duo;

/** Main requests, workers check, and Main adopts only the latest complete composition inputs. */
final class ArtworkCompositionState {
    private ArtworkCompositionKey desired, pending, committed;

    synchronized ArtworkCompositionKey desired() { return desired; }
    synchronized ArtworkCompositionKey pending() { return pending; }
    synchronized ArtworkCompositionKey committed() { return committed; }

    /** Keep the outgoing committed image while deduplicating only matching work or output. */
    synchronized boolean request(ArtworkCompositionKey key) {
        desired = key;
        if (key == null || !key.valid()) { pending = null; return false; }
        if (key.same(pending)) return false;
        if (key.same(committed)) { pending = null; return false; }
        pending = key;
        return true;
    }

    synchronized boolean accepts(ArtworkCompositionKey key) {
        return desired != null && desired.valid() && desired.same(key);
    }

    synchronized boolean commit(ArtworkCompositionKey key) {
        if (!accepts(key)) return false;
        committed = key;
        pending = null;
        return true;
    }

    /** A failed old worker cannot cancel a newer request; the latest failed key can be retried. */
    synchronized boolean cancel(ArtworkCompositionKey key) {
        if (key == null || !key.same(pending)) return false;
        pending = null;
        return true;
    }

    synchronized void clear() { desired = pending = committed = null; }
}
