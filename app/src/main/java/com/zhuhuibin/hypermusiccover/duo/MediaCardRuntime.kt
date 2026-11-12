package com.zhuhuibin.hypermusiccover.duo

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.ColorFilter
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.TransitionDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.Executors

/** Appearance only: no notification folding, clock geometry or lyric hooks are replaced. */
object MediaCardRuntime {
    private const val NOTIFICATION = "com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaViewControllerImpl"
    private const val ISLAND = "com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaViewBinderImpl"
    private val main = Handler(Looper.getMainLooper())
    private val worker by lazy { Executors.newSingleThreadExecutor { r ->
        Thread(r, "MCMediaStyle").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    } }
    @Volatile private var values = MediaCardConfig.parse(null)
    @Volatile private var revision = 0L
    private val states = WeakHashMap<Any, MutableList<CardState>>()
    private val detachedOwners = java.util.Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
    private val layouts = java.util.Collections.newSetFromMap(WeakHashMap<Any, Boolean>())
    private var applying = false
    private var hasOverrides = false
    private var sleeping = false
    private var renderer: MediaCardBackgroundRenderer? = null
    private var flowApi: NativeMusicBgApi? = null
    private var refreshedMaterialTheme = 0
    private val materialInFlight = ThreadLocal.withInitial {
        java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Any, Boolean>())
    }
    private val oval = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) { outline.setOval(0, 0, view.width, view.height) }
    }

    @JvmStatic fun circularNotificationArtwork(): Boolean = setting("notification", "cover") in 1..2

    @JvmStatic fun configJson(): String = MediaCardConfig.encode(values)
    @JvmStatic fun configure(json: String?) {
        values = MediaCardConfig.parse(json)
        revision++
        hasOverrides = overrides()
        refresh()
    }

    @JvmStatic fun set(key: String?, value: Int) {
        if (key == null || key !in values) return
        values = values + (key to MediaCardConfig.limitKey(key, value))
        revision++
        hasOverrides = overrides()
        refresh()
    }

    private fun overrides(): Boolean = values.any { (key, value) -> value != MediaCardConfig.defaults[key.substringAfter('.')] }

    private fun setting(scope: String, name: String) = values.getValue("$scope.$name")

    /** The forced theme as the notification's numbers have it: 0 native, 1 light, 2 dark. */
    private fun forcedTheme(scope: String): Int = setting(scope, "theme")

    private fun on(scope: String, name: String) = setting(scope, name) != 0

    @JvmStatic fun install(loader: ClassLoader) {
        for ((name, scope) in listOf(NOTIFICATION to "notification", ISLAND to "island")) {
            val clazz = runCatching { Xp.findClass(name, loader) }.getOrNull() ?: continue
            for (method in listOf("attach", "bindMediaData", "setAlbumImage", "setMusicBgShader", "updateForegroundColors", "setSeamless")) {
                if (clazz.declaredMethods.none { it.name == method }) continue
                Xp.hookAll(clazz, method) { chain ->
                    val result = chain.proceed()
                    if (!applying) {
                        val owner = chain.thisObject
                        if (owner != null) safely("$scope.$method") {
                            if (method == "attach") detachedOwners.remove(owner)
                            bind(owner, scope, if (method == "bindMediaData") chain.args.firstOrNull() else null)
                        }
                    }
                    result
                }
            }
            Xp.hookAll(clazz, "detach") { chain ->
                chain.thisObject?.let { owner ->
                    detachedOwners.add(owner)
                    states.remove(owner)?.forEach { it.restore() }
                }
                chain.proceed()
            }
        }
        val layout = runCatching { Xp.findClass("com.android.systemui.statusbar.notification.mediacontrol.MiuiMediaNotificationControllerImpl", loader) }.getOrNull()
        if (layout != null) for (name in listOf("loadLayout", "loadLayout\$1", "updateLayout\$1", "updateLayout\$6")) {
            if (layout.declaredMethods.none { it.name == name && it.parameterCount == 0 }) continue
            Xp.hookAll(layout, name) { chain ->
                if (!applying) chain.thisObject?.let { owner ->
                    layouts.add(owner)
                    safely("layout") { configureLayout(owner) }
                }
                val result = chain.proceed()
                if (!applying) chain.thisObject?.let { owner -> safely("layout") { configureLayout(owner) } }
                result
            }
        }
        // Theme the native material by its resource-resolution context, including Soft Glass.
        for (name in listOf("MediaViewNormalEffect", "MediaViewBlurEffect", "MediaViewBlurOnKeyguardEffect",
            "MediaViewGlassEffect", "MediaViewGlassOnKeyguardEffect", "MediaViewGlassOnKeyguardLightWallPaperEffect", "MediaViewGlassFullAodEffect")) {
            val clazz = runCatching { Xp.findClass("com.android.systemui.statusbar.notification.style.vieweffect.$name", loader) }.getOrNull() ?: continue
            for (method in clazz.declaredMethods.filter {
                it.name == "apply" && it.parameterTypes.contentEquals(arrayOf(Any::class.java, Context::class.java))
            }) runCatching {
                method.isAccessible = true
                Xp.api().deoptimize(method)
                Xp.api().hook(method).intercept { chain ->
                    val target = chain.args.firstOrNull()
                    val context = chain.args.getOrNull(1) as? Context
                    val inFlight = requireNotNull(materialInFlight.get())
                    if (target == null || context == null || !inFlight.add(target)) chain.proceed()
                    else try {
                        val theme = setting("notification", "theme")
                        if (theme == 0) chain.proceed()
                        else chain.proceed(arrayOf(target, themed(context, theme)))
                    } finally { inFlight.remove(target) }
                }
            }.onFailure { Xp.w("Media material hook failed: $name: $it") }
        }

        // A screen transition may happen without any media-data callback.
        for ((clazz, method) in listOf("com.android.systemui.keyguard.WakefulnessLifecycle" to "dispatchStartedGoingToSleep",
            "com.android.systemui.keyguard.WakefulnessLifecycle" to "dispatchFinishedWakingUp")) {
            runCatching { Xp.hookAll(Xp.findClass(clazz, loader), method) { chain ->
                val result = chain.proceed()
                sleeping = method == "dispatchStartedGoingToSleep"
                main.post { refreshMotion() }
                result
            } }
        }
        Xp.log("Media card customization hooks installed")
    }

    private fun bind(owner: Any, scope: String, data: Any?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            // A queued callback can outlive this binding; read the owner's current media then.
            main.post { bind(owner, scope, null) }
            return
        }
        if (owner in detachedOwners) return
        val holders = listOfNotNull(field(owner, "holder"), if (scope == "island") field(owner, "dummyHolder") else null).distinct()
        val collection = states.getOrPut(owner) { mutableListOf() }
        val media = data ?: field(owner, "mediaData")
        for (holder in holders) {
            val album = field(holder, "albumImageView") as? ImageView ?: continue
            val state = collection.firstOrNull { it.album === album } ?: CardState(owner, holder, scope, album).also { collection.add(it) }
            state.media = media
            state.apply()
        }
    }

    private fun refresh() {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { refresh() }; return }
        applying = true
        try {
            // Reload native constraints before applying hidden-element overrides.
            layouts.toList().forEach { owner -> safely("reload layout") {
                val methods = owner.javaClass.declaredMethods
                methods.firstOrNull { it.name in listOf("loadLayout", "loadLayout\$1") && it.parameterCount == 0 }?.apply { isAccessible = true }?.invoke(owner)
                configureLayout(owner)
                methods.firstOrNull { it.name in listOf("updateLayout\$1", "updateLayout\$6") && it.parameterCount == 0 }?.apply { isAccessible = true }?.invoke(owner)
            } }
            val materialTheme = setting("notification", "theme")
            val themeChanged = refreshedMaterialTheme != materialTheme
            refreshedMaterialTheme = materialTheme
            states.values.toList().flatten().forEach { state -> safely("refresh") {
                val owned = state.settingsChanged()
                state.applyTheme()
                // Only a real ownership/theme boundary asks the current provider to recalculate.
                // Never replay an effect captured on another lock/AOD/theme scene.
                if (owned || themeChanged) state.rebindNativeMaterial()
                state.apply()
            } }
        } finally { applying = false }
    }

    @JvmStatic fun refreshMotion() {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { refreshMotion() }; return }
        states.values.toList().flatten().forEach { it.refreshPendingMaterial(); it.motion() }
    }

    /** The cover morph owns thumbnail alpha/scale; appearance is reapplied after its pass. */
    @JvmStatic fun afterCardPass(card: View?) {
        if (card == null || !hasOverrides) return
        states.values.toList().flatten().filter { it.scope == "notification" && it.album.rootView === card.rootView }
            .forEach { it.applyElements(); it.motion() }
    }

    /** Live native controls, including app-defined actions, without assigning new behaviors. */
    @JvmStatic fun nativeHeaderControls(card: View): List<View> {
        fun inside(view: View): Boolean {
            var node: View? = view
            while (node != null && node !== card) node = node.parent as? View
            return node === card
        }
        return states.values.toList().flatten().filter { it.scope == "notification" && inside(it.album) }
            .flatMap { state ->
                val actions = runCatching { Xp.callMethod(state.holder, "getActionList") as? List<*> }
                    .getOrNull().orEmpty()
                (listOf(field(state.holder, "seamless"), field(state.holder, "appIcon")) + actions +
                    (0..4).map { field(state.holder, "action$it") }).filterIsInstance<View>()
            }.filter(::inside).distinct()
    }

    @JvmStatic fun describe(): String = states.values.flatten().joinToString("\n") {
        "${it.scope} theme=${setting(it.scope, "theme")} nativeMaterial=${MediaCardConfig.followsNativeMaterial(values, it.scope)} clip=${it.overlay?.clipToOutline} attached=${it.album.isAttachedToWindow} shown=${it.album.isShown} background=${setting(it.scope, "background")} flow=${setting(it.scope, "flow")} cover=${setting(it.scope, "cover")} overlay=${it.overlay != null} rotation=${it.rotation?.isRunning == true} token=${it.requestToken.hashCode()} geometry=${it.geometryReport()}"
    }

    private fun configureLayout(owner: Any) {
        val layout = field(owner, "normalLayout") ?: return
        val context = field(owner, "context") as? Context ?: return
        fun id(name: String) = context.resources.getIdentifier(name, "id", context.packageName)
        fun hidden(target: Any, name: String, hide: Boolean) {
            if (hide && id(name) != 0) Xp.callMethod(target, "setVisibility", id(name), View.GONE)
        }
        if (setting("notification", "cover") == 3) {
            hidden(layout, "album_art", true)
            for ((name, anchor, margin) in listOf(Triple("header_title", 6, 26f), Triple("header_artist", 6, 26f),
                Triple("actions", 3, 67.5f), Triple("action0", 3, 78.5f))) {
                if (id(name) != 0) Xp.callMethod(layout, "setGoneMargin", id(name), anchor, (margin * context.resources.displayMetrics.density).toInt())
            }
        }
        field(owner, "normalAlbumLayout")?.let { hidden(it, "cover_source", on("notification", "hideSource")) }
        hidden(layout, "media_seamless", on("notification", "hideDevice"))
    }

    private class CardState(owner: Any, val holder: Any, val scope: String, val album: ImageView) {
        val owner = WeakReference(owner)
        val albumView = field(holder, "albumView") as? View ?: album
        val background = (field(holder, "mediaBg") ?: field(holder, "mediaBgView")) as? View
        var originalOutline = album.outlineProvider
        var originalClip = album.clipToOutline
        var originalParentOutline = albumView.outlineProvider
        var originalParentClip = albumView.clipToOutline
        private val backgroundAlpha = MediaPropertyLease<Float>()
        private val themeContext = MediaPropertyLease<Context> { a, b -> a === b }
        private var contextTheme = 0
        private var nativeRebinds = 0
        private var nativeRebindAvailable = true
        private val nativeRebindRequest = MediaRebindRequest()
        private var windowDetached = !album.isAttachedToWindow
        private fun materialSettings() = listOf("background", "flow", "theme", "pauseRestore", "animate", "blur", "invert", "tone")
            .map { setting(scope, it) }
        private var lastMaterialSettings = materialSettings()
        val originalRotation = album.rotation
        var media: Any? = null
        var requestToken = ""
        private val renders = MediaRenderGeneration()
        var overlay: View? = null
        var overlayMode = -1
        var rendered: Bitmap? = null
        var rotation: ObjectAnimator? = null
        @Volatile var disposed = false
        var imageVisibility: Int? = null
        private var roundApplied = false
        private var activeColors: Pair<Int, Int>? = null
        private val hidden = WeakHashMap<View, Int>()
        private val textColors = WeakHashMap<TextView, MediaPropertyLease<ColorStateList>>()
        private val scrollingText = listOf("titleText", "artistText").mapNotNull {
            (field(holder, it) as? TextView)?.let(::MediaTextMarquee)
        }
        private val imageColors = WeakHashMap<ImageView, MediaPropertyLease<ColorStateList?>>()
        private val seekThumb = MediaPropertyLease<ColorStateList?> { a, b -> a === b }
        private val seekProgress = MediaPropertyLease<ColorStateList?> { a, b -> a === b }
        private val seekBackground = MediaPropertyLease<ColorStateList?> { a, b -> a === b }
        private var nativePaused = false
        private val islandTheme = MediaIslandTheme()
        private var seekPaint: Paint? = null
        private val seekFilter = MediaPropertyLease<ColorFilter?> { a, b -> a === b }
        private var seekShaderColor: Int? = null
        private var lastMotion: Boolean? = null
        private var lastTone: MediaFlowTone? = null
        private val visibilityListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (hasOverrides) { safely("layout background") { applyBackground() }; motion() }
        }
        private val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) { windowDetached = false; apply() }
            override fun onViewDetachedFromWindow(v: View) {
                windowDetached = true
                stopMotion()
                if (removeAppearance()) nativeRebindRequest.defer()
            }
        }
        private val nativeClip = android.graphics.Rect()
        private val overlayClip = android.graphics.Rect()
        private val preDraw = android.view.ViewTreeObserver.OnPreDrawListener {
            if (hasOverrides) { syncBackgroundGeometry(); motion() }
            true
        }

        fun geometryReport(): String {
            fun bounds(v: View?) = if (v == null) "none" else "${v.javaClass.simpleName}:${v.width}x${v.height},layout=${v.layoutParams?.width}x${v.layoutParams?.height},clip=${v.clipBounds}"
            return "native[${bounds(background)}],custom[${bounds(overlay)}],progress[${bounds(field(holder, "seekBar") as? View)}]" +
                ",ownership[alpha=${backgroundAlpha.held},context=${themeContext.held},rebind=$nativeRebinds/$nativeRebindAvailable,pending=${nativeRebindRequest.pending}]"
        }

        private fun syncBackgroundGeometry() {
            val bg = background ?: return
            val view = overlay ?: return
            if (view.parent !== bg.parent) return
            // Native AOD can crop or resize the background independently of its layout params.
            // The custom surface must follow that geometry rather than retain the seek-bar area.
            if (view.left != bg.left || view.top != bg.top || view.right != bg.right || view.bottom != bg.bottom) {
                view.layout(bg.left, bg.top, bg.right, bg.bottom)
            }
            if (view.pivotX != bg.pivotX) view.pivotX = bg.pivotX
            if (view.pivotY != bg.pivotY) view.pivotY = bg.pivotY
            if (view.scaleX != bg.scaleX) view.scaleX = bg.scaleX
            if (view.scaleY != bg.scaleY) view.scaleY = bg.scaleY
            if (view.translationX != bg.translationX) view.translationX = bg.translationX
            if (view.translationY != bg.translationY) view.translationY = bg.translationY
            val clipped = bg.getClipBounds(nativeClip)
            val overlayClipped = view.getClipBounds(overlayClip)
            if (clipped != overlayClipped || (clipped && nativeClip != overlayClip)) {
                view.clipBounds = if (clipped) nativeClip else null
            }
        }

        init {
            album.addOnAttachStateChangeListener(attachListener)
            album.addOnLayoutChangeListener(visibilityListener)
            album.viewTreeObserver.addOnPreDrawListener(preDraw)
        }

        fun apply() {
            if (disposed || windowDetached || !album.isAttachedToWindow) return
            applyElements()
            applyTheme()
            refreshPendingMaterial()
            applyBackground()
            motion()
        }

        fun applyElements() {
            val cover = setting(scope, "cover")
            val round = cover == 1 || cover == 2
            if (round) {
                // Controllers can be observed before attachment finishes installing native
                // rounding. Capture the live shape when taking ownership, never our own oval.
                if (album.outlineProvider !== oval) {
                    originalOutline = album.outlineProvider
                    originalClip = album.clipToOutline
                }
                if (albumView.outlineProvider !== oval) {
                    originalParentOutline = albumView.outlineProvider
                    originalParentClip = albumView.clipToOutline
                }
            }
            if (round || roundApplied) {
                val imageOutline = if (round) oval else originalOutline
                val parentOutline = if (round) oval else originalParentOutline
                if (album.outlineProvider !== imageOutline) album.outlineProvider = imageOutline
                if (album.clipToOutline != (round || originalClip)) album.clipToOutline = round || originalClip
                if (albumView.outlineProvider !== parentOutline) albumView.outlineProvider = parentOutline
                val parentClip = if (round) false else originalParentClip
                if (albumView.clipToOutline != parentClip) albumView.clipToOutline = parentClip
                if (roundApplied != round) {
                    album.invalidateOutline()
                    albumView.invalidateOutline()
                }
            }
            roundApplied = round
            if (cover == 3) {
                if (imageVisibility == null) imageVisibility = albumView.visibility
                if (albumView.visibility != View.GONE) albumView.visibility = View.GONE
            } else if (imageVisibility != null) {
                albumView.visibility = imageVisibility!!
                imageVisibility = null
            }
            for ((fieldName, hide) in listOf("appIcon" to on(scope, "hideSource"), "seamless" to (on(scope, "hideDevice")))) {
                val view = field(holder, fieldName) as? View ?: continue
                if (hide) {
                    hidden.putIfAbsent(view, view.visibility)
                    if (view.visibility != View.GONE) view.visibility = View.GONE
                } else hidden.remove(view)?.let { view.visibility = it }
            }
            if (cover != 2) stopRotation()
        }

        fun applyTheme() {
            if (disposed || windowDetached || !album.isAttachedToWindow) return
            val theme = forcedTheme(scope)
            if (scope == "island") safely("island theme") { islandTheme.apply(field(holder, "player") as? View, theme == 2) }
            owner.get()?.let { controller ->
                val current = field(controller, "context") as? Context ?: return@let
                if (theme == 0) {
                    themeContext.release(current) { Xp.setObjectField(controller, "context", it) }
                    contextTheme = 0
                } else if (!themeContext.owns(current) || contextTheme != theme) {
                    val base = themeContext.nativeValue(current)
                    val desired = themed(base, theme)
                    if (current.resources.configuration.uiMode != desired.resources.configuration.uiMode)
                        themeContext.write(current, desired) { Xp.setObjectField(controller, "context", it) }
                    contextTheme = theme
                }
            }
            if (theme > 0 && !MediaCardConfig.customBackground(setting(scope, "background"), setting(scope, "flow"))) {
                foreground(if (theme == 1) Color.BLACK else Color.WHITE,
                    if (theme == 1) 0xb3000000.toInt() else 0xccffffff.toInt())
            }
        }

        private fun applyBackground() {
            val bg = background ?: return
            val style = setting(scope, "background")
            val flow = setting(scope, "flow")
            val pausedRestore = style == 0 && flow in 1..3 && on(scope, "pauseRestore") && !playing()
            if (!MediaCardConfig.customBackground(style, flow) || pausedRestore) {
                if (overlay != null || requestToken.isNotEmpty() || backgroundAlpha.held) {
                    val owned = overlay != null || backgroundAlpha.held || activeColors != null || nativePaused
                    removeOverlay(); restoreColors()
                    if (owned) rebindNativeMaterial()
                    applyTheme()
                }
                if (flow == 4 && scope == "island") {
                    backgroundAlpha.write(bg.alpha, 0f) { bg.alpha = it }
                    pauseNative()
                } else {
                    resumeNative()
                }
                return
            }
            if (windowDetached || !album.isAttachedToWindow) return
            val parent = bg.parent as? ViewGroup ?: return
            val w = bg.width.takeIf { it > 0 } ?: parent.width
            val h = bg.height.takeIf { it > 0 } ?: parent.height
            if (w <= 0 || h <= 0) return
            val icon = field(media, "artwork") as? Icon
            val packageName = field(media, "packageName")?.toString().orEmpty()
            val key = "$revision|$w,$h|$style|$flow|$packageName|${field(media, "song")}|${field(media, "artist")}|${System.identityHashCode(icon)}"
            if (key == requestToken) { activeColors?.let { foreground(it.first, it.second) }; return }
            requestToken = key
            val token = renders.invalidate()
            val version = revision
            val snapshot = values
            val weak = WeakReference(this)
            val source = icon ?: album.drawable?.constantState?.newDrawable()?.mutate()
            val loader = owner.get()?.javaClass?.classLoader ?: return
            worker.execute {
                var bitmap: Bitmap? = null
                try {
                    if (weak.get()?.renders?.accepts(token) != true) return@execute
                    val s = weak.get() ?: return@execute
                    val ctx = s.album.context
                    val drawable = when (source) {
                        is Icon -> source.loadDrawable(ctx)
                        is Drawable -> source
                        else -> null
                    } ?: return@execute
                    if (style > 0) {
                        val engine = renderer ?: MediaCardBackgroundRenderer(loader).also { renderer = it }
                        val result = engine.renderDrawable(ctx, drawable, packageName, style,
                            snapshot.getValue("$scope.blur"), snapshot.getValue("$scope.invert") != 0,
                            snapshot.getValue("$scope.tone"), minOf(w, 640), minOf(h, 400)) ?: return@execute
                        bitmap = result.bitmap
                        val owned = result.bitmap
                        main.post {
                            val active = weak.get()
                            if (active == null || !active.acceptsRender(token, version)) { owned.recycle(); return@post }
                            active.showStatic(owned, result.colors)
                        }
                        bitmap = null
                    } else {
                        if (flow in 1..2) {
                            val api = flowApi ?: NativeMusicBgApi.create(loader).also { flowApi = it }
                            val palette = if (flow == 1) api.extractSystemPalette(drawable) else {
                                val input = drawableBitmap(drawable) ?: return@execute
                                try {
                                    val color = MediaCardColorExtractor.extractThemePalette(input, 1).onBlackBackground.firstOrNull() ?: return@execute
                                    api.createPalette(color)
                                } finally { input.recycle() }
                            }
                            main.post {
                                val active = weak.get()
                                if (active == null || !active.acceptsRender(token, version)) return@post
                                active.showNativeFlow(api, palette, flow)
                            }
                        } else {
                            val input = drawableBitmap(drawable) ?: return@execute
                            val artwork = try { MediaFlowArtwork.prepare(input, blur = false) } finally { input.recycle() }
                            if (artwork == null) return@execute
                            main.post {
                                val active = weak.get()
                                if (active == null || !active.acceptsRender(token, version)) return@post
                                active.showFlow(artwork, flow)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Xp.w("Media card rendering failed: $t")
                } finally { bitmap?.recycle() }
            }
        }

        private fun acceptsRender(token: Long, version: Long): Boolean {
            val style = setting(scope, "background")
            val flow = setting(scope, "flow")
            return !disposed && !windowDetached && album.isAttachedToWindow && version == revision && renders.accepts(token)
                    && MediaCardConfig.customBackground(style, flow)
                    && !(style == 0 && on(scope, "pauseRestore") && !playing())
        }

        private fun attachOverlay(view: View) {
            val bg = background ?: return
            val parent = bg.parent as? ViewGroup ?: return
            view.isClickable = false
            view.isFocusable = false
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            // BACKGROUND outlines inspect the drawable on their argument. The overlay has no
            // native material drawable, so resolve the shape from the actual OEM background.
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(target: View, outline: Outline) {
                    bg.outlineProvider?.getOutline(bg, outline)
                    if (outline.isEmpty || outline.radius == 0f) {
                        val parentOutline = Outline()
                        (bg.parent as? View)?.let { it.outlineProvider?.getOutline(it, parentOutline) }
                        if (!parentOutline.isEmpty && parentOutline.radius > 0f) {
                            outline.setRoundRect(0, 0, target.width, target.height, parentOutline.radius)
                        }
                    }
                    if (outline.isEmpty || outline.radius == 0f) {
                        val id = bg.resources.getIdentifier("notification_item_bg_radius", "dimen", "com.android.systemui")
                        val radius = if (id != 0) bg.resources.getDimension(id) else 26f * target.resources.displayMetrics.density
                        outline.setRoundRect(0, 0, target.width, target.height, radius)
                    }
                }
            }
            view.clipToOutline = true
            // Anchor to the native surface instead of copying a fixed height. Otherwise this
            // sibling can keep a wrap-content player tall after AOD removes the progress row.
            val params = (if (bg.id > 0) MediaFlowOverlayLayout.createConstraintFill(bg.layoutParams, bg.id) else null)
                ?: MediaFlowOverlayLayout.copyForOverlay(bg.layoutParams) ?: return
            parent.addView(view, parent.indexOfChild(bg) + 1, params)
            overlay = view
            syncBackgroundGeometry()
            // Moving colors complement the native themed surface. Static artwork replaces it.
            if (view is ImageView) backgroundAlpha.write(bg.alpha, 0f) { bg.alpha = it }
            pauseNative()
        }

        private fun showStatic(bitmap: Bitmap, colors: NotificationMediaColorConfig) {
            if (overlay !is ImageView) { removeOverlay(false); attachOverlay(ImageView(album.context).apply { scaleType = ImageView.ScaleType.FIT_XY }); overlayMode = -1 }
            val view = overlay as? ImageView ?: run { bitmap.recycle(); return }
            val drawable = BitmapDrawable(view.resources, bitmap)
            val old = rendered?.let { BitmapDrawable(view.resources, it) }
            if (on(scope, "animate") && old != null) {
                view.setImageDrawable(TransitionDrawable(arrayOf(old, drawable)).apply { isCrossFadeEnabled = true; startTransition(250) })
            } else view.setImageDrawable(drawable)
            // Do not recycle: the transition may first draw much later when the shade opens.
            rendered = bitmap
            foreground(colors.textPrimary, colors.textSecondary)
        }

        private fun showNativeFlow(api: NativeMusicBgApi, palette: MediaAmbientFlowPalette, mode: Int) {
            if (overlay == null || !api.accepts(overlay!!) || overlayMode != mode) {
                removeOverlay(false)
                attachOverlay(api.createView(if (forcedTheme(scope) == 0) album.context else themed(album.context, forcedTheme(scope))))
                overlayMode = mode
            }
            val view = overlay ?: return
            api.setGradientColor(view, palette.mainColor, palette.colors)
            // Start once before reconciling playback; later updates use pause/resume.
            api.start(view)
            lastMotion = null
            foreground(if (tone() == MediaFlowTone.LIGHT) Color.BLACK else Color.WHITE,
                if (tone() == MediaFlowTone.LIGHT) 0xb3000000.toInt() else 0xccffffff.toInt())
            motion()
        }

        private fun showFlow(artwork: MediaFlowArtwork, mode: Int) {
            if (overlay !is MediaFlowBackgroundView || overlayMode != mode) {
                removeOverlay(false)
                attachOverlay(MediaFlowBackgroundView(album.context, appleMusicStyle = mode == 3))
                overlayMode = mode
            }
            (overlay as? MediaFlowBackgroundView)?.update(artwork, tone(), shouldMove(), on(scope, "animate"))
            foreground(if (tone() == MediaFlowTone.LIGHT) Color.BLACK else Color.WHITE,
                if (tone() == MediaFlowTone.LIGHT) 0xb3000000.toInt() else 0xccffffff.toInt())
        }

        private fun playing() = field(media, "isPlaying") == true
        private fun tone(): MediaFlowTone = when (forcedTheme(scope)) {
            1 -> MediaFlowTone.LIGHT
            2 -> MediaFlowTone.DARK
            else -> if (((field(owner.get(), "context") as? Context) ?: album.context).resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
                MediaFlowTone.DARK else MediaFlowTone.LIGHT
        }
        private fun shouldMove() = MediaCardConfig.animate(playing(), on(scope, "pauseRestore"),
            album.isAttachedToWindow && visible(background),
            !sleeping && Main.screenOnCached())

        private fun updateScrollingText() {
            val enabled = on(scope, "scrollText")
            val awake = !sleeping && Main.screenOnCached()
            scrollingText.forEach { it.update(enabled, awake && visible(it.view) && it.view.alpha > .01f) }
        }

        fun motion() {
            if (disposed) return
            updateScrollingText()
            val visible = visible(album) && !sleeping && Main.screenOnCached()
            if (setting(scope, "cover") == 2 && playing() && visible) {
                if (rotation == null) rotation = ObjectAnimator.ofFloat(album, View.ROTATION, album.rotation, album.rotation + 360f).apply {
                    duration = 20000L; interpolator = LinearInterpolator(); repeatCount = ValueAnimator.INFINITE; start()
                } else if (rotation?.isPaused == true) rotation?.resume()
            } else rotation?.takeIf { it.isRunning }?.pause()
            val move = shouldMove()
            val currentTone = tone()
            if (lastMotion != move || lastTone != currentTone) {
                lastMotion = move; lastTone = currentTone
                (overlay as? MediaFlowBackgroundView)?.update(tone = currentTone, playing = move)
                val native = overlay
                if (native != null && overlayMode in 1..2) safely("native playback") {
                    flowApi?.let { api -> if (move) { api.resume(native); api.ensureFrameLoopEnabled(native) } else api.pause(native) }
                }
            }
        }

        private fun stopMotion() { scrollingText.forEach { it.update(on(scope, "scrollText"), false) }; rotation?.pause(); overlay?.takeIf { overlayMode in 1..2 }?.let { runCatching { flowApi?.pause(it) } }; lastMotion = false; (overlay as? MediaFlowBackgroundView)?.update(tone = tone(), playing = false) }
        private fun stopRotation() { if (rotation != null) { rotation?.cancel(); rotation = null; album.rotation = originalRotation } }

        private fun foreground(primary: Int, secondary: Int) {
            activeColors = primary to secondary
            for ((name, color) in listOf("titleText" to primary, "artistText" to secondary,
                "elapsedTimeView" to primary, "totalTimeView" to primary)) {
                val text = field(holder, name) as? TextView ?: continue
                if (text.currentTextColor != color) textColors.getOrPut(text) {
                    MediaPropertyLease { a, b -> a === b }
                }.write(text.textColors, ColorStateList.valueOf(color)) { text.setTextColor(it) }
            }
            val tint = ColorStateList.valueOf(primary)
            val actions = runCatching { Xp.callMethod(holder, "getActionList") as? List<*> }.getOrNull().orEmpty()
            val icons = (listOf(field(holder, "seamlessIcon")) + actions + (0..4).map { field(holder, "action$it") }).filterIsInstance<ImageView>().distinct()
            for (image in icons) imageColors.getOrPut(image) {
                MediaPropertyLease { a, b -> a === b }
            }.write(image.imageTintList, tint) { image.imageTintList = it }
            (field(holder, "seekBar") as? SeekBar)?.let { seek ->
                if (seekPaint == null) {
                    seekPaint = field(seek, "mPaint") as? Paint
                }
                runCatching { Xp.callMethod(seek, "setForegroundPrimaryColor", primary) }
                runCatching { Xp.callMethod(seek, "setBackgroundPrimaryColor", primary and 0x00ffffff or 0x33000000) }
                seekPaint?.let { paint ->
                    if (seekShaderColor != primary || paint.colorFilter == null) {
                        seekFilter.write(paint.colorFilter, BlendModeColorFilter(primary, BlendMode.SRC_IN)) { paint.colorFilter = it }
                        seekShaderColor = primary; seek.invalidate()
                    }
                }
                seekThumb.write(seek.thumbTintList, tint) { seek.thumbTintList = it }
                seekProgress.write(seek.progressTintList, tint) { seek.progressTintList = it }
                seekBackground.write(seek.progressBackgroundTintList,
                    ColorStateList.valueOf(primary and 0x00ffffff or 0x33000000)) { seek.progressBackgroundTintList = it }
            }
        }

        fun removeAppearance(): Boolean {
            val owned = overlay != null || backgroundAlpha.held || themeContext.held || activeColors != null || nativePaused
            removeOverlay()
            restoreColors()
            owner.get()?.let { controller -> (field(controller, "context") as? Context)?.let { current ->
                themeContext.release(current) { Xp.setObjectField(controller, "context", it) }
            } }
            contextTheme = 0
            return owned
        }

        fun settingsChanged(): Boolean {
            val next = materialSettings()
            if (lastMaterialSettings == next) return false
            lastMaterialSettings = next
            return removeAppearance()
        }

        fun refreshPendingMaterial() {
            if (disposed || windowDetached || !nativeRebindRequest.take(album.isAttachedToWindow)) return
            applyTheme()
            rebindNativeMaterial()
            applyTheme()
        }

        fun rebindNativeMaterial() {
            if (scope != "notification" || disposed) return
            if (windowDetached || !album.isAttachedToWindow) { nativeRebindRequest.defer(); return }
            val controller = owner.get() ?: return
            nativeRebindRequest.clear()
            val previousApplying = applying
            applying = true
            try {
                val result = NativeMediaMaterial.rebind(controller, album)
                nativeRebindAvailable = result != NativeMediaMaterial.Result.UNAVAILABLE
                if (result == NativeMediaMaterial.Result.DEFERRED) { nativeRebindRequest.defer(); return }
                if (result == NativeMediaMaterial.Result.REQUESTED) nativeRebinds++
                // The current controller owns native seek-bar colours as well as text tints.
                runCatching { Xp.callMethod(controller, "updateForegroundColors") }
            } finally { applying = previousApplying }
        }

        fun restoreColors() {
            activeColors = null
            textColors.forEach { (view, lease) -> lease.release(view.textColors) { view.setTextColor(it) } }; textColors.clear()
            imageColors.forEach { (view, lease) -> lease.release(view.imageTintList) { view.imageTintList = it } }; imageColors.clear()
            (field(holder, "seekBar") as? SeekBar)?.let { seek ->
                seekThumb.release(seek.thumbTintList) { seek.thumbTintList = it }
                seekProgress.release(seek.progressTintList) { seek.progressTintList = it }
                seekBackground.release(seek.progressBackgroundTintList) { seek.progressBackgroundTintList = it }
            }
            seekPaint?.let { paint -> seekFilter.release(paint.colorFilter) { paint.colorFilter = it } }
            seekPaint = null; seekShaderColor = null
        }

        private fun removeOverlay(cancelPending: Boolean = true) {
            if (cancelPending) { renders.invalidate(); requestToken = "" }
            lastMotion = null; lastTone = null
            (overlay as? MediaFlowBackgroundView)?.update(tone = tone(), playing = false)
            overlay?.takeIf { overlayMode in 1..2 }?.let { view -> runCatching { flowApi?.pause(view) } }
            (overlay?.parent as? ViewGroup)?.removeView(overlay)
            overlay = null
            overlayMode = -1
            background?.let { bg -> backgroundAlpha.release(bg.alpha) { bg.alpha = it } }
            rendered?.let { if (!it.isRecycled) it.recycle() }; rendered = null
            resumeNative()
        }

        private fun pauseNative() {
            if (!nativePaused && background?.javaClass?.name == "com.mi.widget.view.MusicBgView") {
                nativePaused = runCatching { Xp.callMethod(background, "pause") }.isSuccess
            }
        }

        private fun resumeNative() {
            if (nativePaused) {
                nativePaused = false
                // Do not wake an animation the OEM just paused for AOD or detach.
                if (!disposed && !windowDetached && !sleeping && Main.screenOnCached() && album.isAttachedToWindow && playing())
                    background?.let { runCatching { Xp.callMethod(it, "resume") } }
            }
        }

        fun restore() {
            islandTheme.restore()
            disposed = true
            nativeRebindRequest.clear()
            scrollingText.forEach { it.restore() }
            stopRotation(); removeAppearance()
            hidden.forEach { (view, visibility) -> view.visibility = visibility }; hidden.clear()
            imageVisibility?.let { albumView.visibility = it }; imageVisibility = null
            album.outlineProvider = originalOutline; album.clipToOutline = originalClip
            albumView.outlineProvider = originalParentOutline; albumView.clipToOutline = originalParentClip
            album.removeOnAttachStateChangeListener(attachListener)
            album.removeOnLayoutChangeListener(visibilityListener)
            if (album.viewTreeObserver.isAlive) album.viewTreeObserver.removeOnPreDrawListener(preDraw)
        }
    }

    private fun visible(view: View?): Boolean {
        if (view == null || !view.isAttachedToWindow || !view.isShown || view.windowVisibility != View.VISIBLE) return false
        var current: View? = view
        while (current != null) {
            // The native background itself has alpha zero while a custom sibling replaces it.
            if (current !== view && current.alpha <= .01f) return false
            current = current.parent as? View
        }
        return true
    }

    private fun themed(context: Context, theme: Int): Context {
        val config = Configuration(context.resources.configuration)
        config.uiMode = config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
            if (theme == 1) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES
        return context.createConfigurationContext(config)
    }

    private fun drawableBitmap(drawable: Drawable): Bitmap? = runCatching {
        val w = drawable.intrinsicWidth.coerceIn(1, 256)
        val h = drawable.intrinsicHeight.coerceIn(1, 256)
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bitmap ->
            drawable.setBounds(0, 0, w, h); drawable.draw(Canvas(bitmap))
        }
    }.getOrNull()

    private fun field(owner: Any?, name: String): Any? = if (owner == null) null else runCatching { Xp.getObjectField(owner, name) }.getOrNull()
    private inline fun safely(stage: String, action: () -> Unit) { try { action() } catch (t: Throwable) { Xp.w("Media card $stage failed: $t") } }
}
