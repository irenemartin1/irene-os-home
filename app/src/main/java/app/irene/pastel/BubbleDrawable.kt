package app.irene.pastel

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The whimsical frame around every app icon (real apps AND installed web apps like eBuilt).
 *
 * BASE layer (goes behind the icon): glassy orb with a soft inner glow, glowing rim and a glossy shine.
 * CHARM layer (goes on top of the icon): a tiny charm tucked on the corner + a sparkle.
 *   underwater → seashell, starfish, bubble cluster or coral
 *   forest     → leaf, mushroom, flower or moon
 * Each app always gets the same charm (picked from its package name).
 */
class BubbleDrawable(
    private val p: Palette,
    private val underwater: Boolean,
    seed: Int,
    private val d: Float,          // screen density
    private val charmLayer: Boolean
) : Drawable() {

    private val kind = abs(seed) % 4
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val rect = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val r = min(b.width(), b.height()) / 2f - 1.5f * d
        if (r <= 0) return
        if (charmLayer) drawCharm(c, cx, cy, r) else drawOrb(c, cx, cy, r)
    }

    private fun drawOrb(c: Canvas, cx: Float, cy: Float, r: Float) {
        // body: lighter glow up-left fading into the bubble color
        fill.shader = RadialGradient(
            cx - r * 0.35f, cy - r * 0.4f, r * 1.5f,
            intArrayOf(mix(p.bubble, p.glow, 0.28f), p.bubble, darker(p.bubble)),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r, fill)
        fill.shader = null

        // inner light ring along the bottom (light passing through the glass)
        line.strokeWidth = 2f * d
        line.color = alpha(p.glow, 60)
        rect.set(cx - r * 0.86f, cy - r * 0.86f, cx + r * 0.86f, cy + r * 0.86f)
        c.drawArc(rect, 20f, 140f, false, line)

        // glowing rim
        line.strokeWidth = 1.3f * d
        line.color = p.ring
        c.drawCircle(cx, cy, r, line)

        // glossy shine top-left
        fill.color = alpha(0xFFFFFF, 70)
        rect.set(cx - r * 0.62f, cy - r * 0.78f, cx - r * 0.02f, cy - r * 0.38f)
        c.save(); c.rotate(-30f, rect.centerX(), rect.centerY()); c.drawOval(rect, fill); c.restore()
        fill.color = alpha(0xFFFFFF, 160)
        c.drawCircle(cx - r * 0.52f, cy - r * 0.18f, r * 0.06f, fill)
    }

    private fun drawCharm(c: Canvas, cx: Float, cy: Float, r: Float) {
        // charm sits on the bottom-right of the orb
        val x = cx + r * 0.74f
        val y = cy + r * 0.74f
        val s = r * 0.30f
        if (underwater) when (kind) {
            0 -> shell(c, x, y, s)
            1 -> starfish(c, x, y, s)
            2 -> bubbles(c, x, y, s)
            else -> coral(c, x, y, s)
        } else when (kind) {
            0 -> leaf(c, x, y, s)
            1 -> mushroom(c, x, y, s)
            2 -> flower(c, x, y, s)
            else -> moon(c, x, y, s)
        }
        // little sparkle top-right
        sparkle(c, cx + r * 0.78f, cy - r * 0.7f, r * 0.13f, p.glow)
    }

    // ── ocean charms ──
    private fun shell(c: Canvas, x: Float, y: Float, s: Float) {
        fill.color = 0xFFFFD6E3.toInt()
        rect.set(x - s, y - s, x + s, y + s)
        c.drawArc(rect, 180f, 180f, true, fill)
        fill.color = 0xFFFFC2D4.toInt()
        c.drawRect(x - s * 0.3f, y, x + s * 0.3f, y + s * 0.35f, fill)
        line.strokeWidth = 0.9f * d; line.color = 0xFFE58AA8.toInt()
        for (k in 0..4) {
            val a = Math.toRadians((200 + k * 35).toDouble())
            c.drawLine(x, y, x + (s * 0.95f * cos(a)).toFloat(), y + (s * 0.95f * sin(a)).toFloat(), line)
        }
    }

    private fun starfish(c: Canvas, x: Float, y: Float, s: Float) {
        star(path, x, y, s * 1.1f, s * 0.45f, 5)
        fill.color = 0xFFFFB38A.toInt(); c.drawPath(path, fill)
        fill.color = 0xFFFFE3CF.toInt()
        c.drawCircle(x, y, s * 0.14f, fill)
        c.drawCircle(x, y - s * 0.5f, s * 0.08f, fill)
        c.drawCircle(x + s * 0.45f, y - s * 0.12f, s * 0.08f, fill)
    }

    private fun bubbles(c: Canvas, x: Float, y: Float, s: Float) {
        line.strokeWidth = 1.1f * d; line.color = alpha(p.glow, 220)
        fill.color = alpha(p.glow, 50)
        floatArrayOf(0f, 0f, 0.75f, -0.55f, -0.35f, -0.95f, 0.45f, 0.5f).toList().chunked(2).forEachIndexed { i, (dx, dy) ->
            val rr = s * (0.6f - i * 0.11f)
            c.drawCircle(x + dx * s, y + dy * s, rr, fill)
            c.drawCircle(x + dx * s, y + dy * s, rr, line)
        }
    }

    private fun coral(c: Canvas, x: Float, y: Float, s: Float) {
        line.color = 0xFFFF8FA8.toInt(); line.strokeWidth = s * 0.32f
        c.drawLine(x, y + s, x, y - s * 0.2f, line)
        c.drawLine(x, y + s * 0.2f, x - s * 0.6f, y - s * 0.5f, line)
        c.drawLine(x, y, x + s * 0.65f, y - s * 0.8f, line)
        fill.color = 0xFFFFC2D0.toInt()
        c.drawCircle(x, y - s * 0.3f, s * 0.2f, fill)
        c.drawCircle(x - s * 0.62f, y - s * 0.55f, s * 0.18f, fill)
        c.drawCircle(x + s * 0.68f, y - s * 0.85f, s * 0.18f, fill)
    }

    // ── forest charms ──
    private fun leaf(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        path.moveTo(x - s, y + s * 0.6f)
        path.quadTo(x - s * 0.6f, y - s * 1.0f, x + s, y - s * 0.6f)
        path.quadTo(x + s * 0.5f, y + s * 0.9f, x - s, y + s * 0.6f)
        path.close()
        fill.color = 0xFF9FE0A8.toInt(); c.drawPath(path, fill)
        line.strokeWidth = 0.9f * d; line.color = 0xFF4E8A5B.toInt()
        c.drawLine(x - s * 0.9f, y + s * 0.55f, x + s * 0.8f, y - s * 0.5f, line)
    }

    private fun mushroom(c: Canvas, x: Float, y: Float, s: Float) {
        fill.color = 0xFFFFF1E0.toInt()
        rect.set(x - s * 0.28f, y - s * 0.1f, x + s * 0.28f, y + s)
        c.drawRoundRect(rect, s * 0.2f, s * 0.2f, fill)
        fill.color = 0xFFC77DFF.toInt()
        rect.set(x - s, y - s * 0.9f, x + s, y + s * 0.4f)
        c.drawArc(rect, 180f, 180f, true, fill)
        fill.color = 0xFFFFFFFF.toInt()
        c.drawCircle(x - s * 0.4f, y - s * 0.45f, s * 0.14f, fill)
        c.drawCircle(x + s * 0.35f, y - s * 0.55f, s * 0.11f, fill)
        c.drawCircle(x + s * 0.05f, y - s * 0.15f, s * 0.09f, fill)
    }

    private fun flower(c: Canvas, x: Float, y: Float, s: Float) {
        fill.color = 0xFFFFC2EA.toInt()
        for (k in 0 until 5) {
            val a = Math.toRadians((k * 72 - 90).toDouble())
            c.drawCircle(x + (s * 0.55f * cos(a)).toFloat(), y + (s * 0.55f * sin(a)).toFloat(), s * 0.42f, fill)
        }
        fill.color = 0xFFFFE27A.toInt(); c.drawCircle(x, y, s * 0.32f, fill)
    }

    private fun moon(c: Canvas, x: Float, y: Float, s: Float) {
        fill.color = 0xFFFFE9A8.toInt(); c.drawCircle(x, y, s * 0.85f, fill)
        fill.color = p.bubble or 0xFF000000.toInt(); c.drawCircle(x + s * 0.38f, y - s * 0.25f, s * 0.7f, fill)
        sparkle(c, x - s * 0.1f, y + s * 0.95f, s * 0.3f, 0xFFFFE9A8.toInt())
    }

    // ── shared ──
    private fun sparkle(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        path.reset()
        path.moveTo(x, y - s)
        path.quadTo(x, y, x + s, y)
        path.quadTo(x, y, x, y + s)
        path.quadTo(x, y, x - s, y)
        path.quadTo(x, y, x, y - s)
        path.close()
        fill.color = col
        c.drawPath(path, fill)
    }

    private fun star(path: Path, x: Float, y: Float, outer: Float, inner: Float, points: Int) {
        path.reset()
        for (i in 0 until points * 2) {
            val rr = if (i % 2 == 0) outer else inner
            val a = Math.PI * i / points - Math.PI / 2
            val px = x + (rr * cos(a)).toFloat()
            val py = y + (rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun mix(a: Int, b: Int, t: Float): Int {
        fun ch(v: Int, s: Int) = (v shr s) and 0xFF
        fun lerp(x: Int, y: Int) = (x + (y - x) * t).toInt().coerceIn(0, 255)
        return (lerp(ch(a, 24), 255) shl 24) or (lerp(ch(a, 16), ch(b, 16)) shl 16) or
                (lerp(ch(a, 8), ch(b, 8)) shl 8) or lerp(ch(a, 0), ch(b, 0))
    }

    private fun darker(col: Int): Int {
        val a = (col shr 24) and 0xFF
        val r = ((col shr 16) and 0xFF) * 6 / 10
        val g = ((col shr 8) and 0xFF) * 6 / 10
        val bl = (col and 0xFF) * 6 / 10
        return (a shl 24) or (r shl 16) or (g shl 8) or bl
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
