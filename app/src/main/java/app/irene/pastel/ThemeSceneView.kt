package app.irene.pastel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated background for every Irene OS theme (besides the forest + underwater scenes).
 * The sky + ground are drawn once into a cached picture; each frame only moves the particles.
 */
class ThemeSceneView(context: Context) : View(context) {

    var world: World = Worlds.all.first()
        set(value) { field = value; cache = null; seedParticles(); invalidate() }

    private val d = resources.displayMetrics.density
    private val rnd = Random(21)
    private var t = 0f
    private var running = false
    private var cache: Bitmap? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val rect = RectF()

    private class P(var x: Float, var y: Float, val size: Float, val speed: Float, val phase: Float, val color: Int)
    private val ps = mutableListOf<P>()

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            step(); invalidate()
            postDelayed(this, 50)
        }
    }

    fun start() { if (!running) { running = true; post(frame) } }
    fun stop() { running = false; removeCallbacks(frame) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { cache = null; seedParticles() }

    private fun colors(): IntArray = intArrayOf(world.accent, world.inColor, world.outColor, world.strong)

    private fun seedParticles() {
        ps.clear()
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        val n = when (world.particle) { Particles.NONE -> 0; Particles.STARS, Particles.DOTS, Particles.SNOW -> 55; Particles.TICKERS -> 14; else -> 22 }
        val cs = colors()
        repeat(n) { i ->
            ps += P(rnd.nextFloat() * w, rnd.nextFloat() * h, (3 + rnd.nextFloat() * 6) * d,
                (0.3f + rnd.nextFloat() * 0.9f) * d / 2.5f, rnd.nextFloat() * 6.28f, cs[i % cs.size])
        }
    }

    private fun step() {
        t += 0.05f
        val w = width.toFloat(); val h = height.toFloat()
        for (p in ps) when (world.particle) {
            Particles.STARS, Particles.DOTS, Particles.SPARKLES -> {} // they twinkle in place
            Particles.TICKERS, Particles.BUBBLES, Particles.EMBERS, Particles.HEARTS, Particles.PAWS, Particles.SMILEYS -> { // drift up
                p.y -= p.speed * 2; p.x += sin(t + p.phase) * 0.5f
                if (p.y < -20 * d) { p.y = h + 20 * d; p.x = rnd.nextFloat() * w }
            }
            else -> { // petals, leaves, confetti, daisies, butterflies, notes drift down
                p.y += p.speed * 2; p.x += sin(t * 0.8f + p.phase) * 1.1f
                if (p.y > h + 20 * d) { p.y = -20 * d; p.x = rnd.nextFloat() * w }
            }
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width; val h = height
        if (w == 0 || h == 0) return
        val bmp = cache ?: buildCache(w, h).also { cache = it }
        c.drawBitmap(bmp, 0f, 0f, null)
        for (p in ps) drawParticle(c, p)
    }

    // ───────── still layer: sky + ground ─────────

    private fun buildCache(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val W = w.toFloat(); val H = h.toFloat()
        fill.color = Color.BLACK // fully opaque, so the sky never shows the old wallpaper through it
        val stops = FloatArray(world.sky.size) { it / (world.sky.size - 1f).coerceAtLeast(1f) }
        fill.shader = if (world.sky.size > 1) LinearGradient(0f, 0f, 0f, H, world.sky, stops, Shader.TileMode.CLAMP) else null
        if (world.sky.size == 1) fill.color = world.sky[0]
        c.drawRect(0f, 0f, W, H, fill)
        fill.shader = null
        val g = if (world.dark) World.mix(world.base, world.accent, 0.16f) else World.mix(world.base, world.strong, 0.18f)
        drawGround(c, W, H, g)
        return bmp
    }

    private fun drawGround(c: Canvas, W: Float, H: Float, g: Int) {
        fill.color = g
        when (world.ground) {
            Grounds.HILLS -> { hill(c, W, H, 0.86f, W * 0.6f, World.withAlpha(g, 170)); hill(c, W, H, 0.9f, W * 0.9f, g) }
            Grounds.MUSHROOMS -> {
                hill(c, W, H, 0.9f, W * 0.8f, g)
                for ((fx, s) in listOf(0.12f to 1f, 0.2f to 0.6f, 0.82f to 0.85f)) mushroom(c, W * fx, H * 0.9f, W * 0.08f * s)
            }
            Grounds.SKYLINE -> {
                var x = 0f
                while (x < W) {
                    val bw = (28 + rnd.nextFloat() * 50) * d; val bh = H * (0.08f + rnd.nextFloat() * 0.18f)
                    fill.color = g; c.drawRect(x, H - bh, x + bw - 3 * d, H, fill)
                    fill.color = World.withAlpha(world.accent, 140)
                    var wy = H - bh + 8 * d
                    while (wy < H - 8 * d) {
                        var wx = x + 5 * d
                        while (wx < x + bw - 10 * d) { if (rnd.nextFloat() < 0.4f) c.drawRect(wx, wy, wx + 4 * d, wy + 5 * d, fill); wx += 9 * d }
                        wy += 11 * d
                    }
                    x += bw
                }
            }
            Grounds.WAVES, Grounds.PALMS -> {
                for (k in 0 until 3) {
                    fill.color = World.withAlpha(if (k == 2) g else world.accent, 60 + k * 50)
                    path.reset(); path.moveTo(0f, H)
                    var x = 0f
                    while (x <= W) { path.lineTo(x, H * (0.86f + k * 0.04f) + sin(x / (40 * d) + k) * 8 * d); x += 8 * d }
                    path.lineTo(W, H); path.close(); c.drawPath(path, fill)
                }
                if (world.ground == Grounds.PALMS) { palm(c, W * 0.1f, H * 0.9f, H * 0.25f, g); palm(c, W * 0.88f, H * 0.88f, H * 0.2f, g) }
            }
            Grounds.ZEN -> {
                fill.color = World.withAlpha(g, 120); c.drawRect(0f, H * 0.8f, W, H, fill)
                line.color = World.withAlpha(world.fg, 40); line.strokeWidth = 1.5f * d
                var y = H * 0.82f
                while (y < H) { c.drawLine(0f, y, W, y + sin(y) * 2, line); y += 10 * d }
                fill.color = g
                rect.set(W * 0.12f, H * 0.83f, W * 0.3f, H * 0.89f); c.drawOval(rect, fill)
                rect.set(W * 0.68f, H * 0.86f, W * 0.8f, H * 0.9f); c.drawOval(rect, fill)
            }
            Grounds.DANCE_FLOOR -> {
                val cols = 8; val tile = W / cols
                for (r in 0 until 4) for (col in 0 until cols) {
                    fill.color = World.withAlpha(colors()[(r + col) % 4], 40 + ((r * 3 + col) % 3) * 25)
                    c.drawRect(col * tile, H * 0.82f + r * tile * 0.6f, (col + 1) * tile - 2 * d, H * 0.82f + (r + 1) * tile * 0.6f - 2 * d, fill)
                }
                // the disco ball
                fill.color = World.withAlpha(world.fg, 200); c.drawCircle(W * 0.5f, H * 0.07f, 26 * d, fill)
                line.color = World.withAlpha(world.base, 120); line.strokeWidth = 1 * d
                for (k in -3..3) { c.drawLine(W * 0.5f - 26 * d, H * 0.07f + k * 7 * d, W * 0.5f + 26 * d, H * 0.07f + k * 7 * d, line); c.drawLine(W * 0.5f + k * 7 * d, H * 0.07f - 26 * d, W * 0.5f + k * 7 * d, H * 0.07f + 26 * d, line) }
            }
            Grounds.BIG_TOP -> {
                val stripes = 10
                for (k in 0 until stripes) {
                    fill.color = if (k % 2 == 0) World.withAlpha(world.accent, 70) else World.withAlpha(Color.WHITE, 90)
                    path.reset(); path.moveTo(W * 0.5f, -H * 0.05f)
                    path.lineTo(W * k / stripes, H * 0.12f); path.lineTo(W * (k + 1) / stripes, H * 0.12f); path.close()
                    c.drawPath(path, fill)
                }
                fill.color = World.withAlpha(world.accent, 90)
                for (k in 0 until stripes) c.drawCircle(W * (k + 0.5f) / stripes, H * 0.12f, W / stripes / 2, fill)
            }
            Grounds.TRACES -> {
                line.color = World.withAlpha(world.accent, 70); line.strokeWidth = 2 * d
                fill.color = World.withAlpha(world.accent, 120)
                repeat(14) {
                    var x = rnd.nextFloat() * W; var y = rnd.nextFloat() * H
                    path.reset(); path.moveTo(x, y)
                    repeat(4) { if (rnd.nextBoolean()) x += (rnd.nextFloat() - 0.5f) * 160 * d else y += (rnd.nextFloat() - 0.5f) * 160 * d; path.lineTo(x, y) }
                    c.drawPath(path, line); c.drawCircle(x, y, 3.5f * d, fill)
                }
            }
            Grounds.BOOKS -> {
                var x = 0f
                fill.color = World.mix(world.base, world.accent, 0.25f); c.drawRect(0f, H * 0.86f, W, H * 0.87f, fill)
                while (x < W) {
                    val bw = (10 + rnd.nextFloat() * 14) * d; val bh = H * (0.07f + rnd.nextFloat() * 0.05f)
                    fill.color = World.withAlpha(World.mix(g, colors()[rnd.nextInt(4)], 0.35f), 230)
                    c.drawRect(x, H * 0.86f - bh, x + bw - 1.5f * d, H * 0.86f, fill); x += bw
                }
                fill.color = World.withAlpha(g, 200); c.drawRect(0f, H * 0.87f, W, H, fill)
            }
            Grounds.FRAMES -> {
                line.color = World.withAlpha(world.accent, 150); line.strokeWidth = 3 * d
                for ((fx, fy, fw, fh) in listOf(floatArrayOf(0.08f, 0.16f, 0.26f, 0.16f), floatArrayOf(0.62f, 0.12f, 0.3f, 0.22f), floatArrayOf(0.12f, 0.6f, 0.22f, 0.14f), floatArrayOf(0.66f, 0.64f, 0.24f, 0.12f))) {
                    rect.set(W * fx, H * fy, W * (fx + fw), H * (fy + fh))
                    fill.color = World.withAlpha(World.mix(world.base, world.outColor, 0.25f), 120); c.drawRect(rect, fill)
                    c.drawRect(rect, line)
                }
            }
            Grounds.CLOUDS -> {
                fill.color = World.withAlpha(Color.WHITE, 170)
                for ((cx, cy, s) in listOf(Triple(0.15f, 0.9f, 1.2f), Triple(0.5f, 0.93f, 1.5f), Triple(0.88f, 0.9f, 1.1f), Triple(0.75f, 0.12f, 0.7f), Triple(0.2f, 0.18f, 0.6f))) {
                    val r = 34 * d * s
                    c.drawCircle(W * cx, H * cy, r, fill); c.drawCircle(W * cx - r * 0.9f, H * cy + r * 0.2f, r * 0.7f, fill); c.drawCircle(W * cx + r * 0.9f, H * cy + r * 0.2f, r * 0.7f, fill)
                }
            }
            Grounds.CASTLE -> {
                hill(c, W, H, 0.9f, W, World.withAlpha(Color.WHITE, 150))
                fill.color = World.withAlpha(world.accent, 90)
                val bx = W * 0.5f; val by = H * 0.88f; val s = 40 * d
                c.drawRect(bx - s, by - s * 1.6f, bx + s, by, fill)
                for (k in -1..1 step 2) {
                    c.drawRect(bx + k * s * 1.3f - s * 0.35f, by - s * 2.2f, bx + k * s * 1.3f + s * 0.35f, by, fill)
                    path.reset(); path.moveTo(bx + k * s * 1.3f - s * 0.45f, by - s * 2.2f); path.lineTo(bx + k * s * 1.3f, by - s * 2.9f); path.lineTo(bx + k * s * 1.3f + s * 0.45f, by - s * 2.2f); path.close(); c.drawPath(path, fill)
                }
            }
            Grounds.GRID -> {
                line.color = World.withAlpha(world.accent, 45); line.strokeWidth = 1 * d
                val horizon = H * 0.7f
                for (k in -12..12) c.drawLine(W / 2 + k * 18 * d, horizon, W / 2 + k * 90 * d, H, line)
                var y = horizon; var gap = 6 * d
                while (y < H) { c.drawLine(0f, y, W, y, line); y += gap; gap *= 1.25f }
            }
            Grounds.RAINBOW -> {
                val bands = intArrayOf(Color.parseColor("#ff6b6b"), Color.parseColor("#ffa94d"), Color.parseColor("#ffe066"), Color.parseColor("#69db7c"), Color.parseColor("#4dabf7"), Color.parseColor("#b197fc"))
                line.strokeWidth = 14 * d; line.strokeCap = Paint.Cap.BUTT
                for (k in bands.indices) {
                    line.color = World.withAlpha(bands[k], 110)
                    val r = W * 0.62f - k * 14 * d
                    rect.set(W * 0.5f - r, H * 0.95f - r, W * 0.5f + r, H * 0.95f + r); c.drawArc(rect, 180f, 180f, false, line)
                }
                line.strokeCap = Paint.Cap.ROUND
            }
            Grounds.STAINED -> {
                val cs = colors()
                val cell = W / 5
                for (r in 0 until 3) for (col in 0 until 5) {
                    fill.color = World.withAlpha(cs[(r * 2 + col) % 4], 45)
                    path.reset()
                    val x0 = col * cell; val y0 = H * 0.78f + r * cell * 0.5f
                    path.moveTo(x0, y0 + cell * 0.25f); path.lineTo(x0 + cell * 0.5f, y0); path.lineTo(x0 + cell, y0 + cell * 0.25f)
                    path.lineTo(x0 + cell, y0 + cell * 0.75f); path.lineTo(x0, y0 + cell * 0.75f); path.close()
                    c.drawPath(path, fill)
                    line.color = World.withAlpha(world.fg, 60); line.strokeWidth = 2 * d; c.drawPath(path, line)
                }
            }
            Grounds.CHART -> {
                line.color = World.withAlpha(world.accent, 110); line.strokeWidth = 3 * d
                path.reset(); path.moveTo(0f, H * 0.9f)
                var y = H * 0.9f; var x = 0f
                while (x < W) { x += 22 * d; y -= (rnd.nextFloat() - 0.35f) * 18 * d; path.lineTo(x, y) }
                c.drawPath(path, line)
                path.lineTo(W, H); path.lineTo(0f, H); path.close()
                fill.color = World.withAlpha(world.accent, 30); c.drawPath(path, fill)
            }
            Grounds.SAVANNA -> {
                fill.color = World.withAlpha(Color.parseColor("#ffb347"), 120); c.drawCircle(W * 0.72f, H * 0.78f, W * 0.2f, fill)
                hill(c, W, H, 0.86f, W, World.withAlpha(Color.parseColor("#d9a55a"), 200))
                acacia(c, W * 0.2f, H * 0.87f, W * 0.22f, World.mix(world.fg, world.base, 0.25f))
                acacia(c, W * 0.86f, H * 0.88f, W * 0.15f, World.mix(world.fg, world.base, 0.35f))
            }
            Grounds.PARK -> {
                hill(c, W, H, 0.88f, W, World.withAlpha(world.inColor, 130)); hill(c, W, H, 0.92f, W, World.withAlpha(world.inColor, 200))
                line.color = World.withAlpha(Color.WHITE, 230); line.strokeWidth = 4 * d
                var x = 8 * d
                while (x < W) { c.drawLine(x, H * 0.86f, x, H * 0.92f, line); x += 22 * d }
                c.drawLine(0f, H * 0.875f, W, H * 0.875f, line); c.drawLine(0f, H * 0.9f, W, H * 0.9f, line)
                fill.color = Color.parseColor("#d7e84a"); c.drawCircle(W * 0.3f, H * 0.95f, 9 * d, fill)
                line.color = World.withAlpha(Color.WHITE, 220); line.strokeWidth = 1.5f * d
                rect.set(W * 0.3f - 12 * d, H * 0.95f - 9 * d, W * 0.3f - 2 * d, H * 0.95f + 9 * d); c.drawArc(rect, -60f, 120f, false, line)
            }
            Grounds.DOMES -> {
                fill.color = World.withAlpha(Color.parseColor("#9fd3f5"), 110); c.drawRect(0f, H * 0.9f, W, H, fill)
                val houses = listOf(floatArrayOf(0.02f, 0.2f, 0.1f), floatArrayOf(0.2f, 0.22f, 0.14f), floatArrayOf(0.44f, 0.18f, 0.08f), floatArrayOf(0.6f, 0.24f, 0.12f), floatArrayOf(0.82f, 0.18f, 0.1f))
                for ((x0, bw, bh) in houses) {
                    fill.color = Color.WHITE; c.drawRect(W * x0, H * (0.9f - bh), W * (x0 + bw), H * 0.9f, fill)
                    line.color = World.withAlpha(world.fg, 30); line.strokeWidth = 1 * d; c.drawRect(W * x0, H * (0.9f - bh), W * (x0 + bw), H * 0.9f, line)
                    fill.color = world.accent
                    rect.set(W * (x0 + bw * 0.25f), H * (0.9f - bh) - W * bw * 0.25f, W * (x0 + bw * 0.75f), H * (0.9f - bh) + W * bw * 0.25f); c.drawArc(rect, 180f, 180f, true, fill)
                    fill.color = World.withAlpha(world.accent, 200); c.drawRect(W * (x0 + bw * 0.4f), H * (0.9f - bh * 0.45f), W * (x0 + bw * 0.6f), H * 0.9f, fill)
                }
            }
            Grounds.SPOTS -> {
                repeat(26) {
                    val x = rnd.nextFloat() * W; val y = rnd.nextFloat() * H; val s = (10 + rnd.nextFloat() * 16) * d
                    line.color = World.withAlpha(world.accent, 45); line.strokeWidth = s * 0.28f
                    rect.set(x - s, y - s * 0.7f, x + s, y + s * 0.7f); c.drawArc(rect, rnd.nextFloat() * 90f, 230f, false, line)
                    fill.color = World.withAlpha(world.accent, 25); c.drawOval(rect.apply { inset(s * 0.35f, s * 0.25f) }, fill)
                }
            }
            Grounds.TILE_ROW -> {
                val cell = W / 6
                for (r in 0 until 3) for (col in 0 until 6) {
                    val x0 = col * cell; val y0 = H - (r + 1) * cell
                    fill.color = Color.WHITE; c.drawRect(x0, y0, x0 + cell, y0 + cell, fill)
                    line.color = World.withAlpha(world.accent, 200); line.strokeWidth = 1.5f * d; c.drawRect(x0, y0, x0 + cell, y0 + cell, line)
                    fill.color = World.withAlpha(world.accent, 210)
                    val mx = x0 + cell / 2; val my = y0 + cell / 2
                    for (k in 0 until 4) { c.save(); c.rotate(k * 90f + 45f, mx, my); rect.set(mx - cell * 0.08f, my - cell * 0.38f, mx + cell * 0.08f, my - cell * 0.06f); c.drawOval(rect, fill); c.restore() }
                    c.drawCircle(mx, my, cell * 0.07f, fill)
                    for ((cx0, cy0) in listOf(x0 to y0, x0 + cell to y0, x0 to y0 + cell, x0 + cell to y0 + cell)) c.drawCircle(cx0, cy0, cell * 0.1f, fill)
                }
            }
            Grounds.GINGHAM -> {
                val sq = 22 * d
                fill.color = World.withAlpha(world.accent, 22)
                var x = 0f
                while (x < W) { c.drawRect(x, 0f, x + sq, H, fill); x += sq * 2 }
                var y = 0f
                while (y < H) { c.drawRect(0f, y, W, y + sq, fill); y += sq * 2 }
                hill(c, W, H, 0.9f, W, World.withAlpha(world.inColor, 90))
            }
            Grounds.STICKERS -> {
                val cs = colors()
                repeat(16) { i ->
                    val x = rnd.nextFloat() * W; val y = rnd.nextFloat() * H; val s = (14 + rnd.nextFloat() * 12) * d
                    c.save(); c.rotate(rnd.nextFloat() * 50f - 25f, x, y)
                    fill.color = Color.WHITE
                    when (i % 4) {
                        0 -> { c.drawCircle(x, y, s * 1.15f, fill); fill.color = World.withAlpha(cs[i % 4], 150); star5(c, x, y, s) }
                        1 -> { heartPath(x, y, s * 1.2f); c.drawPath(path, fill); fill.color = World.withAlpha(cs[i % 4], 150); heartPath(x, y, s); c.drawPath(path, fill) }
                        2 -> { c.drawCircle(x, y, s * 1.1f, fill); fill.color = World.withAlpha(Color.parseColor("#ffd23f"), 170); c.drawCircle(x, y, s, fill)
                               fill.color = World.withAlpha(world.fg, 170); c.drawCircle(x - s * 0.35f, y - s * 0.2f, s * 0.12f, fill); c.drawCircle(x + s * 0.35f, y - s * 0.2f, s * 0.12f, fill)
                               line.color = fill.color; line.strokeWidth = s * 0.1f; rect.set(x - s * 0.5f, y - s * 0.4f, x + s * 0.5f, y + s * 0.5f); c.drawArc(rect, 30f, 120f, false, line) }
                        else -> { rect.set(x - s * 1.2f, y - s * 0.8f, x + s * 1.2f, y + s * 0.8f); c.drawRoundRect(rect, s * 0.3f, s * 0.3f, fill); fill.color = World.withAlpha(cs[i % 4], 140); rect.inset(s * 0.15f, s * 0.15f); c.drawRoundRect(rect, s * 0.25f, s * 0.25f, fill) }
                    }
                    c.restore()
                }
            }
            Grounds.TORN -> {
                for (k in 0 until 2) {
                    fill.color = if (k == 0) World.withAlpha(Color.parseColor("#e8e0d0"), 30) else World.withAlpha(world.accent, 35)
                    path.reset(); val top = if (k == 0) H * 0.08f else H * 0.82f
                    path.moveTo(0f, if (k == 0) 0f else H)
                    var x = 0f
                    while (x <= W) { path.lineTo(x, top + (rnd.nextFloat() - 0.5f) * 14 * d); x += 9 * d }
                    path.lineTo(W, if (k == 0) 0f else H); path.close(); c.drawPath(path, fill)
                }
                line.color = World.withAlpha(world.accent, 40); line.strokeWidth = 3 * d
                var y = H * 0.84f
                while (y < H) { c.drawLine(0f, y, W, y, line); y += 18 * d }
                var x = 0f
                while (x < W) { c.drawLine(x, H * 0.82f, x, H, line); x += 18 * d }
            }
            Grounds.CHECKER -> {
                val sq = W / 10
                for (r in 0 until 3) for (col in 0 until 10) if ((r + col) % 2 == 0) {
                    fill.color = World.withAlpha(world.fg, 50); c.drawRect(col * sq, H - (r + 1) * sq, (col + 1) * sq, H - r * sq, fill)
                }
                fill.color = World.withAlpha(Color.parseColor("#ffd23f"), 120); c.drawCircle(W * 0.82f, H * 0.12f, 26 * d, fill)
            }
            Grounds.XMAS -> {
                hill(c, W, H, 0.88f, W, Color.WHITE); hill(c, W, H, 0.92f, W, World.withAlpha(Color.parseColor("#e6eff6"), 255))
                for ((fx, s) in listOf(0.16f to 1f, 0.84f to 0.75f)) {
                    val x = W * fx; val y = H * 0.9f; val h = H * 0.2f * s
                    fill.color = World.withAlpha(world.inColor, 230)
                    for (k in 0 until 3) { path.reset(); val ty = y - h + k * h * 0.28f; path.moveTo(x, ty); path.lineTo(x - h * (0.22f + k * 0.08f), ty + h * 0.38f); path.lineTo(x + h * (0.22f + k * 0.08f), ty + h * 0.38f); path.close(); c.drawPath(path, fill) }
                    fill.color = Color.parseColor("#ffd23f"); star5(c, x, y - h, h * 0.07f)
                    val bulbs = intArrayOf(world.accent, Color.parseColor("#ffd23f"), Color.parseColor("#4dabf7"))
                    for (k in 0 until 7) { fill.color = bulbs[k % 3]; c.drawCircle(x + (k - 3) * h * 0.07f, y - h * 0.35f + kotlin.math.abs(k - 3) * h * 0.04f, 3 * d, fill) }
                }
                line.color = World.withAlpha(world.fg, 80); line.strokeWidth = 1 * d
                path.reset(); path.moveTo(0f, H * 0.05f); path.quadTo(W * 0.5f, H * 0.11f, W, H * 0.05f); c.drawPath(path, line)
                val bulbs = intArrayOf(world.accent, Color.parseColor("#ffd23f"), world.inColor, Color.parseColor("#4dabf7"))
                for (k in 1 until 12) { val x = W * k / 12; val y = H * 0.05f + (1 - ((x / W - 0.5f) * 2).let { it * it }) * H * 0.045f; fill.color = bulbs[k % 4]; c.drawCircle(x, y + 5 * d, 4.5f * d, fill) }
            }
            Grounds.SUNSET -> {
                val sunC = World.withAlpha(world.accent, 90)
                fill.color = sunC; c.drawCircle(W * 0.5f, H * 0.9f, W * 0.32f, fill)
                fill.color = World.withAlpha(world.base, 200)
                for (k in 0 until 5) c.drawRect(0f, H * 0.82f + k * 9 * d, W, H * 0.82f + k * 9 * d + (2 + k) * d, fill)
                for (k in 0 until 3) {
                    fill.color = World.withAlpha(colors()[k], 70)
                    path.reset(); path.moveTo(0f, H)
                    var x = 0f
                    while (x <= W) { path.lineTo(x, H * (0.9f + k * 0.03f) + sin(x / (60 * d) + k * 1.5f) * 12 * d); x += 8 * d }
                    path.lineTo(W, H); path.close(); c.drawPath(path, fill)
                }
            }
        }
    }

    private fun acacia(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        line.color = col; line.strokeWidth = s * 0.06f
        c.drawLine(x, y, x, y - s * 0.7f, line); c.drawLine(x, y - s * 0.5f, x - s * 0.3f, y - s * 0.8f, line); c.drawLine(x, y - s * 0.55f, x + s * 0.3f, y - s * 0.82f, line)
        fill.color = col; rect.set(x - s * 0.7f, y - s * 1.0f, x + s * 0.7f, y - s * 0.72f); c.drawOval(rect, fill)
    }

    private fun star5(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        for (k in 0 until 10) { val rr = if (k % 2 == 0) s else s * 0.45f; val an = -PI / 2 + k * PI / 5; val px = x + (rr * cos(an)).toFloat(); val py = y + (rr * sin(an)).toFloat(); if (k == 0) path.moveTo(px, py) else path.lineTo(px, py) }
        path.close(); c.drawPath(path, fill)
    }

    private fun heartPath(x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y + s * 0.8f)
        path.cubicTo(x - s * 1.3f, y - s * 0.1f, x - s * 0.6f, y - s * 1.1f, x, y - s * 0.35f)
        path.cubicTo(x + s * 0.6f, y - s * 1.1f, x + s * 1.3f, y - s * 0.1f, x, y + s * 0.8f); path.close()
    }

    private fun hill(c: Canvas, W: Float, H: Float, top: Float, span: Float, col: Int) {
        fill.color = col
        path.reset(); path.moveTo(0f, H); path.lineTo(0f, H * top)
        path.cubicTo(W * 0.3f, H * (top - 0.05f), W * 0.6f, H * (top + 0.03f), W, H * (top - 0.02f))
        path.lineTo(W, H); path.close(); c.drawPath(path, fill)
    }

    private fun mushroom(c: Canvas, x: Float, y: Float, s: Float) {
        fill.color = World.mix(world.base, Color.WHITE, 0.5f)
        c.drawRect(x - s * 0.18f, y - s * 0.6f, x + s * 0.18f, y + 4 * d, fill)
        fill.color = world.accent
        rect.set(x - s * 0.6f, y - s * 1.05f, x + s * 0.6f, y - s * 0.35f); c.drawArc(rect, 180f, 180f, true, fill)
        fill.color = Color.WHITE
        c.drawCircle(x - s * 0.22f, y - s * 0.82f, s * 0.08f, fill); c.drawCircle(x + s * 0.2f, y - s * 0.9f, s * 0.06f, fill)
    }

    private fun palm(c: Canvas, x: Float, y: Float, h: Float, col: Int) {
        line.color = col; line.strokeWidth = 7 * d
        path.reset(); path.moveTo(x, y); path.quadTo(x + h * 0.15f, y - h * 0.5f, x + h * 0.05f, y - h); c.drawPath(path, line)
        fill.color = col
        for (k in 0 until 6) {
            val a = -PI / 2 + (k - 2.5) * 0.5
            c.save(); c.rotate(Math.toDegrees(a).toFloat(), x + h * 0.05f, y - h)
            rect.set(x + h * 0.05f, y - h - 7 * d, x + h * 0.05f + h * 0.45f, y - h + 7 * d); c.drawOval(rect, fill); c.restore()
        }
    }

    // ───────── moving bits ─────────

    private fun drawParticle(c: Canvas, p: P) {
        val tw = (sin(t * 1.6f + p.phase) + 1) / 2
        val s = p.size
        when (world.particle) {
            Particles.STARS -> { fill.color = World.withAlpha(if (world.dark) Color.WHITE else p.color, (50 + 170 * tw).toInt()); c.drawCircle(p.x, p.y, s * 0.25f, fill) }
            Particles.DOTS -> { fill.color = World.withAlpha(p.color, (40 + 140 * tw).toInt()); c.drawCircle(p.x, p.y, s * 0.22f, fill) }
            Particles.SPARKLES -> { fill.color = World.withAlpha(p.color, (60 + 190 * tw).toInt()); sparkle(c, p.x, p.y, s * (0.5f + 0.5f * tw)) }
            Particles.EMBERS -> { fill.color = World.withAlpha(p.color, (70 + 150 * tw).toInt()); c.drawCircle(p.x, p.y, s * 0.3f, fill) }
            Particles.BUBBLES -> {
                line.color = World.withAlpha(p.color, 150); line.strokeWidth = 1.3f * d; c.drawCircle(p.x, p.y, s, line)
                fill.color = World.withAlpha(Color.WHITE, 180); c.drawCircle(p.x - s * 0.35f, p.y - s * 0.35f, s * 0.2f, fill)
            }
            Particles.HEARTS -> { fill.color = World.withAlpha(p.color, 150); heart(c, p.x, p.y, s) }
            Particles.PETALS -> {
                fill.color = World.withAlpha(p.color, 150)
                c.save(); c.rotate(t * 40 + p.phase * 50, p.x, p.y); rect.set(p.x - s, p.y - s * 0.5f, p.x + s, p.y + s * 0.5f); c.drawOval(rect, fill); c.restore()
            }
            Particles.LEAVES -> {
                fill.color = World.withAlpha(p.color, 140)
                c.save(); c.rotate(t * 30 + p.phase * 60, p.x, p.y)
                path.reset(); path.moveTo(p.x - s, p.y); path.quadTo(p.x, p.y - s * 0.9f, p.x + s, p.y); path.quadTo(p.x, p.y + s * 0.9f, p.x - s, p.y); c.drawPath(path, fill)
                c.restore()
            }
            Particles.CONFETTI -> {
                fill.color = World.withAlpha(p.color, 190)
                c.save(); c.rotate(t * 90 + p.phase * 60, p.x, p.y); c.drawRect(p.x - s * 0.6f, p.y - s * 0.25f, p.x + s * 0.6f, p.y + s * 0.25f, fill); c.restore()
            }
            Particles.DAISIES -> {
                fill.color = World.withAlpha(Color.WHITE, 200)
                for (k in 0 until 6) { val a = k * PI / 3 + t; c.drawCircle(p.x + (s * 0.7f * cos(a)).toFloat(), p.y + (s * 0.7f * sin(a)).toFloat(), s * 0.42f, fill) }
                fill.color = World.withAlpha(Color.parseColor("#ffc93c"), 230); c.drawCircle(p.x, p.y, s * 0.4f, fill)
            }
            Particles.BUTTERFLIES -> {
                val flap = 0.4f + 0.6f * kotlin.math.abs(sin(t * 4 + p.phase))
                fill.color = World.withAlpha(p.color, 170)
                rect.set(p.x - s * flap, p.y - s * 0.7f, p.x, p.y + s * 0.2f); c.drawOval(rect, fill)
                rect.set(p.x, p.y - s * 0.7f, p.x + s * flap, p.y + s * 0.2f); c.drawOval(rect, fill)
                rect.set(p.x - s * 0.6f * flap, p.y, p.x, p.y + s * 0.6f); c.drawOval(rect, fill)
                rect.set(p.x, p.y, p.x + s * 0.6f * flap, p.y + s * 0.6f); c.drawOval(rect, fill)
            }
            Particles.TICKERS -> {
                val up = p.phase > 3.14f
                fill.color = World.withAlpha(if (up) world.inColor else world.outColor, 120)
                c.drawRect(p.x - s * 0.3f, p.y - s * 1.4f, p.x + s * 0.3f, p.y + s * 1.4f, fill)
                line.color = fill.color; line.strokeWidth = 1.2f * d; c.drawLine(p.x, p.y - s * 2.1f, p.x, p.y + s * 2.1f, line)
            }
            Particles.SNOW -> { fill.color = World.withAlpha(Color.WHITE, 230); c.drawCircle(p.x, p.y, s * 0.32f, fill); fill.color = World.withAlpha(Color.parseColor("#b8c9d9"), 120); c.drawCircle(p.x, p.y, s * 0.12f, fill) }
            Particles.PAWS -> {
                fill.color = World.withAlpha(p.color, 70)
                rect.set(p.x - s * 0.5f, p.y - s * 0.1f, p.x + s * 0.5f, p.y + s * 0.6f); c.drawOval(rect, fill)
                for (k in -1..1) c.drawCircle(p.x + k * s * 0.42f, p.y - s * 0.4f + kotlin.math.abs(k) * s * 0.18f, s * 0.2f, fill)
            }
            Particles.LEMONS -> {
                c.save(); c.rotate(t * 25 + p.phase * 50, p.x, p.y)
                fill.color = World.withAlpha(Color.parseColor("#ffd93b"), 200); rect.set(p.x - s * 0.8f, p.y - s * 0.55f, p.x + s * 0.8f, p.y + s * 0.55f); c.drawOval(rect, fill)
                fill.color = World.withAlpha(Color.parseColor("#5d9b4a"), 200); rect.set(p.x + s * 0.4f, p.y - s * 0.95f, p.x + s * 1.1f, p.y - s * 0.55f); c.drawOval(rect, fill)
                c.restore()
            }
            Particles.TENNIS -> {
                fill.color = World.withAlpha(Color.parseColor("#d7e84a"), 210); c.drawCircle(p.x, p.y, s * 0.6f, fill)
                line.color = World.withAlpha(Color.WHITE, 230); line.strokeWidth = s * 0.1f
                rect.set(p.x - s * 0.9f, p.y - s * 0.6f, p.x - s * 0.1f, p.y + s * 0.6f); c.drawArc(rect, -60f, 120f, false, line)
                rect.set(p.x + s * 0.1f, p.y - s * 0.6f, p.x + s * 0.9f, p.y + s * 0.6f); c.drawArc(rect, 120f, 120f, false, line)
            }
            Particles.SMILEYS -> {
                fill.color = World.withAlpha(Color.parseColor("#ffd23f"), 150); c.drawCircle(p.x, p.y, s * 0.7f, fill)
                fill.color = World.withAlpha(world.fg, 150); c.drawCircle(p.x - s * 0.25f, p.y - s * 0.15f, s * 0.08f, fill); c.drawCircle(p.x + s * 0.25f, p.y - s * 0.15f, s * 0.08f, fill)
                line.color = fill.color; line.strokeWidth = s * 0.08f; rect.set(p.x - s * 0.35f, p.y - s * 0.25f, p.x + s * 0.35f, p.y + s * 0.38f); c.drawArc(rect, 30f, 120f, false, line)
            }
            Particles.NOTES -> { fill.color = World.withAlpha(p.color, 160); c.drawCircle(p.x, p.y, s * 0.4f, fill); line.color = fill.color; line.strokeWidth = 1.5f * d; c.drawLine(p.x + s * 0.4f, p.y, p.x + s * 0.4f, p.y - s * 1.4f, line) }
        }
    }

    private fun sparkle(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y - s); path.quadTo(x, y, x + s, y); path.quadTo(x, y, x, y + s); path.quadTo(x, y, x - s, y); path.quadTo(x, y, x, y - s); path.close()
        c.drawPath(path, fill)
    }

    private fun heart(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        path.moveTo(x, y + s * 0.8f)
        path.cubicTo(x - s * 1.3f, y - s * 0.1f, x - s * 0.6f, y - s * 1.1f, x, y - s * 0.35f)
        path.cubicTo(x + s * 0.6f, y - s * 1.1f, x + s * 1.3f, y - s * 0.1f, x, y + s * 0.8f)
        path.close(); c.drawPath(path, fill)
    }
}
