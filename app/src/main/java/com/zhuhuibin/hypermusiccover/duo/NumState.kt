package com.zhuhuibin.hypermusiccover.duo

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The keyguard stack's "N notifications" state (KeyguardNotificationState.NUMBER) as the stack
 * island's folded form - ColorOS's way (read 2026-09-30): the ordinary notifications' rows never
 * leave the stack; folded, the stack itself hides them (numStateAlpha 0, piled at its bottom) and
 * the island stands where its "N个通知" line was; opened, the stack scrolls them out as its list.
 *
 * Everything here is SystemUI's own path, read from this build (md5 4ddaa4a0): the state is
 * where the stack is scrolled to, and going to one is scrolling there, as the full-screen AOD
 * does it (NotificationContainerViewBinder lambda 16). See keyguard-notification-fold.
 */
internal object NumState {

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private val artworkScrollStates = java.util.WeakHashMap<Any, ArtworkScrollState>()
    private val presentation = NotificationFoldState()
    private val exposure = NotificationListExposure()
    private val openingProgress = NotificationOpeningProgress()
    private var openingVisualComplete = false
    private var exposureQueued = false
    private var exposureUntil = 0L

    fun shouldClickToList(): Boolean? = runCatching {
        val owner = model() ?: return null
        // This build inlines the property getter: the Kotlin Lazy holds the flow directly.
        val flow = Xp.callMethod(Xp.getObjectField(owner, "shouldNotifClickToList\$delegate"), "getValue")
        Xp.callMethod(flow, "getValue") as? Boolean
    }.getOrNull()

    fun notificationListVisible(): Boolean = exposure.listPresented(
        state(), shouldClickToList(), MiniPlayerRuntime.notificationRowsVisible())

    /** Pending verification owns a gesture, but its visual follows the actual native scroll. */
    fun notificationIslandAlpha(): Float = if (exposure.opening)
        openingProgress.alpha(collapseVisualProgress(), position()) else if (openingVisualComplete) 0f else 1f

    fun notificationOpeningFinished(): Boolean = openingVisualComplete

    fun notificationOpening(): Boolean = exposure.opening

    /** A hidden native pile is still the next category to close until its island owns it. */
    fun notificationNeedsCollapse(): Boolean = folding && MiniPlayerRuntime.detachedRail() &&
        LockIslands.foldsToCount() && (exposure.opening || !representedByIsland())

    fun observeNativeTouch(event: android.view.MotionEvent) {
        if (!folding || !MiniPlayerRuntime.detachedRail() || !Main.screenOnCached() ||
            !Main.onKeyguardNow() || Main.bouncerShown() || MiniPlayerScene.aodActive ||
            MiniPlayerScene.keyguardGoingAway) return
        if (event.actionMasked == android.view.MotionEvent.ACTION_MOVE) exposure.interacted()
        if (event.actionMasked == android.view.MotionEvent.ACTION_UP ||
            event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) checkExposureSoon()
    }

    fun requestList(why: String): Boolean {
        // Repeated empty-space swipes while opening are consumed without restarting the scroll.
        if (exposure.opening) return true
        val result = goTo("LIST", why = why)
        if (!result.startsWith("scrollY")) {
            exposure.failedOpen()
            LockIslands.refreshPresentation()
            return false
        }
        return true
    }

    /** Event-driven, bounded settling: no idle frame loop or delayed work after leaving keyguard. */
    fun checkExposureSoon() {
        if (!folding || !MiniPlayerRuntime.detachedRail()) return
        exposureUntil = SystemClock.uptimeMillis() + 2800L
        queueExposure()
    }

    private fun queueExposure() {
        if (exposureQueued) return
        exposureQueued = true
        main.postDelayed(exposureCheck, 64L)
    }

    private val exposureCheck = object : Runnable {
        override fun run() {
            exposureQueued = false
            val active = folding && MiniPlayerRuntime.detachedRail() && Main.screenOnCached() &&
                Main.onKeyguardNow() && !Main.bouncerShown() && !MiniPlayerScene.aodActive &&
                !MiniPlayerScene.keyguardGoingAway && LockIslands.foldsToCount()
            val now = SystemClock.uptimeMillis()
            if (!active) openingVisualComplete = false
            val nativeState = state()
            val before = exposure.holdsIsland
            when (exposure.observe(now, active, busy(), nativeState, shouldClickToList(),
                    active && MiniPlayerRuntime.notificationRowsVisible(), presentation.collapsed)) {
                NotificationListExposure.Action.OPENED -> {
                    openingVisualComplete = true
                    choosePresentation(false)
                    trace("rail list confirmed visible")
                }
                NotificationListExposure.Action.RETARGET_LIST -> goTo("LIST", why = "rail list layout settled")
                NotificationListExposure.Action.RETRY_LIST -> goTo("LIST", why = "rail list remeasure")
                NotificationListExposure.Action.CAPTURE -> {
                    openingVisualComplete = false
                    MiniPlayerRuntime.beginNativeNotificationReturn()
                    choosePresentation(true)
                    goTo("NUMBER", why = "rail captured hidden native pile")
                }
                NotificationListExposure.Action.RETAIN -> {
                    openingVisualComplete = false
                    exposureUntil = now
                    trace("rail visible list did not settle; kept retryable island")
                }
                NotificationListExposure.Action.NONE -> {}
            }
            if (before != exposure.holdsIsland) LockIslands.refreshPresentation()
            if (active && (exposure.opening || now < exposureUntil)) queueExposure()
        }
    }


    /** Seed before entry scrolls, even when ordinary notifications have not arrived yet. */
    fun seedPresentation(collapsed: Boolean, innerLandscape: Boolean) {
        val changed = presentation.seed(innerLandscape, collapsed)
        if (changed) LockIslands.refreshPresentation()
        if (!presentation.collapsed) main.removeCallbacks(foldCheck)
        if (folding) forceDefault(true)
    }

    fun beginLockSession() {
        presentation.beginSession()
        exposure.reset()
        openingVisualComplete = false
    }

    /** The saved lock-screen choice, independent of the rows' transient sleep/wake visibility. */
    fun presentationCollapsed(): Boolean = presentation.collapsed

    fun presentationForProbe(): String = "wanted=${presentation.wakeState()} collapsed=${presentation.collapsed} " +
        "opening=${exposure.opening} captured=${exposure.captured} completed=${!exposure.holdsIsland && notificationListVisible()} " +
        "native=${state()} explicitTarget=${scrollTo("LIST")} position=${position()} maxScroll=${maxScroll()} " +
        "rows=[${MiniPlayerRuntime.notificationExposureForProbe()}]"

    fun representedByIsland(): Boolean =
        (MiniPlayerRuntime.detachedRail() && exposure.holdsIsland) || presentation.representedByIsland(
            state()?.let { it == "NUMBER" } ?: (inNumber == true), MiniPlayerRuntime.detachedRail())

    /** Native AOD geometry callbacks must not call this: only explicit presentation actions do. */
    fun choosePresentation(collapsed: Boolean) {
        val changed = presentation.choose(collapsed)
        if (!collapsed) main.removeCallbacks(foldCheck)
        if (changed || folding && !forcedDefault) forceDefault(folding)
        if (changed) LockIslands.refreshPresentation()
    }

    /** The islands fold the ordinary notifications this way (LockIslands.setActive turns it on). */
    @Volatile var folding = false
        private set

    /** Whether the hooks went in: without them nothing here is used, and the old way stays. */
    @Volatile var available = false
        private set

    /** The stack as it last said, from its own enter/exitNumState; null until it has. */
    @Volatile var inNumber: Boolean? = null
        private set

    private var modelRef: WeakReference<Any>? = null
    private var countRef: WeakReference<View>? = null
    private var railFooterHook = false
    private var nativeRailFooter = 0
    private var effectiveRailFooter = 0

    private var nativeRailFooterRevision = -1L
    private var footerFlow: WeakReference<Any>? = null
    private var footerRevision = -1L
    private var footerOriginal = 0
    private var footerWritten = 0
    private var footerPublishError = false

    /** Re-publish once after display handoff or late rail activation; native consumers keep a cache. */
    fun refreshRailFooter(bottom: Int, revision: Long) {
        if (bottom <= 0) return
        if (footerRevision == revision && footerWritten == bottom && footerFlow?.get() != null) return
        // Resolve the current display's container again instead of keeping an attached old model.
        if (footerRevision != revision) modelRef = null
        runCatching {
            val owner = model() ?: return
            val repository = Xp.callMethod(Xp.getObjectField(owner, "keyguardRepository"), "get")
            val readonly = Xp.callMethod(repository, "getNotificationBottomOnKeyguard")
            val flow = if (readonly.javaClass.name == "com.miui.coroutines.syncflow.SynchronousStateFlowImpl") readonly
                else Xp.getObjectField(readonly, "flow")
            val current = (Xp.callMethod(flow, "getValue") as Number).toInt()
            val changed = footerFlow?.get() !== flow || footerRevision != revision
            val target = RailNotificationFloor.handoff(current, bottom, changed)
            if (changed || current != footerWritten) {
                footerOriginal = if (nativeRailFooterRevision == revision && nativeRailFooter > 0)
                    nativeRailFooter else current
            }
            footerFlow = WeakReference(flow)
            footerRevision = revision
            footerWritten = target
            effectiveRailFooter = target
            if (current != target) {
                // This is the exact mutable source inside ReadonlySynchronousStateFlow in this build.
                Xp.callMethod(flow, "setValue", target)
                trace("rail footer published $current -> $target revision=$revision")
            }
        }.onFailure {
            if (!footerPublishError) Xp.w("MCNum: rail footer refresh unavailable: $it")
            footerPublishError = true
        }
    }

    fun restoreRailFooter() {
        val flow = footerFlow?.get()
        footerFlow = null
        if (flow != null && footerRevision == Main.screenGeometry().revision &&
            footerOriginal > 0 && footerOriginal <= Main.screenHeight()) runCatching {
            if ((Xp.callMethod(flow, "getValue") as Number).toInt() == footerWritten && footerOriginal != footerWritten)
                Xp.callMethod(flow, "setValue", footerOriginal)
        }
        footerRevision = -1L
    }

    fun railFooterState(): String = "hook=$railFooterHook native=$nativeRailFooter effective=$effectiveRailFooter revision=$footerRevision publishError=$footerPublishError"

    /** For `op mini` / `op numstate`: the last few things done here. */
    private val log = ArrayDeque<String>()

    fun trace(what: String) {
        synchronized(log) {
            log.addLast("${SystemClock.uptimeMillis() % 100000} $what")
            while (log.size > 16) log.removeFirst()
        }
    }

    fun describe(): String = "numstate folding=$folding available=$available in=$inNumber wake=${presentation.wakeState()} " +
        "opening=${exposure.opening} captured=${exposure.captured} clickToList=${shouldClickToList()} " +
        LockIslands.systemHiddenForProbe().let { if (it.isEmpty()) "" else "sysHidden=[$it] " } +
        "default=${runCatching { defaultFlow()?.let { Xp.callMethod(it, "getValue") } }.getOrNull()} " +
        "log=" + synchronized(log) { log.joinToString(" ; ") } +
        " || ${LockscreenNotificationRuntime.describe()}" +
        " || artwork=[${LockIslands.notificationArtworkSampling()}]" +
        " || frames=" + framesText()

    fun install(classLoader: ClassLoader) {
        // In the inspected stock SystemUI build, the repository subtracts the number-state footer's
        // height and padding before publishing this value to every native layout consumer.
        // Change that one source, rather than translating visible rows after layout.
        runCatching {
            val transform = Xp.findClass("com.miui.systemui.keyguard.data.repository." +
                "MiuiKeyguardRepositoryImpl\$notificationBottomOnKeyguard\$2\$1", classLoader)
            Xp.hookAll(transform, "invokeSuspend") { chain ->
                val value = chain.proceed()
                if (value !is Int) value else {
                    nativeRailFooter = value
                    nativeRailFooterRevision = Main.screenGeometry().revision
                    if (footerRevision == nativeRailFooterRevision && value > 0) footerOriginal = value
                    val next = LockScreenRailRuntime.notificationBottom(value)
                    if (next != effectiveRailFooter) trace("rail footer $value -> $next")
                    effectiveRailFooter = next
                    next
                }
            }
            railFooterHook = true
        }.onFailure { Xp.log("MCNum: rail footer source unavailable; native layout retained: $it") }
        val vmClass = runCatching {
            Xp.findClass("com.miui.systemui.notification.view.viewmodel.NotificationContainerViewModel", classLoader)
        }.getOrElse {
            Xp.log("MCNum: no container model, ordinary notifications stay islands the old way: $it")
            return
        }
        runCatching {
            for ((name, value) in listOf("enterNumState" to true, "exitNumState" to false)) {
                Xp.hookAll(vmClass, name) { chain ->
                    val result = chain.proceed()
                    modelRef = WeakReference(chain.thisObject)
                    if (inNumber != value) {
                        inNumber = value
                        trace(if (value) "stack folded" else "stack opened")
                    }
                    // A repeated NUMBER callback can complete an explicit return after the
                    // previous AOD session already cached true. Reconcile publication anyway.
                    listeners.forEach { runCatching { it(value) } }
                    checkExposureSoon()
                    result
                }
            }
            available = true
        }.onFailure { Xp.log("MCNum: num state unhookable: $it") }
        // Let go in between, the stack rests as a pile (STACK) with the rows out on the lock
        // screen and no island: folding, a hands-up that lands there lands folded instead, so
        // the stack is either the island or the list, as ColorOS's is either capsule or list.
        runCatching {
            Xp.hookAll(vmClass, "switchStackStatus") { chain ->
                val target = chain.proceed()
                runCatching {
                    val resolved = handsUp(chain.thisObject, chain.args.firstOrNull(), target as Float) ?: target
                    rememberHandsUp(chain.thisObject, chain.args.firstOrNull(), resolved)
                    resolved
                }.getOrNull() ?: target
            }
        }.onFailure { Xp.log("MCNum: hands-up unhookable: $it") }
        // Wake on actual native scrolling, including STACK <-> LIST without number-state callbacks.
        runCatching {
            val stack = Xp.findClass("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", classLoader)
            for (name in listOf("setOwnScrollY", "setOverScrolledPixels")) {
                runCatching {
                    Xp.hookAll(stack, name) { chain ->
                        val value = (chain.args.firstOrNull() as? Number)?.toFloat()
                        val target = chain.thisObject
                        val channel = if (name == "setOwnScrollY") 0
                            else if (chain.args.getOrNull(1) == true) 1 else 2
                        val changed = value != null && target != null &&
                            artworkScrollStates.getOrPut(target) { ArtworkScrollState() }.changed(channel, value)
                        val tracking = changed && LockIslands.tracksNotificationArtwork()
                        val before = if (tracking) position() else null
                        val result = chain.proceed()
                        if (tracking && position() != before) LockIslands.notificationArtworkMotion(before)
                        if (changed) checkExposureSoon()
                        result
                    }
                }.onFailure { Xp.log("MCNum: $name artwork hook unavailable: $it") }
            }

        }.onFailure { Xp.log("MCNum: artwork motion hooks unavailable: $it") }
    }

    fun addListener(l: (Boolean) -> Unit) { listeners.addIfAbsent(l) }

    /** The islands take over the ordinary notifications' folding, or give it back. */
    fun setFolding(on: Boolean) {
        if (!available || folding == on) return
        folding = on
        trace("folding=$on")
        forceDefault(on)
        if (!on) hideCount(false)
    }

    private fun handsUp(model: Any, info: Any?, target: Float): Float? {
        if (!folding || info == null || !LockIslands.foldsToCount()) return null
        if (LockIslands.keepsPileOnFold()) return null
        val event = (Xp.getObjectField(info, "stackShowEvent") as? Enum<*>)?.name
        if (event != "HANDS_UP" || Xp.getObjectField(info, "isNumberState") == true) return null
        if ((Xp.getObjectField(info, "normalNotifCount") as Number).toInt() <= 0) return null
        val factory = Xp.getObjectField(Xp.getObjectField(model, "\$\$delegate_0"), "strategyFactory")
        fun strategy(name: String) = Xp.callMethod(Xp.getObjectField(factory, name), "get")
        fun number() = (Xp.callMethod(strategy("numberStrategy"), "calculateTargetYPosition", info) as Number).toFloat()
        // The row spread out as the list and pulled toward home: folded. The stack's own hands-up
        // reads the list's top without the pull on it (nsslTopYPositionWithoutOverScroll), and a
        // pull down on the list moves only the pull - so it put the list back every time, and with
        // the media card and a focus card in it the list and the pile rest at the same place
        // (the clock's smallest): the row stood half folded (filmed 2026-09-30).
        if (LockIslands.spreadingNow() && MiniPlayerRuntime.spreadFolding()) {
            val number = number()
            trace("hands up with the row going home: folded ($target -> $number)")
            return number
        }
        val stack = (Xp.callMethod(strategy("stackStrategy"), "calculateTargetYPosition", info) as Number).toFloat()
        if (kotlin.math.abs(stack - target) >= 1f && !pileAt(model, info, target)) return null
        // Swiped up, it is the list being opened: with one notification the list rests where the
        // pile does, and a swipe up from the media card folded straight back (2026-10-24).
        if (MiniPlayerRuntime.detachedRail() && shouldClickToList() == false &&
            MiniPlayerRuntime.notificationRowsVisible()) return null
        if (MiniPlayerRuntime.stackSwipedUp && !MiniPlayerRuntime.detachedRail()) {
            trace("hands up at the pile after a swipe up: left open ($target)")
            return null
        }
        val number = number()
        trace("hands up at the pile: folded instead ($target -> $number)")
        return number
    }

    /** A real finger release chooses the next wake pose; a screen-on/off strategy does not. */
    private fun rememberHandsUp(model: Any, info: Any?, target: Float) {
        if (!folding || info == null || !target.isFinite() || MiniPlayerScene.aodActive ||
            !Main.screenOnCached() || !Main.onKeyguardNow()) return
        val event = (Xp.getObjectField(info, "stackShowEvent") as? Enum<*>)?.name
        if (event != "HANDS_UP") return
        if ((Xp.getObjectField(info, "normalNotifCount") as Number).toInt() <= 0) return
        val factory = Xp.getObjectField(Xp.getObjectField(model, "\$\$delegate_0"), "strategyFactory")
        val number = Xp.callMethod(Xp.getObjectField(factory, "numberStrategy"), "get")
        val foldedTop = (Xp.callMethod(number, "calculateTargetYPosition", info) as Number).toFloat()
        exposure.interacted()
        val folded = kotlin.math.abs(target - foldedTop) < 1f
        if (folded && MiniPlayerRuntime.detachedRail()) {
            openingVisualComplete = false
            MiniPlayerRuntime.beginNativeNotificationReturn()
        }
        choosePresentation(folded)
        checkExposureSoon()
    }

    /**
     * Whether the stack resting with its top at [top] reads as its pile (STACK): its own sum
     * (NotificationContainerViewModel's currentsStackState), the room under the top against the
     * number threshold and the pile's height. The strategy's pile top can be clamped to the
     * clock's smallest, where it is not where a hands-up leaves it.
     */
    private fun pileAt(model: Any, info: Any, top: Float): Boolean = runCatching {
        val sample = value(model, "getOnUpdateChildSampleStackInfo") ?: return false
        val bottom = (Xp.getObjectField(info, "notificationBottomOnKeyguard") as Number).toFloat()
        val scrim = (Xp.getObjectField(info, "scrimTopPadding") as Number).toFloat()
        val focus = (Xp.getObjectField(info, "focusNotifsHeight") as Number).toFloat()
        val room = bottom - top - scrim - focus
        val threshold = (Xp.callMethod(model, "getNumStateHeightThreshold", sample) as Number).toFloat()
        val pile = kotlin.math.ceil((Xp.callMethod(model, "stackingStateHeight\$default", model) as Number).toFloat())
        room >= threshold && room <= pile
    }.getOrDefault(false)

    // ---------------------------------------------------------------- the model and its state

    /** The stack's container model (NotificationContainerViewModel). */
    fun model(): Any? {
        modelRef?.get()?.let { return it }
        val stack = MiniPlayerRuntime.stackForProbe() ?: return null
        return runCatching {
            val controller = Xp.getObjectField(stack, "mController")
            val injector = Xp.getObjectField(controller, "mNsslControllerInjector")
            val helper = Xp.getObjectField(injector, "numStateTouchHelper")
            Xp.callMethod(Xp.getObjectField(helper, "notifContainerViewModel"), "get")
        }.getOrNull()?.also { modelRef = WeakReference(it) }
    }

    private fun value(owner: Any, getter: String): Any? =
        Xp.callMethod(Xp.callMethod(owner, getter), "getValue")

    /** NUMBER, STACK or LIST, as the stack reads itself now; null unread. */
    fun state(): String? = runCatching {
        (value(model() ?: return null, "getCurrentsStackState") as Enum<*>).name
    }.getOrNull()

    /** The stack is moving between states on its own (a scroll or the fold's fade). */
    fun busy(): Boolean = runCatching {
        val model = model() ?: return false
        value(model, "isNumStateAnimating") == true ||
            Xp.getObjectField(Xp.getObjectField(model, "isBeingDraggedOrAnimating"), "flow")
                .let { Xp.callMethod(it, "getValue") } == true
    }.getOrDefault(false)

    /** Actual native row fade, not elapsed time: 0 expanded, 1 hidden in NUMBER. */
    fun collapseVisualProgress(): Float? = runCatching {
        val animator = Xp.getObjectField(model() ?: return null, "\$\$delegate_1")
        val flow = Xp.getObjectField(animator, "_numStateAlpha")
        val alpha = (Xp.callMethod(flow, "getValue") as Number).toFloat()
        if (alpha.isFinite()) (1f - alpha).coerceIn(0f, 1f) else null
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun stateConst(model: Any, name: String): Any {
        val now = value(model, "getCurrentsStackState") as Enum<*>
        return java.lang.Enum.valueOf(now.javaClass as Class<out Enum<*>>, name)
    }

    /** The native notification footer, in the same coordinates as the stack's target positions. */
    fun bottomOnKeyguard(): Float? = runCatching {
        val model = model() ?: return null
        val info = value(model, "getOnUpdateChildSampleStackInfo") ?: return null
        val event = Xp.callMethod(model, "notifStackStateToEvent", stateConst(model, "LIST"))
        val status = Xp.callMethod(model, "getKeyguardStackStatusInfo", info, event)
        (Xp.getObjectField(status, "notificationBottomOnKeyguard") as Number).toFloat()
            .takeIf { it.isFinite() && it > 0f }
    }.getOrNull()

    /**
     * A rail LIST is an explicit user expansion. OEM SHOW_LIST is only a default pose: with
     * mixed focus/music rows its listStrategy intentionally hides ordinary notifications and
     * can return the same target as STACK or NUMBER. The inspected clickToListStrategy reserves
     * normal-list space even in that case, without emitting another click or haptic event.
     */
    fun scrollFor(model: Any, state: String): Int {
        val info = value(model, "getOnUpdateChildSampleStackInfo")!!
        val event = Xp.callMethod(model, "notifStackStateToEvent", stateConst(model, state))
        val status = Xp.callMethod(model, "getKeyguardStackStatusInfo", info, event)
        val top = if (state == "LIST" && MiniPlayerRuntime.detachedRail()) {
            val factory = Xp.getObjectField(Xp.getObjectField(model, "\$\$delegate_0"), "strategyFactory")
            val strategy = Xp.callMethod(Xp.getObjectField(factory, "clickToListStrategy"), "get")
            (Xp.callMethod(strategy, "calculateTargetYPosition", status) as Number).toFloat()
        } else (Xp.callMethod(model, "switchStackStatus", status) as Number).toFloat()
        val focus = (Xp.getObjectField(info, "focusNotifsHeight") as Number).toFloat()
        val bound = (Xp.callMethod(model, "positionWithoutMinScrollRange", focus) as Number).toFloat()
        return (bound - top).toInt()
    }

    /** The stack's own scroll now; null unread. */
    fun scrollY(): Int? = runCatching {
        val model = model() ?: return null
        (Xp.callMethod(Xp.callMethod(Xp.getObjectField(model, "notifContainerRefactor"), "get"),
            "getOwnScrollY") as Number).toInt()
    }.getOrNull()

    /**
     * The focus rows the stack has measured (its sample's focusNotifCount); null unread. The
     * list's place is worked out from this sample: asked before it counts rows just come in, the
     * place left them out and the stack read the result as its pile (STACK).
     */
    fun focusCount(): Int? = runCatching {
        val info = value(model() ?: return null, "getOnUpdateChildSampleStackInfo") ?: return null
        (Xp.getObjectField(info, "focusNotifCount") as Number).toInt()
    }.getOrNull()

    fun normalCount(): Int? = runCatching {
        val info = value(model() ?: return null, "getOnUpdateChildSampleStackInfo") ?: return null
        (Xp.getObjectField(info, "normalNotifCount") as Number).toInt()
    }.getOrNull()

    /**
     * The stack's pull past its scroll now (the model's overScrollAmount), 0 unread. A finger
     * pulling the list down toward the fold moves this, not the scroll: the scroll only follows
     * on the let-go, as the hands-up settles it (calculateNotifTop = base - scrollY + this).
     */
    fun overScroll(): Float {
        val fromModel = runCatching {
            val model = model() ?: return@runCatching 0f
            (Xp.callMethod(Xp.getObjectField(Xp.getObjectField(model, "overScrollAmount"), "flow"),
                "getValue") as Number).toFloat()
        }.getOrDefault(0f)
        // The stack's own reading of its pull at the top, should the model's not carry it.
        val fromStack = runCatching {
            (Xp.callMethod(MiniPlayerRuntime.stackForProbe() ?: return@runCatching 0f,
                "getCurrentOverScrollAmount", true) as Number).toFloat()
        }.getOrDefault(0f)
        return if (kotlin.math.abs(fromStack) > kotlin.math.abs(fromModel)) fromStack else fromModel
    }

    /** Both pulls apart, for the frame log. */
    fun overScrollParts(): String = runCatching {
        val m = model()?.let {
            (Xp.callMethod(Xp.getObjectField(Xp.getObjectField(it, "overScrollAmount"), "flow"), "getValue") as Number).toInt()
        }
        val s = MiniPlayerRuntime.stackForProbe()?.let {
            (Xp.callMethod(it, "getCurrentOverScrollAmount", true) as Number).toInt()
        }
        "$m/$s"
    }.getOrDefault("?")

    /**
     * The furthest the stack scrolls (NotificationStackScrollLayout.getScrollRange). The list's
     * place as the click-to-list strategy works it out can be past it - 658 against a stack that
     * went no further than 375 with the media card and a focus card in it (2026-09-30) - and the
     * spread, aimed there, stood at 0.57 for seconds, sent there again and again.
     */
    fun maxScroll(): Int? = runCatching {
        (Xp.callMethod(MiniPlayerRuntime.stackForProbe() ?: return null, "getScrollRange") as Number).toInt()
    }.getOrNull()

    /** Where the list rests: the strategy's place, as far as the stack can scroll. */
    fun listScroll(): Int? {
        val list = scrollTo("LIST") ?: return null
        val max = maxScroll() ?: return list
        val number = scrollTo("NUMBER") ?: return list
        return if (list > number) minOf(list, maxOf(max, number)) else list
    }

    // ---------------------------------------------------------------- the spread's frame log

    /** Every frame of the spread moving, for `op numstate`: the last FRAMES_KEPT of them. */
    private val frames = ArrayDeque<String>()
    private const val FRAMES_KEPT = 90

    fun frame(what: String) {
        synchronized(frames) {
            frames.addLast("${SystemClock.uptimeMillis() % 100000} $what")
            while (frames.size > FRAMES_KEPT) frames.removeFirst()
        }
    }

    fun framesText(): String = synchronized(frames) { frames.joinToString(" ; ") }

    /**
     * Where the stack's top is, as a scroll: the scroll less the pull on it, so a finger's pull
     * counts as it is made. On the same scale as [scrollFor]; null unread.
     */
    fun position(): Int? = scrollY()?.let { (it - overScroll()).toInt() }

    /** [scrollFor] for [state] as the stack is now; null unread. */
    fun scrollTo(state: String): Int? = runCatching { scrollFor(model() ?: return null, state) }.getOrNull()

    /** The stack scrolled to [state]: animated on the screen-on scroll's spring, or set. */
    fun goTo(state: String, anim: Boolean = true, why: String): String = runCatching {
        val model = model() ?: return "no model"
        val stack = MiniPlayerRuntime.stackForProbe() ?: return "no stack"
        if (state == "NUMBER") { exposure.cancelOpen(); openingVisualComplete = false }
        val railList = state == "LIST" && MiniPlayerRuntime.detachedRail()
        val firstRequest = railList && !exposure.opening
        if (firstRequest) {
            exposure.request(SystemClock.uptimeMillis())
            openingVisualComplete = false
        }
        val y = scrollFor(model, state)
        val refactor = Xp.callMethod(Xp.getObjectField(model, "notifContainerRefactor"), "get")
        val scroller = Xp.callMethod(refactor, "getOverScroller")
        val cur = (Xp.callMethod(refactor, "getOwnScrollY") as Number).toInt()
        if (firstRequest) openingProgress.begin(collapseVisualProgress(), position() ?: cur, y)
        else if (railList) openingProgress.retarget(y)
        val injector = Xp.getObjectField(Xp.getObjectField(stack, "mController"), "mNsslControllerInjector")
        val done = if (!anim || cur == y) {
            Xp.callMethod(scroller, "abortAnimation")
            Xp.callMethod(stack, "setOwnScrollY", y)
            "scrollY $cur -> $y (set)"
        } else {
            val animator = Xp.callMethod(Xp.getObjectField(model, "notificationScreenOnOffAnimator"), "get")
            val ease = animator.javaClass.getField("screenOnScrollYEaseStyle").get(null)
            Xp.callMethod(scroller, "startFolmeScroll", 0, cur, y - cur, 0, ease)
            Xp.callMethod(stack, "animateScroll")
            Xp.callMethod(injector, "onStartScroll", y.toFloat(), topChangeType(injector, "NOTIFS_CHANGED"))
            "scrollY $cur -> $y (folme)"
        }
        trace("$why: to $state, $done")
        if (state == "NUMBER" || state == "LIST") choosePresentation(state == "NUMBER")
        if (railList) {
            // A changed wake intent does not mean rows have become visible. Keep the capsule
            // until exposure confirms the settled list, including initial-state restoration.
            LockIslands.refreshPresentation()
            checkExposureSoon()
        }
        done
    }.getOrElse {
        if (state == "LIST" && MiniPlayerRuntime.detachedRail()) {
            exposure.failedOpen()
            LockIslands.refreshPresentation()
        }
        trace("$why: to $state failed: $it")
        "failed: $it"
    }

    /**
     * The stack put at scroll [y] this frame, no animation (a finger has it): as goTo's unanimated
     * way, a running scroll of its own stopped first. False when it could not be.
     */
    fun setScroll(y: Int): Boolean = runCatching {
        val model = model() ?: return false
        val stack = MiniPlayerRuntime.stackForProbe() ?: return false
        val refactor = Xp.callMethod(Xp.getObjectField(model, "notifContainerRefactor"), "get")
        if ((Xp.callMethod(refactor, "getOwnScrollY") as Number).toInt() == y) return true
        Xp.callMethod(Xp.callMethod(refactor, "getOverScroller"), "abortAnimation")
        Xp.callMethod(stack, "setOwnScrollY", y)
        true
    }.getOrDefault(false)

    @Suppress("UNCHECKED_CAST")
    private fun topChangeType(injector: Any, name: String): Any {
        val m = injector.javaClass.methods.first { it.name == "onStartScroll" && it.parameterTypes.size == 2 }
        return java.lang.Enum.valueOf(m.parameterTypes[1] as Class<out Enum<*>>, name)
    }

    // ---------------------------------------------------------------- the screen-on default

    /**
     * The state the lock screen wakes to is the user's setting default_keyguard_notif_state
     * (MiuiKeyguardNotificationSettingsRepository.defaultState). Folding owns that flow in memory
     * only; the setting itself stays untouched. The user's current presentation
     * chooses NUMBER or LIST, so an expanded entry is not folded by the later wake animation.
     * Given back, it takes the setting's own value again. Entry refreshes also reconcile a
     * recreated flow or a native setting emission without reapplying the user's initial layout.
     */
    private var forcedDefault = false

    private fun defaultFlow(): Any? = runCatching {
        val repo = Xp.getObjectField(model() ?: return null, "settingsRepository")
        Xp.getObjectField(Xp.getObjectField(repo, "defaultState"), "\$\$delegate_0")
    }.getOrNull()

    private fun forceDefault(on: Boolean) {
        runCatching {
            val flow = defaultFlow() ?: return
            val model = model() ?: return
            if (on) {
                val name = presentation.wakeState()
                val next = stateConst(model, name)
                val changed = Xp.callMethod(flow, "getValue") != next
                if (changed) Xp.callMethod(flow, "setValue", next)
                if (changed || !forcedDefault) trace("default $name (in memory)")
                forcedDefault = true
            } else if (forcedDefault) {
                forcedDefault = false
                val setting = android.provider.Settings.System.getInt(
                    (Main.sAppCtx ?: return).contentResolver, "default_keyguard_notif_state", 2)
                val name = when (setting) { 3 -> "NUMBER"; 1 -> "LIST"; else -> "STACK" }
                Xp.callMethod(flow, "setValue", stateConst(model, name))
                trace("default back to the setting's $name")
            }
        }.onFailure { trace("default not set: $it") }
    }

    /**
     * Folding and the lock screen up with the stack as a pile: folded. Asked when the islands
     * show (after SystemUI starts, the first lock screen has not been through a wake), and
     * never while the stack is being moved or is the list the user opened.
     */
    fun foldIfPiled(why: String) {
        checkExposureSoon()
        if (!presentation.autoFold(folding)) return
        main.removeCallbacks(foldCheck)
        foldWhy = why
        main.postDelayed(foldCheck, FOLD_SETTLE_MS)
    }

    private var foldWhy = ""
    private const val FOLD_SETTLE_MS = 350L

    private val foldCheck = Runnable {
        if (!presentation.autoFold(folding) || !LockIslands.foldsToCount()) return@Runnable
        // The list the spread opened, passing through the pile on its way: not folded back.
        if (LockIslands.keepsPileOnFold()) return@Runnable
        if (LockIslands.spreadingNow()) { trace("pile check skipped: spread ($foldWhy)"); return@Runnable }
        if (busy()) { foldIfPiled(foldWhy); return@Runnable }
        if (state() == "STACK") goTo("NUMBER", anim = true, why = "piled ($foldWhy)")
    }

    // ---------------------------------------------------------------- the count line

    /**
     * The stack's own "N个通知" line (NotificationNumStateView) sits inside the island's pill:
     * hidden while the island stands for it. On its children's transitionAlpha (the zen mark, the
     * divider, the count), which nothing of SystemUI's writes: the line's own transitionAlpha is
     * the keyguard's fade (scaleAndAlpha, NotificationNumStateViewBinder), and showing the line
     * by setting that to 1 put "勿扰" over the bouncer, where the keyguard had faded it out (#42).
     */
    fun hideCount(hide: Boolean) {
        val v = countView() as? ViewGroup ?: return
        val want = if (hide) 0f else 1f
        var changed = false
        for (k in 0 until v.childCount) {
            val c = v.getChildAt(k)
            if (c.transitionAlpha != want) {
                c.transitionAlpha = want
                changed = true
            }
        }
        if (changed) trace(if (hide) "count hidden" else "count shown")
    }

    private fun countView(): View? {
        countRef?.get()?.takeIf { it.isAttachedToWindow }?.let { return it }
        val root = MiniPlayerRuntime.stackForProbe()?.rootView ?: return null
        val queue = ArrayDeque<View>().apply { add(root) }
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (v.javaClass.name.endsWith("NotificationNumStateView")) {
                countRef = WeakReference(v)
                return v
            }
            if (v is ViewGroup) for (k in 0 until v.childCount) queue.add(v.getChildAt(k))
        }
        return null
    }
}
