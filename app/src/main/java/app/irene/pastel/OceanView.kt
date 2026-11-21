package app.irene.pastel

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Underwater whimsy: deep-water gradient, swaying light rays, flowing water currents,
 * kelp + sand, cute fish, pulsing jellyfish and rising bubbles.
 * burst() = the big bubble explosion when you shake into underwater mode.
 */
class OceanView(context: Context) : View(context) {

    var palette: Palette = Palettes.ocean[0]
        set(value) { field = value; invalidate() }

    private val d = resources.displayMetrics.density
    private val rnd = Random(7)
    private var t = 0f
    private var running = false

    private val bgPaint = Paint()
    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val kelpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private class Bubble(var x: Float, var y: Float, val r: Float, val speed: Float, val phase: Float)
    private class Fish(var x: Float, var y: Float, val size: Float, var dir: Int, var speed: Float, val color: Int, val phase: Float)
    private class Jelly(var x: Float, var y: Float, val size: Float, val phase: Float, val color: Int)

    private val bubbles = mutableListOf<Bubble>()
    private val fish = mutableListOf<Fish>()
    private val jellies = mutableListOf<Jelly>()
    private val kelp = mutableListOf<FloatArray>() // x, height, phase, thickness
    private val sand = Path()

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            step()
            invalidate()
            postDelayed(this, 40) // ~25fps, a little smoother for swimming
        }
    }

    fun start() { if (!running) { running = true; post(frame) } }
    fun stop() { running = false; removeCallbacks(frame) }

    /** Big bubble explosion from the bottom of the screen. */
    fun burst() {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f) return
        repeat(80) {
            bubbles += Bubble(rnd.nextFloat() * w, h + rnd.nextFloat() * h * 0.4f,
                (3 + rnd.nextFloat() * 14) * d, (4 + rnd.nextFloat() * 9) * d / 2.5f, rnd.nextFloat() * 6.28f)
        }
        fish.forEach { it.speed *= 3f } // fish get startled and zoom for a sec
        postDelayed({ fish.forEach { f -> f.speed /= 3f } }, 1200)
    }

    /** A few bubbles where you tap. */
    fun puff(x: Float, y: Float) {
        repeat(7) {
            bubbles += Bubble(x + (rnd.nextFloat() - 0.5f) * 30 * d, y, (2 + rnd.nextFloat() * 7) * d,
                (1.5f + rnd.nextFloat() * 2.5f) * d / 2.5f, rnd.nextFloat() * 6.28f)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val W = w.toFloat(); val H = h.toFloat()
        fish.clear(); jellies.clear(); kelp.clear()
        repeat(7) { i ->
            fish += Fish(rnd.nextFloat() * W, H * (0.15f + rnd.nextFloat() * 0.6f), (10 + rnd.nextFloat() * 16) * d,
                if (rnd.nextBoolean()) 1 else -1, (0.6f + rnd.nextFloat() * 1.4f) * d / 2.5f, i % 3, rnd.nextFloat() * 6.28f)
        }
        repeat(3) { i ->
            jellies += Jelly(W * (0.2f + i * 0.3f), H * (0.3f + rnd.nextFloat() * 0.5f), (22 + rnd.nextFloat() * 14) * d,
                rnd.nextFloat() * 6.28f, i % 3)
        }
        var x = -10f
        while (x < W + 10) {
            kelp += floatArrayOf(x, H * (0.12f + rnd.nextFloat() * 0.22f), rnd.nextFloat() * 6.28f, (5 + rnd.nextFloat() * 5) * d)
            x += (26 + rnd.nextFloat() * 50) * d
        }
        sand.reset()
        sand.moveTo(0f, H)
        sand.lineTo(0f, H * 0.93f)
        sand.cubicTo(W * 0.3f, H * 0.89f, W * 0.6f, H * 0.96f, W, H * 0.91f)
        sand.lineTo(W, H)
        sand.close()
        // little rocks
        sand.addCircle(W * 0.18f, H * 0.93f, 18 * d, Path.Direction.CW)
        sand.addCircle(W * 0.24f, H * 0.94f, 11 * d, Path.Direction.CW)
        sand.addCircle(W * 0.78f, H * 0.925f, 22 * d, Path.Direction.CW)
    }

    private fun step() {
        t += 0.04f
        val w = width.toFloat(); val h = height.toFloat()

        // bubbles rise + wobble
        val iter = bubbles.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.y -= b.speed * (1 + b.r / (20 * d))
            b.x += sin(t * 2 + b.phase) * 0.7f
            if (b.y < -b.r * 2) iter.remove()
        }
        // gentle ambient stream
        if (kelp.isNotEmpty() && bubbles.size < 22 && rnd.nextFloat() < 0.2f) {
            val k = kelp.random(rnd)
            bubbles += Bubble(k[0] + (rnd.nextFloat() - 0.5f) * 20 * d, h * 0.92f, (2 + rnd.nextFloat() * 6) * d,
                (1 + rnd.nextFloat() * 2) * d / 2.5f, rnd.nextFloat() * 6.28f)
        }

        for (f in fish) {
            f.x += f.dir * f.speed
            f.y += sin(t + f.phase) * 0.5f
            if ((f.dir > 0 && f.x > w + f.size * 3) || (f.dir < 0 && f.x < -f.size * 3)) {
                f.dir = if (rnd.nextBoolean()) 1 else -1
                f.x = if (f.dir > 0) -f.size * 2 else w + f.size * 2
                f.y = h * (0.15f + rnd.nextFloat() * 0.6f)
            }
        }
        for (j in jellies) {
            j.y -= (0.25f + 1.2f * max(0f, sin(t * 1.4f + j.phase))) * d / 2.5f // pulse-swim upward
            j.x += sin(t * 0.4f + j.phase) * 0.4f
            if (j.y < -j.size * 3) { j.y = h + j.size * 2; j.x = rnd.nextFloat() * w }
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val p = palette

        bgPaint.shader = LinearGradient(0f, 0f, 0f, h, p.top, p.bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, bgPaint)

        drawRays(c, w, h, p)
        drawCurrents(c, w, h, p)
        drawKelp(c, h, p)
        fill.shader = null; fill.color = p.ground
        c.drawPath(sand, fill)
        for (j in jellies) drawJelly(c, j, p)
        for (f in fish) drawFish(c, f, p)
        drawBubbles(c, p)
    }

    // light shafts from the surface, swaying
    private fun drawRays(c: Canvas, w: Float, h: Float, p: Palette) {
        rayPaint.shader = LinearGradient(0f, 0f, 0f, h * 0.75f, alpha(p.glow, 46), alpha(p.glow, 0), Shader.TileMode.CLAMP)
        for (i in 0 until 5) {
            val top = w * (0.1f + i * 0.2f) + sin(t * 0.3f + i) * 20 * d
            val spread = (30 + i % 3 * 22) * d
            val drift = sin(t * 0.25f + i * 1.7f) * 50 * d
            path.reset()
            path.moveTo(top - 10 * d, 0f)
            path.lineTo(top + 10 * d, 0f)
            path.lineTo(top + spread + drift, h * 0.75f)
            path.lineTo(top - spread + drift, h * 0.75f)
            path.close()
            c.drawPath(path, rayPaint)
        }
    }

    // flowing water: soft sine currents sliding sideways + a shimmering surface line
    private fun drawCurrents(c: Canvas, w: Float, h: Float, p: Palette) {
        wavePaint.color = p.glow
        for (k in 0 until 7) {
            val baseY = h * (0.06f + k * 0.13f)
            val amp = (4 + k % 3 * 3) * d
            val len = (120 + k * 15) * d
            val speed = if (k % 2 == 0) 1.6f else -1.1f
            wavePaint.strokeWidth = (1.2f + (k % 2)) * d
            wavePaint.alpha = if (k == 0) 70 else 22
            path.reset()
            var x = -20f
            path.moveTo(x, baseY)
            while (x <= w + 20) {
                path.lineTo(x, baseY + sin((x / len) * 6.28f + t * speed + k) * amp)
                x += 8 * d
            }
            c.drawPath(path, wavePaint)
        }
    }

    private fun drawKelp(c: Canvas, h: Float, p: Palette) {
        kelpPaint.color = alpha(p.ground, 235)
        for (k in kelp) {
            kelpPaint.strokeWidth = k[3]
            path.reset()
            val segs = 10
            path.moveTo(k[0], h)
            for (s in 1..segs) {
                val y = h - k[1] * s / segs
                val x = k[0] + sin(t * 1.1f + s * 0.45f + k[2]) * s * 2.2f * d
                path.lineTo(x, y)
            }
            c.drawPath(path, kelpPaint)
        }
    }

    private fun drawFish(c: Canvas, f: Fish, p: Palette) {
        val s = f.size
        val col = p.mist[f.color]
        c.save()
        c.translate(f.x, f.y)
        c.scale(f.dir.toFloat(), 1f)
        // tail (wiggles)
        val wig = sin(t * 8 + f.phase) * s * 0.18f
        fill.shader = null; fill.color = alpha(col, 220)
        path.reset()
        path.moveTo(-s * 0.7f, 0f)
        path.lineTo(-s * 1.55f, -s * 0.6f + wig)
        path.quadTo(-s * 1.3f, wig, -s * 1.55f, s * 0.6f + wig)
        path.close()
        c.drawPath(path, fill)
        // body
        fill.color = col
        rect.set(-s, -s * 0.62f, s, s * 0.62f)
        c.drawOval(rect, fill)
        // top fin
        path.reset()
        path.moveTo(-s * 0.3f, -s * 0.5f)
        path.quadTo(0f, -s * 1.05f, s * 0.35f, -s * 0.5f)
        path.close()
        c.drawPath(path, fill)
        // belly shine
        fill.color = alpha(0xFFFFFF, 60)
        rect.set(-s * 0.5f, s * 0.05f, s * 0.6f, s * 0.45f)
        c.drawOval(rect, fill)
        // eye + sparkle
        fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(s * 0.45f, -s * 0.12f, s * 0.2f, fill)
        fill.color = 0xFF1A1030.toInt(); c.drawCircle(s * 0.5f, -s * 0.1f, s * 0.12f, fill)
        fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(s * 0.54f, -s * 0.15f, s * 0.04f, fill)
        // blush
        fill.color = alpha(0xFF7FB0, 110); c.drawCircle(s * 0.3f, s * 0.18f, s * 0.11f, fill)
        c.restore()
    }

    private fun drawJelly(c: Canvas, j: Jelly, p: Palette) {
        val col = p.mist[j.color]
        val pulse = 1f + 0.12f * sin(t * 1.4f + j.phase + 3.14f)
        val s = j.size
        // soft glow
        glowPaint.shader = RadialGradient(j.x, j.y, s * 2.2f, intArrayOf(alpha(col, 70), alpha(col, 0)), null, Shader.TileMode.CLAMP)
        c.drawCircle(j.x, j.y, s * 2.2f, glowPaint)
        // tentacles
        stroke.color = alpha(col, 150); stroke.strokeWidth = 2.2f * d; stroke.strokeCap = Paint.Cap.ROUND
        for (k in 0 until 5) {
            val tx = j.x - s * 0.6f + k * s * 0.3f
            path.reset()
            path.moveTo(tx, j.y)
            var y = j.y
            while (y < j.y + s * 1.9f) {
                y += 4 * d
                path.lineTo(tx + sin(y * 0.08f / d + t * 3 + k) * 4 * d, y)
            }
            c.drawPath(path, stroke)
        }
        // dome
        fill.shader = null; fill.color = alpha(col, 200)
        rect.set(j.x - s * pulse, j.y - s * 0.9f, j.x + s * pulse, j.y + s * 0.5f)
        c.drawArc(rect, 180f, 180f, true, fill)
        // frilly rim
        fill.color = alpha(col, 255)
        for (k in 0..6) c.drawCircle(j.x - s * pulse + k * (2 * s * pulse / 6), j.y - 0.2f * d, s * 0.13f, fill)
        // face
        fill.color = 0xFF1A1030.toInt()
        c.drawCircle(j.x - s * 0.28f, j.y - s * 0.3f, s * 0.07f, fill)
        c.drawCircle(j.x + s * 0.28f, j.y - s * 0.3f, s * 0.07f, fill)
        fill.color = alpha(0xFF7FB0, 140)
        c.drawCircle(j.x - s * 0.45f, j.y - s * 0.16f, s * 0.08f, fill)
        c.drawCircle(j.x + s * 0.45f, j.y - s * 0.16f, s * 0.08f, fill)
        // shine
        fill.color = alpha(0xFFFFFF, 90)
        rect.set(j.x - s * 0.6f, j.y - s * 0.8f, j.x - s * 0.15f, j.y - s * 0.5f)
        c.drawOval(rect, fill)
    }

    private fun drawBubbles(c: Canvas, p: Palette) {
        stroke.strokeWidth = 1.3f * d
        for (b in bubbles) {
            fill.shader = null
            fill.color = alpha(p.glow, 28); c.drawCircle(b.x, b.y, b.r, fill)
            stroke.color = alpha(p.glow, 170); c.drawCircle(b.x, b.y, b.r, stroke)
            fill.color = alpha(0xFFFFFF, 200); c.drawCircle(b.x - b.r * 0.35f, b.y - b.r * 0.35f, b.r * 0.22f, fill)
        }
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)
}
