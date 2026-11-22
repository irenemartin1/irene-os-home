package app.irene.pastel

import android.app.Activity
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class AppEntry(
    val label: String,
    val component: ComponentName,
    val icon: Drawable,
    val mono: Drawable?, // Android 13 themed-icon layer, if the app has one
    val color: Int = 0   // the app's main color (used for its creature)
)

class MainActivity : Activity() {

    companion object {
        const val DOCK_SLOTS = 4
        const val GRID_COLUMNS = 4
        const val DEFAULT_NAME = "irene"
        // apps stay in memory, so if Android restarts the launcher (e.g. after a wallpaper change)
        // it comes back instantly with your dock + home screen already filled in
        private var appCache: List<AppEntry> = emptyList()
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var bg: ForestView
    private lateinit var logo: ImageView
    private lateinit var brandText: TextView
    private lateinit var homeLayer: LinearLayout
    private lateinit var drawerLayer: LinearLayout
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var greetText: TextView
    private lateinit var noteCard: TextView
    private lateinit var hintText: TextView
    private lateinit var dock: LinearLayout
    private lateinit var homeGrid: LinearLayout
    private lateinit var pageDots: TextView
    private var homePage = 0
    private lateinit var search: EditText
    private lateinit var grid: GridView
    private val adapter = AppAdapter()

    private var allApps: List<AppEntry> = emptyList()
    private var shown: List<AppEntry> = emptyList()
    private var palette = Palettes.all[0]
    private var underwater = false
    private var world: World? = null          // any other Irene OS theme (null = fairy forest / underwater)
    private lateinit var scene: ThemeSceneView
    private val themeKey get() = prefs.getString("theme", null) ?: if (prefs.getBoolean("underwater", false)) "underwater" else "forest"
    private lateinit var ocean: OceanView
    private lateinit var sensors: SensorManager
    private val shake = ShakeDetector { toggleUnderwater() }
    private val moods get() = if (underwater) Palettes.ocean else Palettes.all
    private val paletteKey get() = if (underwater) "palette_ocean" else "palette"
    private var drawerOpen = false
    private var pickingSlot = -1

    private val themedIcons get() = prefs.getBoolean("themed", true)
    private val userName get() = prefs.getString("name", DEFAULT_NAME) ?: DEFAULT_NAME

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = updateClock()
    }
    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = loadApps()
    }

    // ───────────────────────── lifecycle ─────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("pastel", MODE_PRIVATE)
        underwater = themeKey == "underwater"
        world = Worlds.byKey(themeKey)
        palette = currentPalette()
        sensors = getSystemService(SENSOR_SERVICE) as SensorManager

        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        setContentView(buildUi())
        // dark theme → white status bar icons
        val light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(0, light)

        // only the active scene is drawn (the hidden ones cost nothing)
        for (v in listOf<View>(bg, ocean, scene)) { v.alpha = 0f; v.visibility = View.INVISIBLE }
        activeScene().apply { alpha = 1f; visibility = View.VISIBLE }
        world?.let { scene.world = it }
        bg.palette = Palettes.all[prefs.getInt("palette", 0).coerceIn(0, Palettes.all.lastIndex)]
        if (appCache.isNotEmpty()) { allApps = appCache; shown = appCache }
        applyPalette()
        updateClock()
        loadApps()

        register(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK).apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        })
        register(pkgReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        })
    }

    override fun onResume() {
        super.onResume()
        HealthSync.maybeSync(this) // hourly, only if health sync is on
        runScene(activeScene(), true)
        updateClock()
        // shake 3x to toggle underwater mode
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(shake, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // apply the themed wallpaper only while you're away from home, so Android's recolor
        // (which briefly restarts the launcher) never interrupts you
        syncWallpaper()
        bg.stop(); ocean.stop(); scene.stop()
        sensors.unregisterListener(shake)
    }

    override fun onStop() {
        super.onStop()
        // coming back home should always land on the home screen, not the drawer
        if (drawerOpen || drawerProgress > 0f) {
            drawerAnim?.cancel()
            drawerOpen = false; pickingSlot = -1
            setDrawerProgress(0f)
            drawerLayer.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        unregisterReceiver(tickReceiver)
        unregisterReceiver(pkgReceiver)
        super.onDestroy()
    }

    // Home button pressed while already home
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // used by the automatic tests to flip through every theme
        intent.getStringExtra("irene_theme")?.let { k ->
            if (Worlds.entries.any { it.key == k } && k != themeKey) setTheme(k)
            return
        }
        if (drawerOpen) closeDrawer()
        else if (homePage != 0) { homePage = 0; refreshHome() } // Home press -> back to page 1
    }

    @Deprecated("fine for a launcher")
    override fun onBackPressed() {
        if (drawerOpen) closeDrawer() // on the home screen, back does nothing
    }

    // ── swipe handling ──
    // Works like the Pixel launcher: the app drawer follows your finger as you drag up (or down to
    // close), and a quick flick finishes it. Android owns the thin gesture strip at the very bottom
    // (Home / Recents), so swipes are tracked from anywhere just above it — including on the dock.
    private var downX = 0f
    private var downY = 0f
    private var trackSwipe = false
    private var dragMode = 0          // 0 = deciding, 1 = pulling drawer up, 2 = pushing drawer down, 3 = gesture used
    private var tracker: VelocityTracker? = null
    private var gestureStrip = 0      // height of Android's own bottom gesture area (px)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = ev.rawX; downY = ev.rawY; dragMode = 0
            val screenH = window.decorView.height
            trackSwipe = screenH == 0 || ev.rawY < screenH - gestureStrip - dp(4)
            tracker?.recycle()
            tracker = VelocityTracker.obtain()
            if (underwater && !drawerOpen) ocean.puff(ev.x, ev.y)
            // tapping anywhere outside the note puts the keyboard away
            if (noteCard.hasFocus()) {
                val r = android.graphics.Rect()
                noteCard.getGlobalVisibleRect(r)
                if (!r.contains(ev.rawX.toInt(), ev.rawY.toInt())) { noteCard.clearFocus(); hideKeyboard(noteCard) }
            }
        }
        tracker?.addMovement(ev)

        // dragging an app around the home screen
        if (dragApp != null) {
            if (dragNeedsCancel) { cancelTouchFor(ev); dragNeedsCancel = false }
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (hypot(ev.rawX - downX, ev.rawY - downY) > dp(10)) dragMoved = true
                    moveDragView(ev.rawX, ev.rawY)
                    checkEdgeFlip(ev.rawX)
                }
                MotionEvent.ACTION_UP -> finishHomeDrag(ev.rawX, ev.rawY, false)
                MotionEvent.ACTION_CANCEL -> finishHomeDrag(ev.rawX, ev.rawY, true)
            }
            return true
        }
        if (!trackSwipe) return super.dispatchTouchEvent(ev)

        val dy = ev.rawY - downY
        val dx = ev.rawX - downX
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (dragMode == 0) {
                    val slop = dp(12)
                    if (abs(dy) > slop && abs(dy) > abs(dx) * 1.2f) {
                        if (!drawerOpen && dy < 0) { dragMode = 1; beginDrawerDrag() }
                        else if (drawerOpen && dy > 0 && !grid.canScrollVertically(-1)) { dragMode = 2; drawerAnim?.cancel() }
                        else if (!drawerOpen && dy > dp(50)) { dragMode = 3; expandNotifications() }
                    } else if (!drawerOpen && abs(dx) > dp(50) && abs(dx) > abs(dy) * 1.4f) {
                        dragMode = 3
                        turnPage(if (dx < 0) 1 else -1) // swipe left / right = next / previous home page
                    }
                    if (dragMode != 0) cancelTouchFor(ev)
                }
                val range = drawerRange()
                when (dragMode) {
                    1 -> setDrawerProgress(((-dy - dp(12)) / range).coerceIn(0f, 1f))
                    2 -> setDrawerProgress((1f - (dy - dp(12)) / range).coerceIn(0f, 1f))
                }
                if (dragMode != 0) return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragMode == 1 || dragMode == 2) {
                    tracker?.computeCurrentVelocity(1000)
                    val vy = tracker?.yVelocity ?: 0f
                    val open = when {
                        vy < -dp(700) -> true                 // flicked up
                        vy > dp(700) -> false                 // flicked down
                        dragMode == 1 -> drawerProgress > 0.3f
                        else -> drawerProgress > 0.7f
                    }
                    settleDrawer(open)
                }
                val used = dragMode != 0
                dragMode = 0
                tracker?.recycle(); tracker = null
                if (used) return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /** Tells the views under the finger that this touch became a swipe (so nothing gets tapped). */
    private fun cancelTouchFor(ev: MotionEvent) {
        val cancel = MotionEvent.obtain(ev)
        cancel.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }

    // ───────────────────────── UI ─────────────────────────

    private fun buildUi(): View {
        val root = FrameLayout(this)
        rootFrame = root
        bg = ForestView(this)
        root.addView(bg, match())
        ocean = OceanView(this)
        root.addView(ocean, match())
        scene = ThemeSceneView(this)
        root.addView(scene, match())

        // HOME
        homeLayer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            // long-press any empty spot on the home screen = settings
            setOnLongClickListener { settingsMenu(homeGrid); true }
        }
        homeLayer.addView(View(this), LinearLayout.LayoutParams(1, dp(14)))

        // Your fairy logo: drop it in res/drawable as fairy_logo.png and it shows up here
        logo = ImageView(this).apply {
            val id = resources.getIdentifier("fairy_logo", "drawable", packageName)
            if (id != 0) setImageResource(id) else visibility = View.GONE
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { nextPalette() }
        }
        homeLayer.addView(logo, LinearLayout.LayoutParams(dp(96), dp(96)).apply { bottomMargin = dp(4) })

        brandText = text(14f, weight = 700).apply {
            text = "✦ irene os ✦"
            letterSpacing = 0.35f
            isAllCaps = true
        }
        // (plain home screen: no title, clock, date or greeting)

        clockText = title(72f).apply {
            letterSpacing = -0.03f
            setOnLongClickListener { launchIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)); true }
        }
        dateText = text(18f, weight = 500)
        greetText = text(17f).apply {
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { nextPalette() }
            setOnLongClickListener { settingsMenu(it); true }
        }
        // your one note: tap it and type right on the home screen, it saves as you go
        noteCard = EditText(this).apply {
            textSize = 15f
            setPadding(dp(20), dp(14), dp(20), dp(14))
            minLines = 1
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            hint = "tap to write your note \u2727"
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
            setText(prefs.getString("note", ""))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    prefs.edit().putString("note", s?.toString().orEmpty()).apply()
                }
            })
        }
        homeLayer.addView(noteCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(20), dp(6), dp(20), 0) })

        // HOME SCREEN PAGES: your apps in a grid; swipe left / right between pages
        homeGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
                if (bottom - top != oldBottom - oldTop) post { refreshHome() }
            }
        }
        homeLayer.addView(homeGrid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            setMargins(dp(12), dp(14), dp(12), 0)
        })
        pageDots = text(12f).apply { letterSpacing = 0.3f }
        homeLayer.addView(pageDots)

        hintText = text(13f).apply {
            text = "swipe up for all apps ✧"; letterSpacing = 0.05f
            setPadding(dp(24), dp(10), dp(24), dp(10))
            setOnClickListener { openDrawer() } // tapping the hint opens apps too
        }
        homeLayer.addView(hintText)

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        homeLayer.addView(dock, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(20), dp(10), dp(20), dp(16)) })
        root.addView(homeLayer, match())

        // DRAWER
        drawerLayer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            isClickable = true // touches on the drawer never fall through to the home screen
        }
        search = EditText(this).apply {
            isSingleLine = true
            textSize = 16f
            setPadding(dp(22), dp(14), dp(22), dp(14))
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, _, _ -> shown.firstOrNull()?.let { onAppTapped(it) }; true }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) = filter(s?.toString().orEmpty())
            })
        }
        grid = GridView(this).apply {
            numColumns = GRID_COLUMNS
            verticalSpacing = dp(16)
            clipToPadding = false
            isVerticalScrollBarEnabled = false
            selector = ColorDrawable(Color.TRANSPARENT)
            setPadding(0, dp(18), 0, dp(24))
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, pos, _ -> onAppTapped(shown[pos]) }
            setOnItemLongClickListener { _, v, pos, _ -> appMenu(v, shown[pos]); true }
        }
        drawerLayer.addView(search, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) })
        drawerLayer.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(drawerLayer, match())

        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            gestureStrip = insets.getInsets(WindowInsets.Type.mandatorySystemGestures()).bottom
            homeLayer.setPadding(0, bars.top, 0, bars.bottom)
            drawerLayer.setPadding(dp(16), bars.top, dp(16), max(bars.bottom, ime.bottom))
            insets
        }
        return root
    }

    private fun applyPalette() {
        val w = world
        if (underwater) ocean.palette = palette else if (w == null) bg.palette = palette else if (scene.world !== w) scene.world = w
        // light themes (Kawaii, Coquette, Zen…) get dark status-bar icons, dark themes get white ones
        val barFlags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (w != null && !w.dark) barFlags else 0, barFlags)
        brandText.text = if (underwater) "◦ irene os ◦" else "✦ irene os ✦"
        hintText.text = when { w != null -> "swipe up for all apps ${w.emoji}"; underwater -> "swipe up for all apps ◦"; else -> "swipe up for all apps ✧" }
        listOf(clockText, noteCard).forEach { it.setTextColor(palette.ink) }
        listOf(dateText, greetText, hintText, brandText).forEach { it.setTextColor(palette.soft) }
        brandText.setShadowLayer(dp(8).toFloat(), 0f, 0f, palette.glow)
        clockText.setShadowLayer(dp(18).toFloat(), 0f, 0f, (palette.glow and 0x00FFFFFF) or (0x66 shl 24))
        noteCard.background = glass(24)
        dock.background = glass(34)
        drawerLayer.setBackgroundColor((palette.top and 0x00FFFFFF) or (0xEB shl 24))
        search.background = glass(30)
        search.setTextColor(palette.ink)
        search.setHintTextColor(palette.soft)
        updateClock()
        refreshNote()
        refreshDock()
        refreshHome()
        adapter.notifyDataSetChanged()
    }

    private fun updateClock() {
        if (!::clockText.isInitialized) return
        val now = Date()
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm"
        clockText.text = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(now).lowercase()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val part = if (underwater) when (hour) {
            in 5..11 -> "sunlight on the waves"
            in 12..16 -> "the fish are out playing"
            in 17..21 -> "the jellyfish are glowing"
            else -> "even the whales are asleep, go to bed"
        } else when (hour) {
            in 5..11 -> "the forest is waking up"
            in 12..16 -> "sun through the leaves"
            in 17..21 -> "the fireflies are out"
            else -> "the fae are dreaming, go to sleep"
        }
        greetText.text = "${palette.spark} $part, $userName ${palette.spark}"
    }

    private fun refreshNote() {
        noteCard.setHintTextColor(palette.soft)
    }

    // ───────────────────────── apps ─────────────────────────

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            val list = pm.queryIntentActivities(main, 0)
                .filter { it.activityInfo.packageName != packageName }
                .map { ri ->
                    val icon = ri.loadIcon(pm)
                    val mono = if (Build.VERSION.SDK_INT >= 33 && icon is AdaptiveIconDrawable) icon.monochrome else null
                    AppEntry(
                        ri.loadLabel(pm).toString(),
                        ComponentName(ri.activityInfo.packageName, ri.activityInfo.name),
                        icon, mono,
                        CreatureIcon.bodyColorFor(icon, ri.activityInfo.packageName.hashCode())
                    )
                }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                appCache = list
                allApps = list
                seedDockDefaults()
                seedHome()
                filter(search.text.toString())
                refreshDock()
                refreshHome()
            }
        }.start()
    }

    private fun filter(q: String) {
        val query = q.trim().lowercase()
        shown = if (query.isEmpty()) allApps else allApps.filter { it.label.lowercase().contains(query) }
        adapter.notifyDataSetChanged()
    }

    /** First run: fill the dock with phone / messages / camera / browser. */
    private fun seedDockDefaults() {
        if (prefs.getBoolean("seeded", false) || allApps.isEmpty()) return
        val wants = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER),
        )
        val e = prefs.edit()
        wants.forEachIndexed { i, intent ->
            val pkg = try {
                @Suppress("DEPRECATION")
                packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            } catch (_: Exception) { null }
            allApps.firstOrNull { it.component.packageName == pkg }
                ?.let { e.putString("fav$i", it.component.flattenToString()) }
        }
        e.putBoolean("seeded", true).apply()
    }

    private fun favAt(i: Int): AppEntry? {
        val s = prefs.getString("fav$i", null) ?: return null
        return allApps.firstOrNull { it.component.flattenToString() == s }
    }

    private fun refreshDock() {
        if (allApps.isEmpty()) return // still loading: keep whatever is showing
        dock.removeAllViews()
        for (i in 0 until DOCK_SLOTS) {
            val app = favAt(i)
            val cell = FrameLayout(this)
            val bubble = newBubble()
            bindBubble(bubble, app)
            cell.addView(bubble, FrameLayout.LayoutParams(dp(58), dp(58), Gravity.CENTER))
            cell.setOnClickListener { if (app != null) launch(app) else openDrawer(i) }
            cell.setOnLongClickListener { dockMenu(it, i, app); true }
            dock.addView(cell, LinearLayout.LayoutParams(0, dp(74), 1f))
        }
    }

    private fun onAppTapped(app: AppEntry) {
        if (pickingSlot >= 0) {
            pin(app, pickingSlot)
            closeDrawer()
        } else launch(app)
    }

    private fun pin(app: AppEntry, slot: Int) {
        prefs.edit().putString("fav$slot", app.component.flattenToString()).apply()
        refreshDock()
        toast("pinned ${app.label} ✧")
    }

    private fun launch(app: AppEntry) {
        launchIntent(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        )
    }

    private fun launchIntent(i: Intent) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { toast("couldn't open that ✧") }
    }

    // ───────────────────────── drawer ─────────────────────────

    private fun openDrawer(pick: Int = -1) {
        beginDrawerDrag()
        pickingSlot = pick
        search.hint = if (pick >= 0) "pick an app for slot ${pick + 1} ✧" else searchHint()
        settleDrawer(true)
    }

    private fun closeDrawer() = settleDrawer(false)

    private fun searchHint() = if (world != null) "search ${world!!.name.lowercase()} ${world!!.emoji}" else if (underwater) "search enchanted underwater ◦" else "search the enchanted forest ✿"

    // ── drawer motion (shared by taps, swipes and the Home button) ──
    private var drawerProgress = 0f       // 0 = closed, 1 = fully open
    private var drawerAnim: ValueAnimator? = null

    /** How far your finger travels to fully open the drawer. */
    private fun drawerRange(): Float {
        val h = if (homeLayer.height > 0) homeLayer.height else resources.displayMetrics.heightPixels
        return h * 0.45f
    }

    private fun beginDrawerDrag() {
        drawerAnim?.cancel()
        if (!drawerOpen && drawerProgress == 0f) {
            pickingSlot = -1
            search.setText("")
            search.hint = searchHint()
            grid.setSelection(0)
        }
    }

    private fun setDrawerProgress(p: Float) {
        drawerProgress = p
        val range = drawerRange()
        drawerLayer.visibility = if (p > 0f) View.VISIBLE else View.GONE
        drawerLayer.alpha = p
        drawerLayer.translationY = (1f - p) * range
        homeLayer.alpha = 1f - p
        homeLayer.translationY = -p * dp(36)
    }

    private fun settleDrawer(open: Boolean) {
        drawerAnim?.cancel()
        drawerOpen = open
        if (!open) { pickingSlot = -1; hideKeyboard() }
        val target = if (open) 1f else 0f
        drawerAnim = ValueAnimator.ofFloat(drawerProgress, target).apply {
            duration = (260 * abs(target - drawerProgress)).toLong().coerceAtLeast(90)
            interpolator = DecelerateInterpolator(1.6f)
            addUpdateListener { setDrawerProgress(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!drawerOpen) drawerLayer.visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun expandNotifications() {
        try {
            @Suppress("WrongConstant")
            val sb = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (_: Throwable) { }
    }

    // ───────────────────────── menus & dialogs ─────────────────────────

    private fun appMenu(anchor: View, app: AppEntry) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 99, 0, "add to home screen ✧")
            for (i in 0 until DOCK_SLOTS) menu.add(0, i, i, "pin to dock slot ${i + 1}")
            menu.add(0, 100, 100, "app info")
            menu.add(0, 101, 101, "uninstall")
            setOnMenuItemClickListener {
                when (it.itemId) {
                    100 -> appInfo(app)
                    101 -> uninstall(app)
                    99 -> addToHome(app)
                    else -> pin(app, it.itemId)
                }
                true
            }
        }.show()
    }

    private fun dockMenu(anchor: View, slot: Int, app: AppEntry?) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 0, 0, "change app")
            if (app != null) {
                menu.add(0, 1, 1, "remove from dock")
                menu.add(0, 2, 2, "app info")
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    0 -> openDrawer(slot)
                    1 -> { prefs.edit().remove("fav$slot").apply(); refreshDock() }
                    2 -> app?.let { a -> appInfo(a) }
                }
                true
            }
        }.show()
    }

    private fun settingsMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 7, 0, "change theme ✦ (${Worlds.entries.firstOrNull { e -> e.key == themeKey }?.label ?: "fairy"})")
            if (world == null) menu.add(0, 0, 0, "next color mood")
            menu.add(0, 1, 1, if (themedIcons) "creature icons: on ✓" else "creature icons: off")
            menu.add(0, 2, 2, "change my name")
            menu.add(0, 3, 3, "set as default home app")
            menu.add(0, 4, 4, if (underwater) "enchanted underwater: on ◦ (shake 3x)" else "enchanted underwater (or shake 3x)")
            menu.add(0, 5, 5, if (useWallpaper) "themed phone wallpaper: on ✓" else "themed phone wallpaper: off")
            menu.add(0, 6, 6, if (HealthSync.key(this@MainActivity).isBlank()) "health sync ♡" else "health sync ♡ (sync now)")
            setOnMenuItemClickListener {
                when (it.itemId) {
                    7 -> themePicker()
                    0 -> nextPalette()
                    1 -> { prefs.edit().putBoolean("themed", !themedIcons).apply(); applyPalette() }
                    2 -> textDialog("what should i call you?", userName) { v ->
                        prefs.edit().putString("name", v.ifBlank { DEFAULT_NAME }).apply(); updateClock()
                    }
                    3 -> launchIntent(Intent(Settings.ACTION_HOME_SETTINGS))
                    4 -> toggleUnderwater()
                    5 -> {
                        prefs.edit().putBoolean("wallpaper", !useWallpaper).remove("wp_key").apply()
                        if (useWallpaper) toast("your wallpaper updates next time you leave home \u2727")
                    }
                    6 -> healthSyncMenu()
                }
                true
            }
        }.show()
    }

    // ── health sync: Health Connect → Irene OS ──
    private fun healthSyncMenu() {
        when (HealthSync.availability(this)) {
            1 -> { toast("install / update Health Connect first ♡"); HealthSync.openStore(this); return }
            2 -> { toast("Health Connect isn't available on this phone"); return }
        }
        val hasKey = HealthSync.key(this).isNotBlank()
        val items = if (hasKey) arrayOf("sync now", "allow / change health permissions", "paste a new key", "turn off health sync")
                    else arrayOf("paste my key from Irene OS")
        AlertDialog.Builder(this)
            .setTitle("health sync ♡")
            .setItems(items) { _, which ->
                when (if (hasKey) which else 2) {
                    0 -> { toast("syncing…"); HealthSync.sync(this) { _, msg -> runOnUiThread { toast(msg) } } }
                    1 -> HealthSync.requestPermissions(this)
                    2 -> textDialog("paste your key (Irene OS → Settings → Health sync)", "") { v ->
                        if (v.isBlank()) return@textDialog
                        HealthSync.setKey(this, v)
                        HealthSync.requestPermissions(this)
                    }
                    3 -> { HealthSync.setKey(this, ""); toast("health sync off") }
                }
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HealthSync.REQUEST_CODE) {
            toast("syncing your health data…")
            HealthSync.sync(this) { _, msg -> runOnUiThread { toast(msg) } }
        }
    }

    private fun editNote() = textDialog("note ✧", prefs.getString("note", "").orEmpty(), multiline = true) { v ->
        prefs.edit().putString("note", v.trim()).apply(); refreshNote()
    }

    private fun textDialog(title: String, current: String, multiline: Boolean = false, onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(current)
            setSelection(current.length)
            isSingleLine = !multiline
            if (multiline) minLines = 3
        }
        val wrap = FrameLayout(this).apply { setPadding(dp(22), dp(8), dp(22), 0); addView(input) }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(wrap)
            .setPositiveButton("save") { _, _ -> onSave(input.text.toString()) }
            .setNegativeButton("cancel", null)
            .show()
    }

    private fun nextPalette() {
        if (world != null) { themePicker(); return }
        val idx = (moods.indexOf(palette) + 1) % moods.size
        palette = moods[idx]
        prefs.edit().putInt(paletteKey, idx).apply()
        applyPalette()
        toast("${palette.spark} ${palette.name}")
    }

    private fun currentPalette(): Palette = world?.palette() ?:
        moods[prefs.getInt(paletteKey, 0).coerceIn(0, moods.lastIndex)]

    /** Shake 3x (or settings menu): dive underwater / come back to the theme you were on. */
    private fun toggleUnderwater() {
        setTheme(if (underwater) (prefs.getString("theme_land", "forest") ?: "forest") else "underwater")
    }

    /** Every Irene OS theme, plus the fairy forest and underwater worlds. */
    private fun themePicker() {
        val items = Worlds.entries
        val cur = items.indexOfFirst { it.key == themeKey }
        AlertDialog.Builder(this)
            .setTitle("choose your irene os theme ✦")
            .setSingleChoiceItems(items.map { "${it.label}\n   ${it.desc}" }.toTypedArray(), cur) { dlg, which ->
                dlg.dismiss()
                if (items[which].key != themeKey) setTheme(items[which].key)
            }
            .setNegativeButton("close", null)
            .show()
    }

    private fun setTheme(key: String) {
        val wasUnder = underwater
        underwater = key == "underwater"
        world = Worlds.byKey(key)
        prefs.edit().putString("theme", key).putBoolean("underwater", underwater).apply()
        if (!underwater) prefs.edit().putString("theme_land", key).apply()
        palette = currentPalette()
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        world?.let { scene.world = it }
        if (underwater) ocean.palette = palette else if (world == null) bg.palette = palette
        val active = activeScene()
        for (v in listOf<View>(bg, ocean, scene)) {
            v.animate().cancel()
            if (v === active) {
                if (v.visibility != View.VISIBLE) { v.alpha = 0f; v.visibility = View.VISIBLE }
                runScene(v, true)
                v.animate().alpha(1f).setDuration(650).start()
            } else if (v.visibility == View.VISIBLE) {
                v.animate().alpha(0f).setDuration(650)
                    .withEndAction { if (activeScene() !== v) { runScene(v, false); v.visibility = View.INVISIBLE } }.start()
            }
        }
        if (underwater && !wasUnder) ocean.post { ocean.burst() }
        toast(world?.let { "${it.emoji} ${it.name}" } ?: if (underwater) "◦ enchanted underwater ◦" else "✦ enchanted forest ✦")
        applyPalette()
    }

    private fun activeScene(): View = when { world != null -> scene; underwater -> ocean; else -> bg }

    private fun runScene(v: View, on: Boolean) {
        when {
            v === bg -> if (on) bg.start() else bg.stop()
            v === ocean -> if (on) ocean.start() else ocean.stop()
            v === scene -> if (on) scene.start() else scene.stop()
        }
    }

    private fun appInfo(app: AppEntry) = launchIntent(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.component.packageName}"))
    )

    private fun uninstall(app: AppEntry) = launchIntent(
        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.component.packageName}"))
    )

    // ───────────────────────── icon bubbles ─────────────────────────

    private fun newBubble(): FrameLayout = FrameLayout(this).apply {
        addView(ImageView(this@MainActivity).apply { tag = "img" }, match())
        addView(TextView(this@MainActivity).apply {
            tag = "plus"; text = "+"; textSize = 26f; gravity = Gravity.CENTER
        }, match())
    }

    private fun bindBubble(bubble: FrameLayout, app: AppEntry?) {
        // whimsical orb behind the icon + a little ocean/forest charm on top (same charm per app, every time)
        val seed = app?.component?.packageName?.hashCode() ?: 0
        val density = resources.displayMetrics.density
        val creatures = themedIcons && app != null
        bubble.background = if (creatures) null else BubbleDrawable(palette, underwater, seed, density, charmLayer = false)
        bubble.foreground = if (app != null && !creatures) BubbleDrawable(palette, underwater, seed, density, charmLayer = true) else null
        val img = bubble.findViewWithTag<ImageView>("img")
        val plus = bubble.findViewWithTag<TextView>("plus")
        if (app == null) {
            img.setImageDrawable(null)
            plus.visibility = View.VISIBLE
            plus.setTextColor(palette.soft)
            return
        }
        plus.visibility = View.GONE
        val mono = app.mono
        if (creatures) {
            // the app itself, turned into a little forest / sea creature in the app's own colors
            val art = if (mono != null) fresh(mono) else fresh(app.icon)
            img.setPadding(0, 0, 0, 0)
            img.setImageDrawable(CreatureIcon(art, mono != null, app.color, underwater, seed, density, palette.glow, world))
        } else {
            img.setPadding(dp(9), dp(9), dp(9), dp(9))
            img.setImageDrawable(fresh(app.icon))
        }
    }

    /** Drawables can't be shared between views; make a copy per view. */
    private fun fresh(d: Drawable): Drawable = d.constantState?.newDrawable(resources)?.mutate() ?: d

    inner class AppAdapter : BaseAdapter() {
        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val app = shown[position]
            val cell = (convertView as? LinearLayout) ?: emptyCell()
            bindCell(cell, app)
            return cell
        }
    }

    /** One app: creature icon + glowing name. Used by the drawer and the home screen. */
    private fun emptyCell(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        addView(newBubble().apply { tag = "bubble" }, LinearLayout.LayoutParams(dp(58), dp(58)))
        addView(TextView(this@MainActivity).apply {
            tag = "label"; textSize = 12f; maxLines = 1
            ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.CENTER
            setPadding(dp(2), dp(6), dp(2), 0)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun bindCell(cell: LinearLayout, app: AppEntry) {
        bindBubble(cell.findViewWithTag("bubble"), app)
        cell.findViewWithTag<TextView>("label").apply {
            text = app.label
            setTextColor(palette.ink)
            setShadowLayer(dp(6).toFloat(), 0f, 0f, palette.glow) // soft glow under the name
        }
    }

    // ───────────────────────── home screen pages ─────────────────────────
    // Saved as a list of slots (empty slots allowed), split into pages of 4 columns x however many
    // rows fit. Long-press + drag to move an app anywhere; hold it at the screen edge to change page;
    // drop it on the dock to swap it in. Long-press + let go (no drag) = remove / app info.

    private lateinit var rootFrame: FrameLayout
    private var dragApp: AppEntry? = null
    private var dragFrom = -1
    private var dragCell: View? = null
    private var dragView: ImageView? = null
    private var dragMoved = false
    private var dragNeedsCancel = false
    private var edgeSince = 0L

    private fun homeApps(): MutableList<String> =
        prefs.getString("home_apps", "").orEmpty().split("|").toMutableList().also { trimEnd(it) }

    private fun trimEnd(list: MutableList<String>) {
        while (list.isNotEmpty() && list.last().isBlank()) list.removeAt(list.size - 1)
    }

    private fun saveHomeApps(list: MutableList<String>) {
        trimEnd(list)
        prefs.edit().putString("home_apps", list.joinToString("|")).apply()
    }

    private fun pageSize(): Int = (homeGrid.height / dp(92)).coerceIn(1, 7) * 4

    /** One entry per slot: the app, or null for an empty spot. */
    private fun homeSlots(): List<AppEntry?> =
        homeApps().map { key -> if (key.isBlank()) null else allApps.firstOrNull { it.component.flattenToString() == key } }

    private fun pageCount(): Int {
        val size = pageSize()
        val used = max(1, (homeSlots().size + size - 1) / size)
        return if (dragApp != null) max(used, homePage + 1) else used
    }

    private fun refreshHome() {
        if (!::homeGrid.isInitialized || homeGrid.height == 0) return
        val slots = homeSlots()
        val size = pageSize()
        val pages = pageCount()
        homePage = homePage.coerceIn(0, pages - 1)
        homeGrid.removeAllViews()
        for (r in 0 until size / 4) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (c in 0 until 4) {
                val slot = homePage * size + r * 4 + c
                val app = slots.getOrNull(slot)
                val cell: View = if (app != null) emptyCell().also { cv ->
                    bindCell(cv, app)
                    cv.setOnClickListener { launch(app) }
                    cv.setOnLongClickListener { startHomeDrag(cv, app, slot); true }
                    if (slot == dragFrom) cv.alpha = 0.25f
                } else View(this)
                row.addView(cell, LinearLayout.LayoutParams(0, dp(92), 1f))
            }
            homeGrid.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        if (slots.all { it == null } && allApps.isNotEmpty() && dragApp == null) {
            homeGrid.removeAllViews()
            homeGrid.addView(text(13f).apply {
                text = "swipe up, long-press any app \u2192 \"add to home screen\" \u2727"
                setTextColor(palette.soft)
                setPadding(dp(24), dp(40), dp(24), 0)
            })
        }
        pageDots.text = if (pages > 1) (0 until pages).joinToString("  ") { if (it == homePage) "\u25cf" else "\u25cb" } else ""
        pageDots.setTextColor(palette.soft)
    }

    private fun turnPage(delta: Int) {
        val next = homePage + delta
        val limit = if (dragApp != null) pageCount() + 1 else pageCount()
        if (next < 0 || next >= limit) return
        homePage = next
        refreshHome()
        homeGrid.alpha = 0f
        homeGrid.translationX = delta * dp(60).toFloat()
        homeGrid.animate().alpha(1f).translationX(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun addToHome(app: AppEntry) {
        val list = homeApps()
        val key = app.component.flattenToString()
        if (key in list) { toast("already on your home screen \u2727"); return }
        val free = list.indexOfFirst { it.isBlank() }
        val slot = if (free >= 0) free else list.size
        if (free >= 0) list[free] = key else list += key
        saveHomeApps(list)
        if (drawerOpen) closeDrawer()
        homePage = slot / pageSize()
        refreshHome()
        toast("added ${app.label} to home \u2727")
    }

    private fun startHomeDrag(cell: View, app: AppEntry, slot: Int) {
        dragApp = app; dragFrom = slot; dragCell = cell
        dragMoved = false; dragNeedsCancel = true; edgeSince = 0L
        cell.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        cell.alpha = 0.25f
        val mono = app.mono
        val seed = app.component.packageName.hashCode()
        val img = ImageView(this).apply {
            setImageDrawable(
                if (themedIcons) CreatureIcon(if (mono != null) fresh(mono) else fresh(app.icon), mono != null, app.color,
                    underwater, seed, resources.displayMetrics.density, palette.glow, world)
                else fresh(app.icon)
            )
            scaleX = 1.2f; scaleY = 1.2f
            elevation = dp(10).toFloat()
        }
        rootFrame.addView(img, FrameLayout.LayoutParams(dp(66), dp(66)))
        dragView = img
        moveDragView(downX, downY)
    }

    private fun moveDragView(x: Float, y: Float) {
        dragView?.let { v ->
            v.x = x - dp(33)
            v.y = y - dp(33) - dp(24) // a little above your finger so you can see it
        }
    }

    /** Hold the app at the left/right edge to carry it to another page. */
    private fun checkEdgeFlip(x: Float) {
        val w = window.decorView.width
        if (w == 0 || (x > dp(28) && x < w - dp(28))) { edgeSince = 0L; return }
        val now = SystemClock.uptimeMillis()
        if (edgeSince == 0L) edgeSince = now
        else if (now - edgeSince > 650) {
            edgeSince = now
            turnPage(if (x < w / 2) -1 else 1)
        }
    }

    private fun slotAt(x: Float, y: Float): Int? {
        val g = IntArray(2)
        homeGrid.getLocationOnScreen(g)
        if (x < g[0] || x > g[0] + homeGrid.width || y < g[1] || y > g[1] + homeGrid.height) return null
        val size = pageSize()
        val col = ((x - g[0]) / (homeGrid.width / 4f)).toInt().coerceIn(0, 3)
        val row = ((y - g[1]) / dp(92)).toInt()
        if (row >= size / 4) return null
        return homePage * size + row * 4 + col
    }

    private fun finishHomeDrag(x: Float, y: Float, cancelled: Boolean) {
        val app = dragApp ?: return
        val from = dragFrom
        val anchor = dragCell
        dragView?.let { rootFrame.removeView(it) }
        dragView = null; dragApp = null; dragFrom = -1; dragCell = null
        if (cancelled) { refreshHome(); return }
        if (!dragMoved) { // long-press without dragging -> options
            refreshHome()
            homeMenu(anchor ?: homeGrid, app, from)
            return
        }
        val list = homeApps()
        // dropped on the dock: swap with that dock slot
        val d = IntArray(2)
        dock.getLocationOnScreen(d)
        if (y >= d[1] - dp(8) && y <= d[1] + dock.height + dp(8)) {
            val slot = ((x - d[0]) / (dock.width / DOCK_SLOTS.toFloat())).toInt().coerceIn(0, DOCK_SLOTS - 1)
            val previous = prefs.getString("fav$slot", null)
            while (list.size <= from) list += ""
            list[from] = previous ?: ""
            saveHomeApps(list)
            pin(app, slot)
            refreshHome()
            return
        }
        // dropped on the grid: move there (swaps if another app is in that spot)
        val target = slotAt(x, y)
        if (target != null && target != from) {
            while (list.size <= max(target, from)) list += ""
            val other = list[target]
            list[target] = list[from]
            list[from] = other
            saveHomeApps(list)
        }
        refreshHome()
    }

    private fun homeMenu(anchor: View, app: AppEntry, slot: Int) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 0, 0, "remove from home screen")
            menu.add(0, 1, 1, "app info")
            menu.add(0, 2, 2, "uninstall")
            setOnMenuItemClickListener {
                when (it.itemId) {
                    0 -> {
                        val list = homeApps()
                        if (slot in list.indices) { list[slot] = ""; saveHomeApps(list) }
                        refreshHome()
                    }
                    1 -> appInfo(app)
                    2 -> uninstall(app)
                }
                true
            }
        }.show()
    }

    /** First run: put a few everyday apps on page 1. */
    private fun seedHome() {
        if (prefs.getBoolean("home_seeded", false) || allApps.isEmpty()) return
        val wanted = listOf(
            "com.google.android.calendar", "com.google.android.apps.maps", "com.google.android.apps.photos",
            "com.google.android.gm", "com.google.android.youtube", "com.google.android.deskclock",
            "com.google.android.apps.nbu.files", "com.android.settings", "com.android.chrome",
            "com.google.android.apps.messaging", "com.google.android.dialer", "com.google.android.GoogleCamera"
        )
        val dockKeys = (0 until DOCK_SLOTS).mapNotNull { prefs.getString("fav$it", null) }
        val picks = wanted.mapNotNull { p -> allApps.firstOrNull { it.component.packageName == p } }
            .map { it.component.flattenToString() }
            .filter { it !in dockKeys }
            .take(8)
            .toMutableList()
        saveHomeApps(picks)
        prefs.edit().putBoolean("home_seeded", true).apply()
    }

    // ───────────────────────── themed phone wallpaper ─────────────────────────

    private val useWallpaper get() = prefs.getBoolean("wallpaper", true)

    /**
     * Paints the current scene as the phone's real wallpaper + lock screen. Android's own swipe-home
     * animation, Recents and lock screen show the wallpaper, so this keeps those moments themed too.
     * Only runs when the look actually changed.
     */
    private fun syncWallpaper(force: Boolean = false) {
        if (!useWallpaper) return
        val key = themeKey + ":" + palette.name + ":v2"
        if (!force && prefs.getString("wp_key", null) == key) return
        val scene: View = activeScene()
        if (scene.width == 0 || scene.height == 0) return
        val bmp = try {
            Bitmap.createBitmap(scene.width, scene.height, Bitmap.Config.ARGB_8888).also { scene.draw(Canvas(it)) }
        } catch (_: Throwable) { return }
        prefs.edit().putString("wp_key", key).apply()
        Thread {
            try {
                WallpaperManager.getInstance(applicationContext)
                    .setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } catch (_: Throwable) {
                prefs.edit().remove("wp_key").apply()
            }
            bmp.recycle()
        }.start()
    }

    // ───────────────────────── helpers ─────────────────────────

    private fun text(size: Float, weight: Int = 400) = TextView(this).apply {
        textSize = size
        gravity = Gravity.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, weight, false)
    }

    /** Chunky title font. Drop a .ttf in res/font named title_font.ttf to use your own. */
    private fun title(size: Float) = TextView(this).apply {
        textSize = size
        gravity = Gravity.CENTER
        val id = resources.getIdentifier("title_font", "font", packageName)
        typeface = if (id != 0) resources.getFont(id)
                   else Typeface.create(Typeface.SERIF, 900, false)
    }

    /** Dark frosted glass card with a thin glowing edge. */
    private fun glass(radiusDp: Int) = GradientDrawable().apply {
        setColor(palette.card)
        cornerRadius = dp(radiusDp).toFloat()
        setStroke(dp(1), palette.ring)
    }

    private fun match() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun hideKeyboard(from: View = search) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(from.windowToken, 0)
    }

    private fun register(r: BroadcastReceiver, f: IntentFilter) {
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(r, f)
    }
}
