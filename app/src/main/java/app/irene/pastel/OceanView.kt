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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Underwater whimsy. Back to front:
 * deep-water gradient → light rays → a gentle whale far away → flowing currents → glowing plankton →
 * kelp, coral + swaying anemones → sand with starfish & shells → a crab walking →
 * seahorses, jellyfish, an octopus, a sea turtle, fish & pufferfish → bubbles.
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
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    private class Bubble(var x: Float, var y: Float, val r: Float, val speed: Float, val phase: Float)
    private class Fish(var x: Float, var y: Float, val size: Float, var dir: Int, var speed: Float, val color: Int, val phase: Float, val kind: Int)
    private class Jelly(var x: Float, var y: Float, val size: Float, val phase: Float, val color: Int)
    private class Swimmer(var x: Float, var y: Float, val size: Float, var dir: Int, val speed: Float, val phase: Float)

    private val bubbles = mutableListOf<Bubble>()
    private val fish = mutableListOf<Fish>()
    private val jellies = mutableListOf<Jelly>()
    private val seahorses = mutableListOf<Swimmer>()
    private val plankton = mutableListOf<FloatArray>() // x, y, r, phase
    private val kelp = mutableListOf<FloatArray>()     // x, height, phase, thickness
    private val anemones = mutableListOf<FloatArray>() // x, size, colorIdx
    private val corals = mutableListOf<FloatArray>()   // x, size, colorIdx
    private val stars = mutableListOf<FloatArray>()    // x, size, rotation
    private val shells = mutableListOf<FloatArray>()   // x, size
    private var turtle = Swimmer(0f, 0f, 1f, 1, 1f, 0f)
    private var octo = Swimmer(0f, 0f, 1f, 1, 1f, 0f)
    private var whale = Swimmer(0f, 0f, 1f, 1, 1f, 0f)
    private var crab = Swimmer(0f, 0f, 1f, 1, 1f, 0f)
    private val sand = Path()

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            step()
            invalidate()
            postDelayed(this, 40) // ~25fps
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
        fish.clear(); jellies.clear(); kelp.clear(); seahorses.clear(); plankton.clear()
        anemones.clear(); corals.clear(); stars.clear(); shells.clear()

        repeat(8) { i ->
            fish += Fish(rnd.nextFloat() * W, H * (0.15f + rnd.nextFloat() * 0.6f), (10 + rnd.nextFloat() * 16) * d,
                if (rnd.nextBoolean()) 1 else -1, (0.6f + rnd.nextFloat() * 1.4f) * d / 2.5f, i % 3, rnd.nextFloat() * 6.28f,
                i % 4) // kind: 0 plain, 1 striped, 2 spotty, 3 pufferfish
        }
        repeat(3) { i ->
            jellies += Jelly(W * (0.2f + i * 0.3f), H * (0.3f + rnd.nextFloat() * 0.5f), (20 + rnd.nextFloat() * 12) * d,
                rnd.nextFloat() * 6.28f, i % 3)
        }
        seahorses += Swimmer(W * 0.12f, H * 0.66f, 22 * d, 1, 0f, 0.4f)
        seahorses += Swimmer(W * 0.86f, H * 0.58f, 18 * d, -1, 0f, 2.1f)
        turtle = Swimmer(-60 * d, H * 0.42f, 30 * d, 1, 0.55f * d / 2.5f, 1f)
        octo = Swimmer(W * 0.62f, H * 0.72f, 26 * d, 1, 0f, 0.7f)
        whale = Swimmer(W * 1.4f, H * 0.2f, W * 0.42f, -1, 0.18f * d / 2.5f, 0f)
        crab = Swimmer(W * 0.45f, H * 0.935f, 14 * d, 1, 0.5f * d / 2.5f, 0f)

        repeat(40) { plankton += floatArrayOf(rnd.nextFloat() * W, rnd.nextFloat() * H * 0.85f, (0.8f + rnd.nextFloat() * 1.6f) * d, rnd.nextFloat() * 6.28f) }

        var x = -10f
        while (x < W + 10) {
            kelp += floatArrayOf(x, H * (0.12f + rnd.nextFloat() * 0.22f), rnd.nextFloat() * 6.28f, (5 + rnd.nextFloat() * 5) * d)
            x += (34 + rnd.nextFloat() * 60) * d
        }
        corals += floatArrayOf(W * 0.30f, 34 * d, 0f); corals += floatArrayOf(W * 0.72f, 28 * d, 1f); corals += floatArrayOf(W * 0.95f, 24 * d, 2f)
        anemones += floatArrayOf(W * 0.06f, 20 * d, 0f); anemones += floatArrayOf(W * 0.52f, 16 * d, 2f); anemones += floatArrayOf(W * 0.83f, 18 * d, 1f)
        stars += floatArrayOf(W * 0.22f, 11 * d, 10f); stars += floatArrayOf(W * 0.66f, 9 * d, -20f)
        shells += floatArrayOf(W * 0.4f, 9 * d); shells += floatArrayOf(W * 0.9f, 8 * d)

        sand.reset()
        sand.moveTo(0f, H)
        sand.lineTo(0f, H * 0.93f)
        sand.cubicTo(W * 0.3f, H * 0.89f, W * 0.6f, H * 0.96f, W, H * 0.91f)
        sand.lineTo(W, H)
        sand.close()
        sand.addCircle(W * 0.18f, H * 0.935f, 16 * d, Path.Direction.CW)
        sand.addCircle(W * 0.78f, H * 0.93f, 20 * d, Path.Direction.CW)
    }

    private fun sandY(x: Float): Float {
        val u = x / width
        val h = height.toFloat()
        return h * (0.93f - 0.03f * sin(u * 3.3f) + 0.01f * sin(u * 9f))
    }

    private fun step() {
        t += 0.04f
        val w = width.toFloat(); val h = height.toFloat()

        val iter = bubbles.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.y -= b.speed * (1 + b.r / (20 * d))
            b.x += sin(t * 2 + b.phase) * 0.7f
            if (b.y < -b.r * 2) iter.remove()
        }
        if (kelp.isNotEmpty() && bubbles.size < 22 && rnd.nextFloat() < 0.2f) {
            val k = kelp.random(rnd)
            bubbles += Bubble(k[0] + (rnd.nextFloat() - 0.5f) * 20 * d, h * 0.92f, (2 + rnd.nextFloat() * 6) * d,
                (1 + rnd.nextFloat() * 2) * d / 2.5f, rnd.nextFloat() * 6.28f)
        }
        // the octopus blows a bubble now and then
        if (rnd.nextFloat() < 0.03f) bubbles += Bubble(octo.x + octo.size * 0.5f, octo.y - octo.size * 0.2f, 3 * d, 1.2f * d / 2.5f, 0f)

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
            j.y -= (0.25f + 1.2f * max(0f, sin(t * 1.4f + j.phase))) * d / 2.5f
            j.x += sin(t * 0.4f + j.phase) * 0.4f
            if (j.y < -j.size * 3) { j.y = h + j.size * 2; j.x = rnd.nextFloat() * w }
        }
        for (pl in plankton) {
            pl[1] -= 0.15f
            pl[0] += sin(t * 0.7f + pl[3]) * 0.2f
            if (pl[1] < 0f) pl[1] = h * 0.85f
        }

        turtle.x += turtle.dir * turtle.speed
        turtle.y += sin(t * 0.5f) * 0.25f
        if (turtle.x > w + turtle.size * 3) turtle.dir = -1 else if (turtle.x < -turtle.size * 3) turtle.dir = 1

        whale.x += whale.dir * whale.speed
        if (whale.x < -whale.size * 1.6f) whale.dir = 1 else if (whale.x > w + whale.size * 1.6f) whale.dir = -1

        octo.x += sin(t * 0.2f) * 0.35f
        crab.x += crab.dir * crab.speed
        if (crab.x > w * 0.85f) crab.dir = -1 else if (crab.x < w * 0.15f) crab.dir = 1
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val p = palette

        bgPaint.shader = LinearGradient(0f, 0f, 0f, h, p.top, p.bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, bgPaint)

        drawRays(c, w, h, p)
        drawWhale(c, p)
        drawCurrents(c, w, h, p)
        drawPlankton(c, p)
        drawKelp(c, h, p)
        for (co in corals) drawCoral(c, co[0], sandY(co[0]), co[1], p.mist[co[2].toInt()])
        for (a in anemones) drawAnemone(c, a[0], sandY(a[0]), a[1], p.mist[a[2].toInt()])
        fill.shader = null; fill.color = p.ground
        c.drawPath(sand, fill)
        for (s in stars) drawStarfish(c, s[0], sandY(s[0]) + 4 * d, s[1], s[2])
        for (s in shells) drawShell(c, s[0], sandY(s[0]) + 3 * d, s[1])
        drawCrab(c)
        for (s in seahorses) drawSeahorse(c, s, p)
        for (j in jellies) drawJelly(c, j, p)
        drawOctopus(c, p)
        drawTurtle(c, p)
        for (f in fish) drawFish(c, f, p)
        drawBubbles(c, p)
    }

    // ───────── environment ─────────

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

    private fun drawPlankton(c: Canvas, p: Palette) {
        for (pl in plankton) {
            fill.color = alpha(p.glow, (40 + 120 * (sin(t * 2 + pl[3]) + 1) / 2).toInt())
            c.drawCircle(pl[0], pl[1], pl[2], fill)
        }
    }

    private fun drawKelp(c: Canvas, h: Float, p: Palette) {
        for (k in kelp) {
            kelpPaint.color = alpha(p.ground, 235)
            kelpPaint.strokeWidth = k[3]
            path.reset()
            path.moveTo(k[0], h)
            var lx = k[0]
            var ly = h
            for (s in 1..10) {
                val y = h - k[1] * s / 10
                val x = k[0] + sin(t * 1.1f + s * 0.45f + k[2]) * s * 2.2f * d
                path.lineTo(x, y)
                if (s % 3 == 0) { // little leaves along the stalk
                    fill.color = alpha(p.ground, 235)
                    rect.set(x - 7 * d, y - 3 * d, x + 1 * d, y + 3 * d)
                    c.save(); c.rotate(if (s % 2 == 0) -30f else 210f, x, y); c.drawOval(rect, fill); c.restore()
                }
                lx = x; ly = y
            }
            c.drawPath(path, kelpPaint)
            fill.color = alpha(p.glow, 120) // glowing bulb at the tip
            c.drawCircle(lx, ly, k[3] * 0.45f, fill)
        }
    }

    private fun drawCoral(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        stroke.color = alpha(col, 200); stroke.strokeWidth = s * 0.16f
        coralBranch(c, x, y, s * 0.5f, (-PI / 2).toFloat(), 2, s, col)
    }

    private fun coralBranch(c: Canvas, bx: Float, by: Float, len: Float, ang: Float, depth: Int, s: Float, col: Int) {
        val ex = bx + len * cos(ang)
        val ey = by + len * sin(ang)
        c.drawLine(bx, by, ex, ey, stroke)
        if (depth > 0) {
            coralBranch(c, ex, ey, len * 0.7f, ang - 0.5f, depth - 1, s, col)
            coralBranch(c, ex, ey, len * 0.7f, ang + 0.45f, depth - 1, s, col)
        } else {
            fill.color = alpha(col, 255)
            c.drawCircle(ex, ey, s * 0.09f, fill)
        }
    }

    private fun drawAnemone(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        stroke.color = alpha(col, 220); stroke.strokeWidth = 3 * d
        for (k in 0 until 9) {
            val base = -PI.toFloat() * (0.15f + 0.7f * k / 8f)
            val a = base + sin(t * 1.6f + k * 0.6f + x) * 0.25f
            val ex = x + cos(a) * s
            val ey = y + sin(a) * s
            c.drawLine(x, y, ex, ey, stroke)
            fill.color = alpha(0xFFFFFF, 150)
            c.drawCircle(ex, ey, 2.2f * d, fill)
        }
        fill.color = alpha(col, 255)
        rect.set(x - s * 0.35f, y - s * 0.25f, x + s * 0.35f, y + s * 0.2f)
        c.drawOval(rect, fill)
    }

    private fun drawStarfish(c: Canvas, x: Float, y: Float, s: Float, rot: Float) {
        c.save(); c.rotate(rot, x, y)
        path.reset()
        for (i in 0 until 10) {
            val rr = if (i % 2 == 0) s else s * 0.45f
            val a = PI * i / 5 - PI / 2
            val px = x + (rr * cos(a)).toFloat()
            val py = y + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        fill.color = 0xFFFFA98A.toInt(); c.drawPath(path, fill)
        fill.color = 0xFFFFE0CC.toInt()
        c.drawCircle(x, y, s * 0.15f, fill)
        for (i in 0 until 5) {
            val a = PI * 2 * i / 5 - PI / 2
            c.drawCircle(x + (s * 0.55f * cos(a)).toFloat(), y + (s * 0.55f * sin(a)).toFloat(), s * 0.07f, fill)
        }
        c.restore()
    }

    private fun drawShell(c: Canvas, x: Float, y: Float, s: Float) {
        fill.color = 0xFFFFD9E6.toInt()
        rect.set(x - s, y - s, x + s, y + s)
        c.drawArc(rect, 180f, 180f, true, fill)
        stroke.color = 0xFFE89AB5.toInt(); stroke.strokeWidth = 1f * d
        for (k in 0..4) {
            val a = PI * (1.1 + k * 0.2)
            c.drawLine(x, y, x + (s * cos(a)).toFloat(), y + (s * sin(a)).toFloat(), stroke)
        }
    }

    // ───────── creatures ─────────

    private fun drawWhale(c: Canvas, p: Palette) {
        val s = whale.size
        c.save()
        c.translate(whale.x, whale.y + sin(t * 0.3f) * 6 * d)
        c.scale(whale.dir.toFloat(), 1f)
        fill.color = alpha(mix(p.top, p.glow, 0.25f), 70)
        rect.set(-s, -s * 0.28f, s * 0.75f, s * 0.3f)
        c.drawOval(rect, fill)
        val flap = sin(t * 0.8f) * s * 0.08f
        path.reset()
        path.moveTo(-s * 0.85f, 0f)
        path.quadTo(-s * 1.15f, -s * 0.05f, -s * 1.3f, -s * 0.25f + flap)
        path.quadTo(-s * 1.15f, 0f, -s * 1.3f, s * 0.2f + flap)
        path.quadTo(-s * 1.1f, s * 0.05f, -s * 0.85f, s * 0.06f)
        path.close()
        c.drawPath(path, fill)
        rect.set(-s * 0.1f, s * 0.1f, s * 0.25f, s * 0.42f)
        c.drawOval(rect, fill)
        stroke.color = alpha(p.glow, 35); stroke.strokeWidth = 1.5f * d
        for (k in 0 until 4) c.drawLine(-s * 0.1f + k * s * 0.12f, s * 0.2f, s * 0.05f + k * s * 0.12f, s * 0.24f, stroke)
        fill.color = alpha(p.glow, 90)
        c.drawCircle(s * 0.5f, -s * 0.02f, s * 0.025f, fill)
        c.restore()
    }

    private fun drawFish(c: Canvas, f: Fish, p: Palette) {
        val s = f.size
        val col = p.mist[f.color]
        c.save()
        c.translate(f.x, f.y)
        c.scale(f.dir.toFloat(), 1f)
        if (f.kind == 3) {
            drawPuffer(c, s, col)
            c.restore()
            return
        }
        val wig = sin(t * 8 + f.phase) * s * 0.18f
        fill.shader = null; fill.color = alpha(col, 220)
        path.reset()
        path.moveTo(-s * 0.7f, 0f)
        path.lineTo(-s * 1.55f, -s * 0.6f + wig)
        path.quadTo(-s * 1.3f, wig, -s * 1.55f, s * 0.6f + wig)
        path.close()
        c.drawPath(path, fill)
        fill.color = col
        rect.set(-s, -s * 0.62f, s, s * 0.62f)
        c.drawOval(rect, fill)
        path.reset()
        path.moveTo(-s * 0.3f, -s * 0.5f)
        path.quadTo(0f, -s * 1.05f, s * 0.35f, -s * 0.5f)
        path.close()
        c.drawPath(path, fill)
        // stripes or spots, clipped to the body
        c.save()
        path.reset(); path.addOval(rect, Path.Direction.CW); c.clipPath(path)
        if (f.kind == 1) {
            fill.color = alpha(0xFFFFFF, 110)
            c.drawRect(-s * 0.25f, -s, -s * 0.08f, s, fill)
            c.drawRect(-s * 0.65f, -s, -s * 0.5f, s, fill)
        } else if (f.kind == 2) {
            fill.color = alpha(0xFFFFFF, 90)
            c.drawCircle(-s * 0.3f, -s * 0.2f, s * 0.12f, fill)
            c.drawCircle(-s * 0.55f, s * 0.15f, s * 0.09f, fill)
            c.drawCircle(-s * 0.05f, s * 0.25f, s * 0.08f, fill)
        }
        c.restore()
        fill.color = alpha(0xFFFFFF, 60)
        rect.set(-s * 0.5f, s * 0.05f, s * 0.6f, s * 0.45f)
        c.drawOval(rect, fill)
        fill.color = alpha(darker(col), 200) // side fin flapping
        rect.set(-s * 0.05f, 0f, s * 0.35f, s * (0.25f + 0.08f * sin(t * 6 + f.phase)))
        c.drawOval(rect, fill)
        cuteFace(c, s * 0.45f, -s * 0.12f, s)
        c.restore()
    }

    private fun drawPuffer(c: Canvas, s: Float, col: Int) {
        val r = s * (0.85f + 0.06f * sin(t * 3))
        stroke.color = darker(col); stroke.strokeWidth = 1.6f * d
        for (k in 0 until 14) {
            val a = PI * 2 * k / 14
            c.drawLine((r * cos(a)).toFloat(), (r * sin(a)).toFloat(), (r * 1.25f * cos(a)).toFloat(), (r * 1.25f * sin(a)).toFloat(), stroke)
        }
        fill.color = col; c.drawCircle(0f, 0f, r, fill)
        fill.color = alpha(0xFFFFFF, 80); c.drawCircle(0f, r * 0.3f, r * 0.55f, fill)
        fill.color = alpha(col, 220)
        path.reset(); path.moveTo(-r * 0.9f, 0f); path.lineTo(-r * 1.4f, -r * 0.35f); path.lineTo(-r * 1.4f, r * 0.35f); path.close()
        c.drawPath(path, fill)
        cuteFace(c, r * 0.45f, -r * 0.15f, r)
        fill.color = 0xFF1A1030.toInt()
        c.drawCircle(r * 0.85f, r * 0.12f, r * 0.08f, fill) // tiny "o" mouth
    }

    private fun drawJelly(c: Canvas, j: Jelly, p: Palette) {
        val col = p.mist[j.color]
        val pulse = 1f + 0.12f * sin(t * 1.4f + j.phase + 3.14f)
        val s = j.size
        glowPaint.shader = RadialGradient(j.x, j.y, s * 2.2f, intArrayOf(alpha(col, 70), alpha(col, 0)), null, Shader.TileMode.CLAMP)
        c.drawCircle(j.x, j.y, s * 2.2f, glowPaint)
        stroke.color = alpha(col, 150); stroke.strokeWidth = 2.2f * d
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
        fill.shader = null; fill.color = alpha(col, 200)
        rect.set(j.x - s * pulse, j.y - s * 0.9f, j.x + s * pulse, j.y + s * 0.5f)
        c.drawArc(rect, 180f, 180f, true, fill)
        fill.color = alpha(col, 255)
        for (k in 0..6) c.drawCircle(j.x - s * pulse + k * (2 * s * pulse / 6), j.y - 0.2f * d, s * 0.13f, fill)
        fill.color = 0xFF1A1030.toInt()
        c.drawCircle(j.x - s * 0.28f, j.y - s * 0.3f, s * 0.07f, fill)
        c.drawCircle(j.x + s * 0.28f, j.y - s * 0.3f, s * 0.07f, fill)
        fill.color = alpha(0xFF7FB0, 140)
        c.drawCircle(j.x - s * 0.45f, j.y - s * 0.16f, s * 0.08f, fill)
        c.drawCircle(j.x + s * 0.45f, j.y - s * 0.16f, s * 0.08f, fill)
        fill.color = alpha(0xFFFFFF, 90)
        rect.set(j.x - s * 0.6f, j.y - s * 0.8f, j.x - s * 0.15f, j.y - s * 0.5f)
        c.drawOval(rect, fill)
    }

    private fun drawOctopus(c: Canvas, p: Palette) {
        val s = octo.size
        val x = octo.x
        val y = octo.y + sin(t * 0.9f) * 8 * d
        val col = p.mist[0]
        stroke.color = col; stroke.strokeWidth = s * 0.2f
        for (k in 0 until 6) { // 6 curly arms
            val bx = x - s * 0.6f + k * s * 0.24f
            var px = bx
            var py = y + s * 0.3f
            path.reset(); path.moveTo(px, py)
            for (seg in 1..8) {
                py += s * 0.13f
                px = bx + sin(t * 2f + k * 1.3f + seg * 0.6f) * s * 0.12f * seg / 4f + (k - 2.5f) * seg * s * 0.03f
                path.lineTo(px, py)
            }
            c.drawPath(path, stroke)
            fill.color = col
            c.drawCircle(px, py, s * 0.1f, fill)
        }
        fill.color = col
        rect.set(x - s * 0.75f, y - s * 0.95f, x + s * 0.75f, y + s * 0.45f)
        c.drawOval(rect, fill)
        fill.color = alpha(0xFFFFFF, 60)
        rect.set(x - s * 0.5f, y - s * 0.8f, x - s * 0.1f, y - s * 0.5f)
        c.drawOval(rect, fill)
        fill.color = alpha(darker(col), 120)
        c.drawCircle(x + s * 0.35f, y - s * 0.55f, s * 0.1f, fill)
        c.drawCircle(x + s * 0.5f, y - s * 0.25f, s * 0.07f, fill)
        cuteFace(c, x - s * 0.25f, y - s * 0.05f, s * 0.9f)
        cuteFace(c, x + s * 0.25f, y - s * 0.05f, s * 0.9f)
        stroke.color = 0xFF1A1030.toInt(); stroke.strokeWidth = 1.5f * d
        rect.set(x - s * 0.1f, y + s * 0.05f, x + s * 0.1f, y + s * 0.2f)
        c.drawArc(rect, 20f, 140f, false, stroke)
    }

    private fun drawTurtle(c: Canvas, p: Palette) {
        val s = turtle.size
        c.save()
        c.translate(turtle.x, turtle.y)
        c.scale(turtle.dir.toFloat(), 1f)
        val shellCol = p.mist[2]
        val paddle = sin(t * 2.2f) * 25f
        fill.color = 0xFF9FE0C0.toInt() // skin
        val flippers = arrayOf(
            floatArrayOf(s * 0.4f, -s * 0.35f, -40f), floatArrayOf(s * 0.4f, s * 0.35f, 40f),
            floatArrayOf(-s * 0.55f, -s * 0.3f, -140f), floatArrayOf(-s * 0.55f, s * 0.3f, 140f)
        )
        for (fl in flippers) {
            val fx = fl[0]; val fy = fl[1]
            val ang = fl[2] + (if (fy < 0f) -paddle else paddle)
            c.save(); c.rotate(ang, fx, fy)
            rect.set(fx, fy - s * 0.12f, fx + s * 0.6f, fy + s * 0.12f)
            c.drawOval(rect, fill)
            c.restore()
        }
        c.drawCircle(s * 0.95f, 0f, s * 0.26f, fill) // head
        fill.color = shellCol
        rect.set(-s * 0.8f, -s * 0.55f, s * 0.75f, s * 0.55f)
        c.drawOval(rect, fill)
        fill.color = alpha(darker(shellCol), 170)
        c.drawCircle(0f, 0f, s * 0.2f, fill)
        for (k in 0 until 6) {
            val a = PI * 2 * k / 6
            c.drawCircle((s * 0.45f * cos(a)).toFloat(), (s * 0.32f * sin(a)).toFloat(), s * 0.13f, fill)
        }
        stroke.color = alpha(0xFFFFFF, 90); stroke.strokeWidth = 1.5f * d
        c.drawOval(rect, stroke)
        cuteFace(c, s * 1.05f, -s * 0.06f, s * 0.6f)
        c.restore()
    }

    private fun drawSeahorse(c: Canvas, hs: Swimmer, p: Palette) {
        val s = hs.size
        val col = p.mist[1]
        c.save()
        c.translate(hs.x, hs.y + sin(t * 1.2f + hs.phase) * 10 * d)
        c.scale(hs.dir.toFloat(), 1f)
        fill.color = col
        for (i in 0..12) { // body: chain of circles down a gentle S
            val u = i / 12f
            val bx = sin(u * PI.toFloat()) * s * 0.35f
            val by = -s * 0.9f + u * s * 1.6f
            c.drawCircle(bx, by, s * (0.32f - u * 0.2f), fill)
        }
        for (i in 0..10) { // curled tail
            val a = PI.toFloat() * 0.5f + i * 0.45f
            val rr = s * (0.32f - i * 0.025f)
            c.drawCircle(-s * 0.05f + cos(a) * rr, s * 0.75f + sin(a) * rr, s * (0.12f - i * 0.009f), fill)
        }
        c.drawCircle(s * 0.05f, -s * 1.05f, s * 0.3f, fill) // head
        rect.set(s * 0.15f, -s * 1.1f, s * 0.75f, -s * 0.92f)
        c.drawRoundRect(rect, s * 0.1f, s * 0.1f, fill) // snout
        path.reset(); path.moveTo(-s * 0.15f, -s * 1.3f); path.lineTo(-s * 0.05f, -s * 1.5f); path.lineTo(s * 0.1f, -s * 1.32f); path.close()
        c.drawPath(path, fill) // little crown
        fill.color = alpha(0xFFFFFF, 110) // fluttering back fin
        val fl = sin(t * 10 + hs.phase) * s * 0.06f
        rect.set(-s * 0.5f - fl, -s * 0.35f, -s * 0.15f, -s * 0.05f)
        c.drawOval(rect, fill)
        stroke.color = alpha(0xFFFFFF, 70); stroke.strokeWidth = 1.2f * d
        for (i in 2..9) { // belly ridges
            val u = i / 12f
            val bx = sin(u * PI.toFloat()) * s * 0.35f
            val by = -s * 0.9f + u * s * 1.6f
            c.drawLine(bx + s * 0.1f, by, bx + s * 0.28f - u * s * 0.15f, by, stroke)
        }
        cuteFace(c, s * 0.12f, -s * 1.1f, s * 0.7f)
        c.restore()
    }

    private fun drawCrab(c: Canvas) {
        val s = crab.size
        val x = crab.x
        val y = sandY(x) - s * 0.35f
        val col = 0xFFFF8F7A.toInt()
        val stepAmt = sin(t * 8) * s * 0.12f
        stroke.color = darker(col); stroke.strokeWidth = 1.6f * d
        for (k in 0 until 3) { // legs
            val off = (k - 1) * s * 0.35f
            val lift = if (k % 2 == 0) stepAmt else -stepAmt
            c.drawLine(x - s * 0.5f + off * 0.3f, y, x - s * 1.0f + off * 0.3f, y + s * 0.5f + lift, stroke)
            c.drawLine(x + s * 0.5f - off * 0.3f, y, x + s * 1.0f - off * 0.3f, y + s * 0.5f - lift, stroke)
        }
        fill.color = col
        val wave = sin(t * 3) * s * 0.15f // claws waving hello
        c.drawLine(x - s * 0.5f, y - s * 0.1f, x - s * 0.9f, y - s * 0.45f + wave, stroke)
        c.drawLine(x + s * 0.5f, y - s * 0.1f, x + s * 0.9f, y - s * 0.45f - wave, stroke)
        c.drawCircle(x - s * 0.95f, y - s * 0.55f + wave, s * 0.28f, fill)
        c.drawCircle(x + s * 0.95f, y - s * 0.55f - wave, s * 0.28f, fill)
        rect.set(x - s * 0.7f, y - s * 0.45f, x + s * 0.7f, y + s * 0.35f)
        c.drawOval(rect, fill)
        c.drawLine(x - s * 0.22f, y - s * 0.35f, x - s * 0.25f, y - s * 0.75f, stroke) // eye stalks
        c.drawLine(x + s * 0.22f, y - s * 0.35f, x + s * 0.25f, y - s * 0.75f, stroke)
        cuteFace(c, x - s * 0.25f, y - s * 0.8f, s * 0.6f)
        cuteFace(c, x + s * 0.25f, y - s * 0.8f, s * 0.6f)
        fill.color = alpha(0xFF7FB0, 160)
        c.drawCircle(x - s * 0.4f, y - s * 0.05f, s * 0.1f, fill)
        c.drawCircle(x + s * 0.4f, y - s * 0.05f, s * 0.1f, fill)
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

    /** Big shiny eye + sparkle + blush. (x, y) = eye center, s = creature size. */
    private fun cuteFace(c: Canvas, x: Float, y: Float, s: Float) {
        fill.shader = null
        fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x, y, s * 0.2f, fill)
        fill.color = 0xFF1A1030.toInt(); c.drawCircle(x + s * 0.04f, y + s * 0.02f, s * 0.12f, fill)
        fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x + s * 0.08f, y - s * 0.03f, s * 0.04f, fill)
        fill.color = alpha(0xFF7FB0, 110); c.drawCircle(x - s * 0.15f, y + s * 0.28f, s * 0.1f, fill)
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun darker(col: Int): Int {
        val r = ((col shr 16) and 0xFF) * 6 / 10
        val g = ((col shr 8) and 0xFF) * 6 / 10
        val b = (col and 0xFF) * 6 / 10
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun mix(a: Int, b: Int, f: Float): Int {
        val r = (((a shr 16) and 0xFF) + ((((b shr 16) and 0xFF) - ((a shr 16) and 0xFF)) * f)).toInt().coerceIn(0, 255)
        val g = (((a shr 8) and 0xFF) + ((((b shr 8) and 0xFF) - ((a shr 8) and 0xFF)) * f)).toInt().coerceIn(0, 255)
        val bl = ((a and 0xFF) + (((b and 0xFF) - (a and 0xFF)) * f)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}
