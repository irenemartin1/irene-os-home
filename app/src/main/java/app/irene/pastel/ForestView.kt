package app.irene.pastel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Irene OS fairy-forest background:
 * night sky gradient → twinkling stars → drifting purple mist →
 * tree + mushroom silhouettes → fireflies floating up.
 * Runs ~20fps and stops when you leave home, so it's light on battery.
 */
class ForestView(context: Context) : View(context) {

    var palette: Palette = Palettes.all[0]
        set(value) {
            field = value
            glowPaint.colorFilter = PorterDuffColorFilter(value.glow, PorterDuff.Mode.SRC_IN)
            invalidate()
        }

    private val skyPaint = Paint()
    private val mistPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var t = 0f
    private var running = false
    private val rnd = Random(13)

    private val backTrees = Path()
    private val frontGround = Path()
    private val mushroomCaps = mutableListOf<RectF>()
    private val mushroomSpots = mutableListOf<FloatArray>() // x, y, r

    private class Fly(var x: Float, var y: Float, val speed: Float, val phase: Float, val size: Float)
    private val flies = mutableListOf<Fly>()
    private val stars = mutableListOf<FloatArray>() // x, y, r, phase

    private val glowBitmap: Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also { bmp ->
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(32f, 32f, 32f, intArrayOf(0xFFFFFFFF.toInt(), 0x55FFFFFF, 0x00FFFFFF), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP)
        Canvas(bmp).drawCircle(32f, 32f, 32f, p)
    }
    private val glowRect = RectF()

    private val mistSpecs = arrayOf( // x%, y%, radius(%w), speed
        floatArrayOf(0.15f, 0.30f, 0.70f, 1.0f),
        floatArrayOf(0.90f, 0.55f, 0.75f, 1.3f),
        floatArrayOf(0.40f, 0.82f, 0.80f, 0.8f),
    )

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            step()
            invalidate()
            postDelayed(this, 50)
        }
    }

    init { palette = Palettes.all[0] }

    fun start() { if (!running) { running = true; post(frame) } }
    fun stop() { running = false; removeCallbacks(frame) }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        buildScene(w.toFloat(), h.toFloat())
    }

    private fun buildScene(w: Float, h: Float) {
        // stars in the upper sky
        stars.clear()
        repeat(45) {
            stars += floatArrayOf(rnd.nextFloat() * w, rnd.nextFloat() * h * 0.45f, 0.8f + rnd.nextFloat() * 1.8f, rnd.nextFloat() * 6.28f)
        }
        // fireflies
        flies.clear()
        repeat(26) {
            flies += Fly(rnd.nextFloat() * w, h * (0.35f + rnd.nextFloat() * 0.65f),
                0.4f + rnd.nextFloat() * 1.1f, rnd.nextFloat() * 6.28f, 10f + rnd.nextFloat() * 18f)
        }

        // back layer: round fairy-tree crowns hugging the left + right edges
        backTrees.reset()
        val crowns = listOf(
            floatArrayOf(-0.05f, 0.62f, 0.28f), floatArrayOf(0.10f, 0.72f, 0.22f), floatArrayOf(-0.02f, 0.80f, 0.25f),
            floatArrayOf(1.05f, 0.58f, 0.30f), floatArrayOf(0.90f, 0.70f, 0.22f), floatArrayOf(1.00f, 0.82f, 0.26f),
            floatArrayOf(0.30f, 0.88f, 0.20f), floatArrayOf(0.70f, 0.87f, 0.21f),
        )
        crowns.forEach { (cx, cy, r) -> backTrees.addCircle(cx * w, cy * h, r * w, Path.Direction.CW) }
        backTrees.addRect(-10f, h * 0.85f, w + 10f, h, Path.Direction.CW)
        // trunks
        backTrees.addRect(w * 0.06f, h * 0.70f, w * 0.10f, h, Path.Direction.CW)
        backTrees.addRect(w * 0.88f, h * 0.66f, w * 0.92f, h, Path.Direction.CW)

        // front layer: rolling hill + grass blades
        frontGround.reset()
        val base = h * 0.90f
        frontGround.moveTo(0f, h)
        frontGround.lineTo(0f, base)
        frontGround.cubicTo(w * 0.25f, base - h * 0.04f, w * 0.55f, base + h * 0.03f, w, base - h * 0.02f)
        frontGround.lineTo(w, h)
        frontGround.close()
        var x = 0f
        while (x < w) {
            val bh = h * (0.012f + rnd.nextFloat() * 0.025f)
            val y = groundY(x, w, h)
            frontGround.moveTo(x, y + 4f)
            frontGround.quadTo(x + 4f, y - bh * 0.5f, x + 6f + rnd.nextFloat() * 6f, y - bh)
            frontGround.lineTo(x + 6f, y + 4f)
            frontGround.close()
            x += 9f + rnd.nextFloat() * 22f
        }

        // mushrooms: (x%, size)
        mushroomCaps.clear(); mushroomSpots.clear()
        listOf(floatArrayOf(0.14f, 1.0f), floatArrayOf(0.22f, 0.6f), floatArrayOf(0.80f, 0.85f), floatArrayOf(0.87f, 0.5f)).forEach { (mx, s) ->
            val cx = mx * w
            val gy = groundY(cx, w, h)
            val capW = w * 0.11f * s
            val stemH = h * 0.035f * s
            frontGround.addRoundRect(RectF(cx - capW * 0.16f, gy - stemH, cx + capW * 0.16f, gy + 6f), 8f, 8f, Path.Direction.CW)
            val cap = RectF(cx - capW / 2, gy - stemH - capW * 0.45f, cx + capW / 2, gy - stemH + capW * 0.25f)
            mushroomCaps += cap
            mushroomSpots += floatArrayOf(cx - capW * 0.18f, cap.top + capW * 0.17f, capW * 0.06f)
            mushroomSpots += floatArrayOf(cx + capW * 0.15f, cap.top + capW * 0.12f, capW * 0.045f)
            mushroomSpots += floatArrayOf(cx + capW * 0.02f, cap.top + capW * 0.28f, capW * 0.035f)
        }
    }

    private fun groundY(x: Float, w: Float, h: Float): Float {
        val u = x / w
        val base = h * 0.90f
        return base - h * 0.04f * sin(u * 3.14f) * 0.6f + h * 0.01f * sin(u * 9f)
    }

    private fun step() {
        t += 0.03f
        val h = height.toFloat()
        val w = width.toFloat()
        for (f in flies) {
            f.y -= f.speed
            f.x += sin(t + f.phase) * 0.6f
            if (f.y < h * 0.18f) { f.y = h * (0.85f + rnd.nextFloat() * 0.1f); f.x = rnd.nextFloat() * w }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val p = palette

        skyPaint.shader = LinearGradient(0f, 0f, 0f, h, p.top, p.bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, skyPaint)

        // stars
        starPaint.color = p.glow
        for (s in stars) {
            starPaint.alpha = (60 + 140 * (sin(t * 1.5f + s[3]) + 1) / 2).toInt()
            canvas.drawCircle(s[0], s[1], s[2], starPaint)
        }

        // mist
        for (i in mistSpecs.indices) {
            val m = mistSpecs[i]
            val cx = w * m[0] + sin(t * 0.25f * m[3] + i) * w * 0.10f
            val cy = h * m[1] + cos(t * 0.25f * m[3] + i * 2) * h * 0.04f
            val r = w * m[2]
            mistPaint.shader = RadialGradient(cx, cy, r, intArrayOf(alpha(p.mist[i], 120), alpha(p.mist[i], 0)), null, Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, r, mistPaint)
        }

        // back trees (slightly see-through so mist shows behind)
        groundPaint.color = alpha(p.ground, 200)
        canvas.drawPath(backTrees, groundPaint)

        // front ground + stems
        groundPaint.color = p.ground
        canvas.drawPath(frontGround, groundPaint)
        for (cap in mushroomCaps) canvas.drawArc(cap, 180f, 180f, true, groundPaint)

        // glowing mushroom spots
        dotPaint.color = p.glow
        for (s in mushroomSpots) {
            dotPaint.alpha = (150 + 80 * sin(t + s[0])).toInt().coerceIn(0, 255)
            canvas.drawCircle(s[0], s[1], s[2], dotPaint)
        }

        // fireflies
        for (f in flies) {
            val tw = (sin(t * 2f + f.phase) + 1) / 2
            glowPaint.alpha = (70 + 185 * tw).toInt()
            val r = f.size * (0.8f + 0.4f * tw)
            glowRect.set(f.x - r, f.y - r, f.x + r, f.y + r)
            canvas.drawBitmap(glowBitmap, null, glowRect, glowPaint)
        }
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)
}
