package app.irene.pastel

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import kotlin.math.max

data class AppEntry(
    val label: String,
    val component: ComponentName,
    val icon: Drawable,
    val mono: Drawable? // Android 13 themed-icon layer, if the app has one
)

class MainActivity : Activity() {

    companion object {
        const val DOCK_SLOTS = 4
        const val GRID_COLUMNS = 4
        const val DEFAULT_NAME = "irene"
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
    private lateinit var search: EditText
    private lateinit var grid: GridView
    private lateinit var gestures: GestureDetector
    private val adapter = AppAdapter()

    private var allApps: List<AppEntry> = emptyList()
    private var shown: List<AppEntry> = emptyList()
    private var palette = Palettes.all[0]
    private var underwater = false
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
        underwater = prefs.getBoolean("underwater", false)
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

        gestures = GestureDetector(this, Swipes())
        ocean.alpha = if (underwater) 1f else 0f
        bg.palette = Palettes.all[prefs.getInt("palette", 0).coerceIn(0, Palettes.all.lastIndex)]
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
        if (underwater) ocean.start() else bg.start()
        updateClock()
        // shake 3x to toggle underwater mode
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(shake, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        bg.stop(); ocean.stop()
        sensors.unregisterListener(shake)
    }

    override fun onStop() {
        super.onStop()
        // coming back home should always land on the home screen, not the drawer
        if (drawerOpen) {
            drawerOpen = false; pickingSlot = -1
            drawerLayer.visibility = View.GONE
            homeLayer.alpha = 1f
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
        if (drawerOpen) closeDrawer()
    }

    @Deprecated("fine for a launcher")
    override fun onBackPressed() {
        if (drawerOpen) closeDrawer() // on the home screen, back does nothing
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestures.onTouchEvent(ev)
        if (underwater && !drawerOpen && ev.actionMasked == MotionEvent.ACTION_DOWN) ocean.puff(ev.x, ev.y)
        return super.dispatchTouchEvent(ev)
    }

    // ───────────────────────── UI ─────────────────────────

    private fun buildUi(): View {
        val root = FrameLayout(this)
        bg = ForestView(this)
        root.addView(bg, match())
        ocean = OceanView(this)
        root.addView(ocean, match())

        // HOME
        homeLayer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        homeLayer.addView(View(this), LinearLayout.LayoutParams(1, 0, 0.6f))

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
        homeLayer.addView(brandText)

        clockText = title(84f).apply {
            letterSpacing = -0.03f
            setOnLongClickListener { launchIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS)); true }
        }
        dateText = text(18f, weight = 500)
        greetText = text(17f).apply {
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener { nextPalette() }
            setOnLongClickListener { settingsMenu(it); true }
        }
        noteCard = text(15f).apply {
            setPadding(dp(20), dp(14), dp(20), dp(14))
            maxLines = 4
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(300)
            setOnClickListener { editNote() }
        }
        homeLayer.addView(clockText)
        homeLayer.addView(dateText)
        homeLayer.addView(greetText)
        homeLayer.addView(noteCard, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(22) })

        homeLayer.addView(View(this), LinearLayout.LayoutParams(1, 0, 1.2f))

        hintText = text(13f).apply { text = "swipe up to enter the forest ✧"; letterSpacing = 0.05f }
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
            homeLayer.setPadding(0, bars.top, 0, bars.bottom)
            drawerLayer.setPadding(dp(16), bars.top, dp(16), max(bars.bottom, ime.bottom))
            insets
        }
        return root
    }

    private fun applyPalette() {
        if (underwater) ocean.palette = palette else bg.palette = palette
        brandText.text = if (underwater) "◦ irene os ◦" else "✦ irene os ✦"
        hintText.text = if (underwater) "swipe up to dive deeper ◦" else "swipe up to enter the forest ✧"
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
        val note = prefs.getString("note", "").orEmpty()
        noteCard.text = note.ifBlank { "tap to leave a note for future you ✧" }
        noteCard.alpha = if (note.isBlank()) 0.7f else 1f
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
                        icon, mono
                    )
                }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                allApps = list
                seedDockDefaults()
                filter(search.text.toString())
                refreshDock()
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
        pickingSlot = pick
        search.setText("")
        search.hint = if (pick >= 0) "pick an app for slot ${pick + 1} ✧" else "search the forest ⋆˚✿"
        drawerOpen = true
        grid.setSelection(0)
        drawerLayer.visibility = View.VISIBLE
        drawerLayer.alpha = 0f
        drawerLayer.translationY = dp(60).toFloat()
        drawerLayer.animate().alpha(1f).translationY(0f).setDuration(240)
            .setInterpolator(DecelerateInterpolator()).start()
        homeLayer.animate().alpha(0f).setDuration(180).start()
    }

    private fun closeDrawer() {
        drawerOpen = false
        pickingSlot = -1
        hideKeyboard()
        drawerLayer.animate().alpha(0f).translationY(dp(60).toFloat()).setDuration(180)
            .withEndAction { if (!drawerOpen) drawerLayer.visibility = View.GONE }.start()
        homeLayer.animate().alpha(1f).setDuration(220).start()
    }

    private fun expandNotifications() {
        try {
            @Suppress("WrongConstant")
            val sb = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (_: Throwable) { }
    }

    inner class Swipes : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            val start = e1 ?: return false
            val dy = e2.y - start.y
            val dx = e2.x - start.x
            if (abs(dy) < abs(dx) || abs(dy) < dp(80)) return false
            if (!drawerOpen) {
                if (dy < 0) openDrawer() else expandNotifications()
            } else if (dy > 0 && !grid.canScrollVertically(-1)) {
                closeDrawer()
            }
            return true
        }
    }

    // ───────────────────────── menus & dialogs ─────────────────────────

    private fun appMenu(anchor: View, app: AppEntry) {
        PopupMenu(this, anchor).apply {
            for (i in 0 until DOCK_SLOTS) menu.add(0, i, i, "pin to dock slot ${i + 1}")
            menu.add(0, 100, 100, "app info")
            menu.add(0, 101, 101, "uninstall")
            setOnMenuItemClickListener {
                when (it.itemId) {
                    100 -> appInfo(app)
                    101 -> uninstall(app)
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
            menu.add(0, 0, 0, "next palette")
            menu.add(0, 1, 1, if (themedIcons) "themed icons: on ✓" else "themed icons: off")
            menu.add(0, 2, 2, "change my name")
            menu.add(0, 3, 3, "set as default home app")
            menu.add(0, 4, 4, if (underwater) "underwater mode: on ◦" else "underwater mode (or shake 3x)")
            setOnMenuItemClickListener {
                when (it.itemId) {
                    0 -> nextPalette()
                    1 -> { prefs.edit().putBoolean("themed", !themedIcons).apply(); applyPalette() }
                    2 -> textDialog("what should i call you?", userName) { v ->
                        prefs.edit().putString("name", v.ifBlank { DEFAULT_NAME }).apply(); updateClock()
                    }
                    3 -> launchIntent(Intent(Settings.ACTION_HOME_SETTINGS))
                    4 -> toggleUnderwater()
                }
                true
            }
        }.show()
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
        val idx = (moods.indexOf(palette) + 1) % moods.size
        palette = moods[idx]
        prefs.edit().putInt(paletteKey, idx).apply()
        applyPalette()
        toast("${palette.spark} ${palette.name}")
    }

    private fun currentPalette(): Palette =
        moods[prefs.getInt(paletteKey, 0).coerceIn(0, moods.lastIndex)]

    /** Shake 3x (or settings menu): dive into the ocean / come back to the forest. */
    private fun toggleUnderwater() {
        underwater = !underwater
        prefs.edit().putBoolean("underwater", underwater).apply()
        palette = currentPalette()
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        if (underwater) {
            ocean.palette = palette
            ocean.start()
            ocean.animate().alpha(1f).setDuration(700)
                .withEndAction { if (underwater) bg.stop() }.start()
            ocean.post { ocean.burst() }
            toast("◦ underwater mode ◦")
        } else {
            bg.start()
            ocean.animate().alpha(0f).setDuration(700)
                .withEndAction { if (!underwater) ocean.stop() }.start()
            toast("✦ back to the forest ✦")
        }
        applyPalette()
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
        bubble.background = BubbleDrawable(palette, underwater, seed, density, charmLayer = false)
        bubble.foreground = if (app != null) BubbleDrawable(palette, underwater, seed, density, charmLayer = true) else null
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
        if (themedIcons && mono != null) {
            img.setPadding(dp(2), dp(2), dp(2), dp(2))
            img.setImageDrawable(fresh(mono).apply { setTint(palette.ink) })
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
            val cell = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(newBubble().apply { tag = "bubble" }, LinearLayout.LayoutParams(dp(58), dp(58)))
                addView(TextView(this@MainActivity).apply {
                    tag = "label"; textSize = 12f; maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END; gravity = Gravity.CENTER
                    setPadding(dp(2), dp(6), dp(2), 0)
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            bindBubble(cell.findViewWithTag("bubble"), app)
            cell.findViewWithTag<TextView>("label").apply {
                text = app.label
                setTextColor(palette.ink)
                setShadowLayer(dp(6).toFloat(), 0f, 0f, palette.glow) // soft glow under the name
            }
            return cell
        }
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

    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(search.windowToken, 0)
    }

    private fun register(r: BroadcastReceiver, f: IntentFilter) {
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(r, f)
    }
}
