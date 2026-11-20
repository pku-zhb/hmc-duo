package com.zhuhuibin.hypermusiccover.duo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure-JVM v1 lockscreen handoff client state machine for HMC Duo.
 *
 * Coordinates independent per-provider sessions with MiDuo (0x7e484d01) and
 * Notification Optimizer / xmz (0x7e484d02), enforces cross-provider capability
 * gating, compensating release, zero-regression fallback when neither provider
 * is installed, and synchronous revocation cleanup before onState returns.
 */
public final class LockHandoffClientState {
    public static final int TAG_MIDUO = 0x7e484d01;
    public static final int TAG_XMZ = 0x7e484d02;

    public static final String PROTOCOL_MIDUO = "duo.lockhandoff.v1.miduo";
    public static final String PROTOCOL_XMZ = "duo.lockhandoff.v1.xmz";

    public static final String PKG_MIDUO = "dev.duo.hookprobe";
    public static final String PKG_XMZ = "com.miduonotification.xmz";

    public static final String CAP_SHORTCUTS = "lockscreen.shortcuts";
    public static final String CAP_STATUSBAR = "lockscreen.statusbar";
    public static final String CAP_TRIO = "lockscreen.trio";
    public static final String CAP_NOTIFICATIONS = "lockscreen.notifications";

    public static final int BIT_SHORTCUTS = 1 << 0;
    public static final int BIT_STATUSBAR = 1 << 1;
    public static final int BIT_TRIO = 1 << 2;
    public static final int BIT_NOTIFICATIONS = 1 << 3;
    public static final int ALL_CAPS =
            BIT_SHORTCUTS | BIT_STATUSBAR | BIT_TRIO | BIT_NOTIFICATIONS;
    public static final int CROSS_PROVIDER_CAPS = BIT_STATUSBAR | BIT_NOTIFICATIONS;

    public static final int PROVIDER_MIDUO = 0;
    public static final int PROVIDER_XMZ = 1;
    public static final int PROVIDER_COUNT = 2;

    public static final int PMASK_MIDUO = 1 << PROVIDER_MIDUO;
    public static final int PMASK_XMZ = 1 << PROVIDER_XMZ;

    public static final String METHOD_READ = "read";
    public static final String METHOD_ACQUIRE = "acquire";
    public static final String METHOD_RELEASE = "release";

    public static final String STATE_NONE = "NONE";
    public static final String STATE_PENDING = "PENDING";
    public static final String STATE_GRANTED = "GRANTED";

    public static final String ERR_BAD_REQUEST = "BAD_REQUEST";
    public static final String ERR_WRONG_THREAD = "WRONG_THREAD";
    public static final String ERR_STALE_GENERATION = "STALE_GENERATION";
    public static final String ERR_BAD_SESSION = "BAD_SESSION";
    public static final String ERR_STALE_REQUEST = "STALE_REQUEST";
    public static final String ERR_ID_CONFLICT = "ID_CONFLICT";
    public static final String ERR_BUSY = "BUSY";
    public static final String ERR_NOT_READY = "NOT_READY";

    public enum ProbeResult {
        READY,
        REPLACED,
        ABSENT,
        INVALID_TAG
    }

    public static final class Request {
        public final String method;
        public final boolean hasRequestId;
        public final long requestId;
        public final String[] capabilities;
        public final String generation;
        public final String sessionId;
        public final boolean includeCallback;

        public Request(String method, boolean hasRequestId, long requestId,
                String[] capabilities, String generation, String sessionId,
                boolean includeCallback) {
            this.method = method;
            this.hasRequestId = hasRequestId;
            this.requestId = requestId;
            this.capabilities = capabilities == null ? null : capabilities.clone();
            this.generation = generation;
            this.sessionId = sessionId;
            this.includeCallback = includeCallback;
        }
    }

    public static final class Response {
        public final String protocol;
        public final String state;
        public final String generation;
        public final String sessionId;
        public final boolean hasRequestId;
        public final long requestId;
        public final String[] granted;
        public final Map<String, String> denied;
        public final String error;

        public Response(String protocol, String state, String generation, String sessionId,
                boolean hasRequestId, long requestId, String[] granted,
                Map<String, String> denied, String error) {
            this.protocol = protocol;
            this.state = state;
            this.generation = generation;
            this.sessionId = sessionId;
            this.hasRequestId = hasRequestId;
            this.requestId = requestId;
            this.granted = granted == null ? new String[0] : granted.clone();
            this.denied = denied == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(denied));
            this.error = error;
        }
    }

    public interface Host {
        boolean isPackageInstalled(String packageName);

        ProbeResult probeEndpoint(int provider);

        Response callEndpoint(int provider, Request request, int callbackToken);

        void schedulePostAttachRetry(Object windowToken);

        void scheduleReconcile(Object windowToken);

        /**
         * Synchronously stop HMC writes and restore modified properties for all bits
         * in {@code revokedMask}. Must run synchronously before returning from onState
         * or detach; never defer via post().
         */
        void onCapabilitiesRevokedSync(int revokedMask, int effectiveMask);

        /**
         * Optional notification when newly granted capabilities become effective.
         */
        void onCapabilitiesGained(int gainedMask, int effectiveMask);

        void logWarn(String message);
    }

    private static final class EndpointSlot {
        final int provider;
        final String packageName;
        final String protocol;
        final int supportedMask;

        boolean installed;
        boolean endpointReady;
        boolean replacing;
        String generation;
        String sessionId;
        long nextRequestId = 1L;
        int grantedMask;
        int pendingMask;
        long pendingRequestId;
        boolean pendingInitialAcquire;
        int releasingMask;
        int compensationMask;
        int unpairedHeldMask;
        int lastDeniedMask;
        int callbackToken;

        EndpointSlot(int provider, String packageName, String protocol, int supportedMask) {
            this.provider = provider;
            this.packageName = packageName;
            this.protocol = protocol;
            this.supportedMask = supportedMask;
        }

        void resetSession() {
            generation = null;
            sessionId = null;
            nextRequestId = 1L;
            grantedMask = 0;
            pendingMask = 0;
            pendingRequestId = 0;
            pendingInitialAcquire = false;
            releasingMask = 0;
            compensationMask = 0;
            unpairedHeldMask = 0;
            callbackToken++;
        }

        void resetWindow() {
            endpointReady = false;
            lastDeniedMask = 0;
            resetSession();
        }
    }

    private final Host host;
    private final EndpointSlot[] slots = new EndpointSlot[PROVIDER_COUNT];

    private boolean windowAttached;
    private Object currentWindowToken;
    private boolean pendingPostProbe;
    private boolean reconcilePosted;
    private int asyncDeniedMask;
    private int desiredMask;
    private volatile int effectiveMask;
    private int inCallbackDepth;
    private boolean cleaningUp;
    private boolean reconciling;

    public LockHandoffClientState(Host host) {
        if (host == null) throw new IllegalArgumentException("host == null");
        this.host = host;
        slots[PROVIDER_MIDUO] = new EndpointSlot(
                PROVIDER_MIDUO, PKG_MIDUO, PROTOCOL_MIDUO, ALL_CAPS);
        slots[PROVIDER_XMZ] = new EndpointSlot(
                PROVIDER_XMZ, PKG_XMZ, PROTOCOL_XMZ, BIT_STATUSBAR | BIT_NOTIFICATIONS);
        slots[PROVIDER_MIDUO].installed = safeIsInstalled(PKG_MIDUO);
        slots[PROVIDER_XMZ].installed = safeIsInstalled(PKG_XMZ);
        this.effectiveMask = computeEffectiveMask();
    }

    public static int requiredProvidersForCap(int capBit) {
        switch (capBit) {
            case BIT_SHORTCUTS:
            case BIT_TRIO:
                return PMASK_MIDUO;
            case BIT_STATUSBAR:
            case BIT_NOTIFICATIONS:
                return PMASK_MIDUO | PMASK_XMZ;
            default:
                return 0;
        }
    }

    public static String protocolForProvider(int provider) {
        return provider == PROVIDER_MIDUO ? PROTOCOL_MIDUO : PROTOCOL_XMZ;
    }

    public static int tagKeyForProvider(int provider) {
        return provider == PROVIDER_MIDUO ? TAG_MIDUO : TAG_XMZ;
    }

    public static int capNameToBit(String name) {
        if (CAP_SHORTCUTS.equals(name)) return BIT_SHORTCUTS;
        if (CAP_STATUSBAR.equals(name)) return BIT_STATUSBAR;
        if (CAP_TRIO.equals(name)) return BIT_TRIO;
        if (CAP_NOTIFICATIONS.equals(name)) return BIT_NOTIFICATIONS;
        return 0;
    }

    public static int capsToMask(String[] caps) {
        if (caps == null) return 0;
        int mask = 0;
        for (String cap : caps) {
            mask |= capNameToBit(cap);
        }
        return mask;
    }

    public static int capsToMask(Iterable<String> caps) {
        if (caps == null) return 0;
        int mask = 0;
        for (String cap : caps) {
            mask |= capNameToBit(cap);
        }
        return mask;
    }

    public static String[] maskToCaps(int mask) {
        List<String> out = new ArrayList<>(4);
        if ((mask & BIT_SHORTCUTS) != 0) out.add(CAP_SHORTCUTS);
        if ((mask & BIT_STATUSBAR) != 0) out.add(CAP_STATUSBAR);
        if ((mask & BIT_TRIO) != 0) out.add(CAP_TRIO);
        if ((mask & BIT_NOTIFICATIONS) != 0) out.add(CAP_NOTIFICATIONS);
        return out.toArray(new String[0]);
    }

    public int effectiveMask() {
        return effectiveMask;
    }

    public boolean allowsShortcuts() {
        return (effectiveMask & BIT_SHORTCUTS) != 0;
    }

    public boolean allowsStatusBar() {
        return (effectiveMask & BIT_STATUSBAR) != 0;
    }

    public boolean allowsTrio() {
        return (effectiveMask & BIT_TRIO) != 0;
    }

    public boolean allowsNotifications() {
        return (effectiveMask & BIT_NOTIFICATIONS) != 0;
    }

    public boolean isProviderInstalled(int provider) {
        return provider >= 0 && provider < PROVIDER_COUNT && slots[provider].installed;
    }

    public boolean isProviderEndpointReady(int provider) {
        return provider >= 0 && provider < PROVIDER_COUNT && slots[provider].endpointReady;
    }

    public int providerGrantedMask(int provider) {
        return provider >= 0 && provider < PROVIDER_COUNT ? slots[provider].grantedMask : 0;
    }

    public int providerPendingMask(int provider) {
        return provider >= 0 && provider < PROVIDER_COUNT ? slots[provider].pendingMask : 0;
    }

    public String providerGeneration(int provider) {
        return provider >= 0 && provider < PROVIDER_COUNT ? slots[provider].generation : null;
    }

    public String providerSessionId(int provider) {
        return provider >= 0 && provider < PROVIDER_COUNT ? slots[provider].sessionId : null;
    }

    public boolean isWindowAttached() {
        return windowAttached;
    }

    public int desiredMask() {
        return desiredMask;
    }

    /**
     * Called when NotificationShadeWindowView attaches to window on the main thread.
     */
    public void onWindowAttached(Object windowToken) {
        onWindowAttached(windowToken, desiredMask);
    }

    /**
     * Called when NotificationShadeWindowView attaches to window with an updated desired mask.
     */
    public void onWindowAttached(Object windowToken, int newDesiredMask) {
        if (windowToken == null) return;
        if (windowAttached && currentWindowToken == windowToken) {
            syncDesired(newDesiredMask);
            return;
        }
        if (windowAttached) {
            detachCurrentWindowInternal();
        }
        desiredMask = newDesiredMask & ALL_CAPS;
        windowAttached = true;
        currentWindowToken = windowToken;
        for (EndpointSlot slot : slots) {
            slot.resetWindow();
            if (!slot.replacing) slot.installed = safeIsInstalled(slot.packageName);
        }
        recomputeEffectiveAndSyncCleanup();
        boolean hasAbsentInstalled = probeInstalledEndpoints();
        reconcile();
        if (hasAbsentInstalled && windowAttached && currentWindowToken == windowToken) {
            pendingPostProbe = true;
            try {
                host.schedulePostAttachRetry(windowToken);
            } catch (Throwable error) {
                pendingPostProbe = false;
                host.logWarn("schedulePostAttachRetry failed: " + error);
            }
        }
    }

    /**
     * One-shot post-attach retry after NotificationShadeWindowView.onAttachedToWindow exits.
     */
    public void onPostAttachRetry(Object windowToken) {
        if (!windowAttached || currentWindowToken != windowToken || !pendingPostProbe) {
            return;
        }
        pendingPostProbe = false;
        probeInstalledEndpoints();
        reconcile();
    }

    /**
     * Called when NotificationShadeWindowView detaches from window on the main thread.
     * Late detaches from older windows are ignored.
     */
    public void onWindowDetached(Object windowToken) {
        if (!windowAttached || currentWindowToken != windowToken) {
            return;
        }
        detachCurrentWindowInternal();
    }

    private void detachCurrentWindowInternal() {
        windowAttached = false;
        currentWindowToken = null;
        pendingPostProbe = false;
        reconcilePosted = false;
        asyncDeniedMask = 0;
        for (EndpointSlot slot : slots) {
            slot.resetWindow();
        }
        recomputeEffectiveAndSyncCleanup();
    }

    /**
     * Updates the desired capability mask based on HMC settings/scene and reconciles
     * with installed providers on the main thread.
     */
    public void syncDesired(int newDesiredMask) {
        desiredMask = newDesiredMask & ALL_CAPS;
        if (inCallbackDepth > 0 || cleaningUp || reconciling) {
            return;
        }
        if (!windowAttached) {
            recomputeEffectiveAndSyncCleanup();
            return;
        }
        // Re-check the current tags on scene changes, never per frame.
        asyncDeniedMask = 0;
        probeInstalledEndpoints();
        reconcile();
    }

    public void onPackageReplacementStarted(String packageName) {
        for (EndpointSlot slot : slots) {
            if (!slot.packageName.equals(packageName)) continue;
            slot.installed = true;
            slot.replacing = true;
            slot.endpointReady = false;
            recomputeEffectiveAndSyncCleanup();
            return;
        }
    }

    /** Package broadcasts do not replace a Function already loaded in SystemUI. */
    public void onPackageChanged(String packageName, boolean installedNow) {
        for (EndpointSlot slot : slots) {
            if (!slot.packageName.equals(packageName)) continue;
            if (!installedNow && slot.installed) {
                slot.endpointReady = false;
                recomputeEffectiveAndSyncCleanup();
            }
            slot.replacing = false;
            slot.installed = installedNow;
            slot.lastDeniedMask = 0;
            if (!bothProvidersInstalled()) {
                for (EndpointSlot endpoint : slots) endpoint.compensationMask = 0;
            }
            if (installedNow) recomputeEffectiveAndSyncCleanup();
            if (windowAttached) {
                asyncDeniedMask = 0;
                probeInstalledEndpoints();
                if (slot.endpointReady && slot.generation != null && !performRead(slot)) {
                    slot.endpointReady = false;
                }
                reconcile();
            } else {
                recomputeEffectiveAndSyncCleanup();
            }
            return;
        }
    }

    public void refreshInstalledPackages() {
        for (EndpointSlot slot : slots) {
            if (!slot.replacing) {
                boolean installedNow = safeIsInstalled(slot.packageName);
                if (installedNow != slot.installed) onPackageChanged(slot.packageName, installedNow);
            }
        }
    }

    /**
     * A local ResultReceiver runs inside the provider's callback. Publish and restore
     * synchronously, then let the provider return before sending any next request.
     */
    public void onEndpointStatePush(int provider, int callbackToken, Response snapshot) {
        if (provider < 0 || provider >= PROVIDER_COUNT) return;
        EndpointSlot slot = slots[provider];
        if (!windowAttached || slot.callbackToken != callbackToken || !isProtocolValid(slot, snapshot)
                || !isStateValid(snapshot.state)) {
            return;
        }
        inCallbackDepth++;
        try {
            int failed = 0;
            boolean completed = false;
            if (!Objects.equals(slot.generation, snapshot.generation)) {
                failed = slot.grantedMask | slot.pendingMask;
                slot.resetSession();
                slot.lastDeniedMask = 0;
                slot.generation = snapshot.generation;
                if (slot.generation == null) slot.endpointReady = false;
            } else if (!snapshot.hasRequestId && STATE_NONE.equals(snapshot.state)) {
                failed = slot.grantedMask | slot.pendingMask;
                slot.resetSession();
                slot.generation = snapshot.generation;
                if (slot.generation == null) slot.endpointReady = false;
                slot.lastDeniedMask |= failed & slot.supportedMask;
            } else {
                int prevPending = slot.pendingMask;
                int prevGranted = slot.grantedMask & ~slot.releasingMask;
                if (snapshot.hasRequestId) {
                    if (snapshot.requestId != slot.pendingRequestId) return;
                    completed = !STATE_PENDING.equals(snapshot.state);
                    applySnapshot(slot, snapshot, prevPending);
                } else {
                    // Unsolicited snapshots may revoke but cannot establish ownership.
                    slot.grantedMask &= capsToMask(snapshot.granted);
                }
                failed = (prevPending | prevGranted) & ~(slot.pendingMask | slot.grantedMask);
                failed |= capsToMask(snapshot.denied.keySet());
                slot.lastDeniedMask = (slot.lastDeniedMask & ~(slot.grantedMask | slot.pendingMask))
                        | (failed & slot.supportedMask);
            }
            asyncDeniedMask |= failed;
            if (queueCompensation(1 - provider, failed) || (completed && hasFollowUpWork())) {
                if (!reconcilePosted) {
                    reconcilePosted = true;
                    host.scheduleReconcile(currentWindowToken);
                }
            }
            recomputeEffectiveAndSyncCleanup();
        } finally {
            inCallbackDepth--;
        }
    }

    public void onPostedReconcile(Object windowToken) {
        if (!windowAttached || currentWindowToken != windowToken || !reconcilePosted) return;
        reconcilePosted = false;
        probeInstalledEndpoints();
        reconcile();
    }

    private boolean hasFollowUpWork() {
        for (EndpointSlot slot : slots) {
            if (!slot.endpointReady || slot.pendingRequestId != 0) continue;
            int target = computeProviderTargetMask(slot.provider) & ~asyncDeniedMask;
            if ((slot.grantedMask & ~desiredMask) != 0 || (target & ~slot.grantedMask) != 0
                    || slot.compensationMask != 0) return true;
        }
        return false;
    }

    private boolean probeInstalledEndpoints() {
        boolean anyAbsent = false;
        for (EndpointSlot slot : slots) {
            if (!slot.installed || slot.replacing) continue;
            ProbeResult result = safeProbe(slot.provider);
            if (result == ProbeResult.REPLACED) {
                slot.endpointReady = false;
                slot.lastDeniedMask = 0;
                slot.resetSession();
                recomputeEffectiveAndSyncCleanup();
            }
            if (result == ProbeResult.INVALID_TAG && (slot.endpointReady || slot.generation == null)) {
                host.logWarn("Invalid or collided handoff tag for " + slot.protocol);
            }
            slot.endpointReady = result == ProbeResult.READY || result == ProbeResult.REPLACED;
            anyAbsent |= result == ProbeResult.ABSENT;
        }
        return anyAbsent;
    }

    private boolean queueCompensation(int provider, int failed) {
        if (!bothProvidersInstalled()) return false;
        EndpointSlot slot = slots[provider];
        int compensation = failed & slot.unpairedHeldMask;
        slot.compensationMask |= compensation;
        return compensation != 0;
    }

    private void reconcile() {
        if (inCallbackDepth > 0 || reconciling || cleaningUp || !windowAttached) return;
        reconciling = true;
        try {
            // Bounded to at most 2 passes: if a synchronous compensating release cancels an
            // initial PENDING acquire (or a peer's previously denied shared capability clears),
            // issue one follow-up acquire for the remaining eligible capabilities.
            for (int pass = 0; pass < 2; pass++) {
                int failed = asyncDeniedMask;
                for (EndpointSlot slot : slots) failed |= slot.compensationMask;

                // Process outstanding returns before taking any more ownership.
                int attemptedRelease = 0;
                for (EndpointSlot slot : slots) {
                    if (releaseUnwanted(slot)) attemptedRelease |= 1 << slot.provider;
                }
                int[] denied = new int[PROVIDER_COUNT];
                int attemptedAcquire = 0;
                boolean anyProgress = false;
                for (EndpointSlot slot : slots) {
                    if (!slot.endpointReady || slot.pendingRequestId != 0) continue;
                    int target = computeProviderTargetMask(slot.provider) & ~failed;
                    int toAcquire = target & ~slot.grantedMask;
                    if (toAcquire != 0) {
                        attemptedAcquire |= 1 << slot.provider;
                        denied[slot.provider] = acquireFromProvider(slot, target, toAcquire);
                        asyncDeniedMask |= denied[slot.provider];
                        // Clear newly recovered bits immediately so a later slot in the same pass
                        // can acquire them in one shot; record new denials after the pass so
                        // same-pass compensation stays symmetric.
                        int cleared = slot.lastDeniedMask & (slot.grantedMask | slot.pendingMask);
                        if (cleared != 0) {
                            slot.lastDeniedMask &= ~cleared;
                            anyProgress = true;
                        }
                    }
                }
                for (EndpointSlot slot : slots) {
                    if ((attemptedAcquire & (1 << slot.provider)) == 0) continue;
                    slot.lastDeniedMask |= denied[slot.provider] & slot.supportedMask;
                }
                for (EndpointSlot slot : slots) {
                    int prevPending = slot.pendingMask;
                    queueCompensation(slot.provider, denied[1 - slot.provider]);
                    if (slot.compensationMask != 0 && (attemptedRelease & (1 << slot.provider)) == 0) {
                        if (releaseUnwanted(slot) && prevPending != 0 && slot.pendingMask == 0) {
                            anyProgress = true;
                        }
                    }
                }
                if (!anyProgress || !hasFollowUpWork()) {
                    break;
                }
            }
            recomputeEffectiveAndSyncCleanup();
        } finally {
            reconciling = false;
        }
    }

    private boolean releaseUnwanted(EndpointSlot slot) {
        if (!slot.endpointReady || slot.sessionId == null
                || (slot.pendingRequestId != 0 && !slot.pendingInitialAcquire)) return false;
        int toRelease = (slot.grantedMask | slot.pendingMask) & (~desiredMask | slot.compensationMask);
        if (toRelease == 0) return false;
        // The provider may retain ownership on BUSY, and may list the returned item
        // in granted until its animation settles. Neither response allows HMC writes.
        recomputeEffectiveAndSyncCleanup();
        sendReleaseRequest(slot, toRelease);
        return true;
    }

    private int acquireFromProvider(EndpointSlot slot, int target, int toAcquire) {
        if (slot.generation == null && !performRead(slot)) return toAcquire;
        for (int attempt = 0; attempt < 2; attempt++) {
            int prevGranted = slot.grantedMask;
            long reqId = slot.nextRequestId++;
            boolean initialSession = slot.sessionId == null;
            Request request = new Request(METHOD_ACQUIRE, true, reqId, maskToCaps(toAcquire),
                    slot.generation, slot.sessionId, initialSession);
            Response response = safeCall(slot, request, slot.callbackToken);
            if (!isModifyResponseValid(slot, request, response)) {
                markTagProtocolInvalid(slot);
                return toAcquire;
            }
            if (response.error == null) {
                slot.pendingRequestId = reqId;
                slot.pendingInitialAcquire = initialSession;
                applySnapshot(slot, response, toAcquire);
                if (bothProvidersInstalled()) {
                    slot.unpairedHeldMask |= ((slot.grantedMask & ~prevGranted) | slot.pendingMask)
                            & CROSS_PROVIDER_CAPS;
                }
                return (toAcquire | prevGranted) & ~(slot.grantedMask | slot.pendingMask);
            }
            if (!recoverSession(slot, response)) return toAcquire;
            toAcquire = target;
            if (slot.generation == null && !performRead(slot)) return toAcquire;
        }
        return toAcquire;
    }

    private boolean recoverSession(EndpointSlot slot, Response response) {
        boolean stale = ERR_STALE_GENERATION.equals(response.error);
        if (!stale && !ERR_BAD_SESSION.equals(response.error)) return false;
        String generation = response.generation != null ? response.generation
                : (stale ? null : slot.generation);
        long nextRequestId = slot.nextRequestId;
        boolean sameGeneration = generation != null && generation.equals(slot.generation);
        slot.resetSession();
        slot.generation = generation;
        if (sameGeneration) slot.nextRequestId = nextRequestId;
        else slot.lastDeniedMask = 0;
        recomputeEffectiveAndSyncCleanup();
        return true;
    }

    private void applySnapshot(EndpointSlot slot, Response response, int acquireMask) {
        int denied = capsToMask(response.denied.keySet());
        slot.grantedMask = capsToMask(response.granted) & slot.supportedMask;
        slot.sessionId = response.sessionId;
        if (STATE_PENDING.equals(response.state)) {
            slot.pendingMask = acquireMask & ~slot.grantedMask & ~denied;
        } else {
            slot.pendingRequestId = 0;
            slot.pendingInitialAcquire = false;
            slot.pendingMask = 0;
            slot.releasingMask = 0;
            if (slot.sessionId == null) slot.callbackToken++;
        }
        int held = slot.grantedMask | slot.pendingMask;
        slot.unpairedHeldMask &= held;
        slot.compensationMask &= held;
    }

    private boolean performRead(EndpointSlot slot) {
        Request req = new Request(METHOD_READ, false, 0L, null, null, null, false);
        Response resp = safeCall(slot, req, slot.callbackToken);
        if (!isProtocolValid(slot, resp)) {
            markTagProtocolInvalid(slot);
            return false;
        }
        if (resp.error != null || !isStateValid(resp.state)
                || resp.generation == null || resp.generation.isEmpty()) {
            return false;
        }
        if (!resp.generation.equals(slot.generation)) {
            slot.generation = resp.generation;
            slot.nextRequestId = 1L;
            slot.sessionId = null;
            slot.grantedMask = 0;
            slot.pendingMask = 0;
            slot.unpairedHeldMask = 0;
            slot.lastDeniedMask = 0;
            slot.callbackToken++;
        }
        return true;
    }

    private void sendReleaseRequest(EndpointSlot slot, int toRelease) {
        long reqId = slot.nextRequestId++;
        Request request = new Request(METHOD_RELEASE, true, reqId, maskToCaps(toRelease),
                slot.generation, slot.sessionId, false);
        Response response = safeCall(slot, request, slot.callbackToken);
        if (!isModifyResponseValid(slot, request, response)) {
            markTagProtocolInvalid(slot);
            return;
        }
        if (response.error != null) {
            recoverSession(slot, response);
            return;
        }
        slot.pendingRequestId = reqId;
        slot.pendingInitialAcquire = false;
        slot.releasingMask = toRelease;
        applySnapshot(slot, response, 0);
    }

    private int computeProviderTargetMask(int provider) {
        EndpointSlot slot = slots[provider];
        if (!slot.installed || !slot.endpointReady) return 0;
        int target = desiredMask & slot.supportedMask;
        if (target == 0) return 0;
        if (bothProvidersInstalled()) {
            EndpointSlot peer = slots[1 - provider];
            if (!peer.endpointReady) {
                // Peer is installed but not yet ready/collided: hold off cross-provider capabilities.
                target &= ~CROSS_PROVIDER_CAPS;
            } else {
                // Hold off shared capabilities previously denied only by peer until peer re-probes.
                int peerBlocked = peer.lastDeniedMask & ~slot.lastDeniedMask & CROSS_PROVIDER_CAPS;
                target &= ~peerBlocked;
            }
        }
        return target;
    }

    private boolean bothProvidersInstalled() {
        return slots[PROVIDER_MIDUO].installed && slots[PROVIDER_XMZ].installed;
    }

    private int computeEffectiveMask() {
        int mask = 0;
        int[] bits = {BIT_SHORTCUTS, BIT_STATUSBAR, BIT_TRIO, BIT_NOTIFICATIONS};
        for (int capBit : bits) {
            if (isCapabilityEffective(capBit)) {
                mask |= capBit;
            }
        }
        return mask;
    }

    private boolean isCapabilityEffective(int capBit) {
        int reqProviders = requiredProvidersForCap(capBit);
        for (int p = 0; p < PROVIDER_COUNT; p++) {
            if ((reqProviders & (1 << p)) == 0) continue;
            EndpointSlot slot = slots[p];
            if (!slot.installed) continue;
            if (!windowAttached || !slot.endpointReady || (desiredMask & capBit) == 0
                    || (slot.grantedMask & capBit) == 0
                    || ((slot.releasingMask | slot.compensationMask) & capBit) != 0) {
                return false;
            }
        }
        return true;
    }

    private void recomputeEffectiveAndSyncCleanup() {
        int prev = effectiveMask;
        int next = computeEffectiveMask();
        int lost = prev & ~next;
        int gained = next & ~prev;
        for (EndpointSlot slot : slots) slot.unpairedHeldMask &= ~next;
        effectiveMask = next;

        if (lost != 0) {
            boolean prevCleaning = cleaningUp;
            cleaningUp = true;
            try {
                host.onCapabilitiesRevokedSync(lost, next);
            } catch (Throwable error) {
                host.logWarn("onCapabilitiesRevokedSync threw: " + error);
            } finally {
                cleaningUp = prevCleaning;
            }
        }
        if (gained != 0) {
            try {
                host.onCapabilitiesGained(gained, next);
            } catch (Throwable error) {
                host.logWarn("onCapabilitiesGained threw: " + error);
            }
        }
    }

    private boolean isProtocolValid(EndpointSlot slot, Response resp) {
        return resp != null && slot.protocol.equals(resp.protocol);
    }

    private boolean isModifyResponseValid(EndpointSlot slot, Request req, Response response) {
        if (!isProtocolValid(slot, response) || !isStateValid(response.state)) return false;
        if (response.hasRequestId && response.requestId != req.requestId) return false;
        if (response.error != null) return true;
        return response.hasRequestId && req.generation.equals(response.generation)
                && (STATE_NONE.equals(response.state) ? response.sessionId == null
                        : response.sessionId != null
                                && (req.sessionId == null || req.sessionId.equals(response.sessionId)));
    }

    private static boolean isStateValid(String state) {
        return STATE_NONE.equals(state) || STATE_PENDING.equals(state) || STATE_GRANTED.equals(state);
    }

    private void markTagProtocolInvalid(EndpointSlot slot) {
        slot.endpointReady = false;
        slot.resetSession();
        host.logWarn("Endpoint protocol mismatch or error on " + slot.protocol + "; rejected safely");
        recomputeEffectiveAndSyncCleanup();
    }

    private boolean safeIsInstalled(String pkg) {
        try {
            return host.isPackageInstalled(pkg);
        } catch (Throwable error) {
            host.logWarn("isPackageInstalled(" + pkg + ") failed: " + error);
            return true;
        }
    }

    private ProbeResult safeProbe(int provider) {
        try {
            ProbeResult r = host.probeEndpoint(provider);
            return r == null ? ProbeResult.ABSENT : r;
        } catch (Throwable error) {
            host.logWarn("probeEndpoint(" + provider + ") threw: " + error);
            return ProbeResult.INVALID_TAG;
        }
    }

    private Response safeCall(EndpointSlot slot, Request req, int callbackToken) {
        try {
            return host.callEndpoint(slot.provider, req, callbackToken);
        } catch (Throwable error) {
            host.logWarn("callEndpoint(" + slot.protocol + ") threw: " + error);
            return null;
        }
    }
}
