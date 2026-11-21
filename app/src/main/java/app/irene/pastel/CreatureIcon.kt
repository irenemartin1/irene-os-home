package app.irene.pastel

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Every app icon becomes a little creature made of that app:
 * body = the app's own main color, belly = the app's symbol.
 *
 * Forest:     bunny, fox, owl, frog, bear, moth, hedgehog, mushroom sprite
 * Underwater: fish, octopus, crab, jellyfish, turtle, axolotl, pufferfish, whale
 * Each app always becomes the same creature (picked from its package name).
 */
class CreatureIcon(
    private val art: Drawable?,       // the app's symbol (monochrome layer if it has one, else its full icon)
    private val artIsMono: Boolean,
    bodyColor: Int,
    private val underwater: Boolean,
    seed: Int,
    private val d: Float,             // screen density
    private val glow: Int
) : Drawable() {

    private val kind = Math.floorMod(seed, 8)
    private val body = bodyColor or 0xFF000000.toInt()
    private val dark = shade(body, 0.5f)
    private val light = tint(body, 0.62f)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val rect = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        val s = min(b.width(), b.height()).toFloat()
        if (s <= 0f) return
        val cx = b.exactCenterX()
        val cy = b.exactCenterY() + s * 0.05f
        val r = s * 0.31f

        // soft glow under the creature
        fill.color = alpha(glow, 45)
        c.drawCircle(cx, cy, r * 1.45f, fill)

        if (underwater) oceanBack(c, cx, cy, r) else forestBack(c, cx, cy, r)

        // body + belly
        fill.color = body
        c.drawCircle(cx, cy, r, fill)
        val br = r * 0.56f
        val by = cy + r * 0.24f
        fill.color = light
        c.drawCircle(cx, by, br, fill)
        drawArt(c, cx, by, br)

        if (underwater) oceanFront(c, cx, cy, r) else forestFront(c, cx, cy, r)

        // face
        val eyeY = if (!underwater && kind == 3) cy - r * 0.92f else cy - r * 0.42f
        val eyeX = if (!underwater && kind == 3) r * 0.5f else r * 0.36f
        val er = r * (if (!underwater && kind == 3) 0.2f else 0.17f)
        eye(c, cx - eyeX, eyeY, er)
        eye(c, cx + eyeX, eyeY, er)
        fill.color = 0x66FF7FB0
        c.drawCircle(cx - r * 0.62f, cy - r * 0.12f, r * 0.12f, fill)
        c.drawCircle(cx + r * 0.62f, cy - r * 0.12f, r * 0.12f, fill)

        // glossy shine
        fill.color = 0x55FFFFFF
        rect.set(cx - r * 0.72f, cy - r * 0.78f, cx - r * 0.3f, cy - r * 0.52f)
        c.drawOval(rect, fill)
    }

    // ───────── forest creatures ─────────

    private fun forestBack(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (kind) {
            0 -> { // bunny: long ears
                for (sx in floatArrayOf(-1f, 1f)) {
                    c.save(); c.rotate(sx * 14f, cx + sx * r * 0.4f, cy - r * 0.6f)
                    fill.color = body; rect.set(cx + sx * r * 0.4f - r * 0.22f, cy - r * 1.75f, cx + sx * r * 0.4f + r * 0.22f, cy - r * 0.45f); c.drawOval(rect, fill)
                    fill.color = light; rect.inset(r * 0.09f, r * 0.15f); c.drawOval(rect, fill)
                    c.restore()
                }
            }
            1 -> { // fox: pointy ears + fluffy tail
                for (sx in floatArrayOf(-1f, 1f)) {
                    path.reset()
                    path.moveTo(cx + sx * r * 0.2f, cy - r * 0.8f)
                    path.lineTo(cx + sx * r * 0.85f, cy - r * 1.35f)
                    path.lineTo(cx + sx * r * 0.9f, cy - r * 0.45f)
                    path.close()
                    fill.color = body; c.drawPath(path, fill)
                }
                fill.color = body
                rect.set(cx + r * 0.4f, cy - r * 0.2f, cx + r * 1.45f, cy + r * 0.9f)
                c.drawOval(rect, fill)
                fill.color = 0xFFFFF6EE.toInt()
                c.drawCircle(cx + r * 1.3f, cy + r * 0.05f, r * 0.2f, fill)
            }
            2 -> { // owl: ear tufts + wings
                fill.color = dark
                for (sx in floatArrayOf(-1f, 1f)) {
                    path.reset()
                    path.moveTo(cx + sx * r * 0.3f, cy - r * 0.85f)
                    path.lineTo(cx + sx * r * 0.75f, cy - r * 1.3f)
                    path.lineTo(cx + sx * r * 0.85f, cy - r * 0.6f)
                    path.close(); c.drawPath(path, fill)
                    rect.set(cx + sx * r * 0.7f - r * 0.32f, cy - r * 0.3f, cx + sx * r * 0.7f + r * 0.32f, cy + r * 0.75f)
                    c.drawOval(rect, fill)
                }
            }
            3 -> { // frog: eye bumps on top
                fill.color = body
                c.drawCircle(cx - r * 0.5f, cy - r * 0.88f, r * 0.34f, fill)
                c.drawCircle(cx + r * 0.5f, cy - r * 0.88f, r * 0.34f, fill)
            }
            4 -> { // bear: round ears
                for (sx in floatArrayOf(-1f, 1f)) {
                    fill.color = body; c.drawCircle(cx + sx * r * 0.7f, cy - r * 0.75f, r * 0.3f, fill)
                    fill.color = light; c.drawCircle(cx + sx * r * 0.7f, cy - r * 0.75f, r * 0.16f, fill)
                }
            }
            5 -> { // moth / butterfly: wings + antennae
                for (sx in floatArrayOf(-1f, 1f)) {
                    fill.color = light
                    rect.set(cx + sx * r * 0.35f - r * 0.6f, cy - r * 1.05f, cx + sx * r * 0.35f + r * 0.6f, cy + r * 0.15f)
                    rect.offset(sx * r * 0.45f, 0f); c.drawOval(rect, fill)
                    fill.color = alpha(body, 200)
                    rect.set(cx + sx * r * 0.9f - r * 0.4f, cy - r * 0.05f, cx + sx * r * 0.9f + r * 0.4f, cy + r * 0.85f); c.drawOval(rect, fill)
                    fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(cx + sx * r * 1.0f, cy - r * 0.5f, r * 0.12f, fill)
                    line.color = dark; line.strokeWidth = r * 0.07f
                    c.drawLine(cx + sx * r * 0.15f, cy - r * 0.9f, cx + sx * r * 0.45f, cy - r * 1.45f, line)
                    fill.color = dark; c.drawCircle(cx + sx * r * 0.45f, cy - r * 1.45f, r * 0.08f, fill)
                }
            }
            6 -> { // hedgehog: spikes
                fill.color = dark
                for (k in 0 until 9) {
                    val a = Math.PI * (1.05 + k * 0.1)
                    val bx = cx + (r * 0.85f * cos(a)).toFloat(); val bY = cy + (r * 0.85f * sin(a)).toFloat()
                    val tx = cx + (r * 1.38f * cos(a)).toFloat(); val ty = cy + (r * 1.38f * sin(a)).toFloat()
                    val px = (-sin(a)).toFloat() * r * 0.2f; val py = cos(a).toFloat() * r * 0.2f
                    path.reset(); path.moveTo(bx - px, bY - py); path.lineTo(tx, ty); path.lineTo(bx + px, bY + py); path.close()
                    c.drawPath(path, fill)
                }
            }
        }
    }

    private fun forestFront(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (kind) {
            2 -> { // owl beak
                fill.color = 0xFFFFC35A.toInt()
                path.reset(); path.moveTo(cx - r * 0.1f, cy - r * 0.3f); path.lineTo(cx + r * 0.1f, cy - r * 0.3f); path.lineTo(cx, cy - r * 0.12f); path.close()
                c.drawPath(path, fill)
            }
            7 -> { // mushroom sprite: big spotted cap on top
                fill.color = 0xFFC77DFF.toInt()
                rect.set(cx - r * 1.2f, cy - r * 1.45f, cx + r * 1.2f, cy - r * 0.1f)
                c.drawArc(rect, 180f, 180f, true, fill)
                fill.color = 0xFFFFFFFF.toInt()
                c.drawCircle(cx - r * 0.55f, cy - r * 0.95f, r * 0.16f, fill)
                c.drawCircle(cx + r * 0.3f, cy - r * 1.15f, r * 0.13f, fill)
                c.drawCircle(cx + r * 0.75f, cy - r * 0.6f, r * 0.11f, fill)
            }
        }
        // little feet
        fill.color = dark
        rect.set(cx - r * 0.55f, cy + r * 0.82f, cx - r * 0.12f, cy + r * 1.08f); c.drawOval(rect, fill)
        rect.set(cx + r * 0.12f, cy + r * 0.82f, cx + r * 0.55f, cy + r * 1.08f); c.drawOval(rect, fill)
    }

    // ───────── ocean creatures ─────────

    private fun oceanBack(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (kind) {
            0 -> { // fish: tail + top fin
                fill.color = dark
                path.reset(); path.moveTo(cx - r * 0.8f, cy); path.lineTo(cx - r * 1.5f, cy - r * 0.6f); path.lineTo(cx - r * 1.5f, cy + r * 0.6f); path.close()
                c.drawPath(path, fill)
                path.reset(); path.moveTo(cx - r * 0.4f, cy - r * 0.85f); path.quadTo(cx, cy - r * 1.5f, cx + r * 0.4f, cy - r * 0.85f); path.close()
                c.drawPath(path, fill)
            }
            1 -> { // octopus: curly arms
                fill.color = body
                for (k in 0 until 5) {
                    val x = cx - r * 0.8f + k * r * 0.4f
                    rect.set(x - r * 0.17f, cy + r * 0.3f, x + r * 0.17f, cy + r * 1.35f); c.drawOval(rect, fill)
                    fill.color = light; c.drawCircle(x, cy + r * 1.15f, r * 0.07f, fill); fill.color = body
                }
            }
            2 -> { // crab: claws + legs
                line.color = dark; line.strokeWidth = r * 0.12f
                for (sx in floatArrayOf(-1f, 1f)) {
                    c.drawLine(cx + sx * r * 0.7f, cy - r * 0.1f, cx + sx * r * 1.15f, cy - r * 0.7f, line)
                    for (k in 0 until 3) c.drawLine(cx + sx * r * 0.8f, cy + r * (0.15f + k * 0.25f), cx + sx * r * 1.3f, cy + r * (0.3f + k * 0.3f), line)
                    fill.color = body; c.drawCircle(cx + sx * r * 1.2f, cy - r * 0.85f, r * 0.32f, fill)
                    fill.color = dark
                    path.reset(); path.moveTo(cx + sx * r * 1.2f, cy - r * 0.85f); path.lineTo(cx + sx * r * 1.55f, cy - r * 1.15f); path.lineTo(cx + sx * r * 1.55f, cy - r * 0.75f); path.close()
                    c.drawPath(path, fill)
                }
            }
            3 -> { // jellyfish: wavy tentacles
                line.color = alpha(body, 220); line.strokeWidth = r * 0.1f
                for (k in 0 until 5) {
                    val x = cx - r * 0.6f + k * r * 0.3f
                    path.reset(); path.moveTo(x, cy + r * 0.4f)
                    path.quadTo(x + r * 0.2f, cy + r * 0.9f, x, cy + r * 1.2f)
                    path.quadTo(x - r * 0.2f, cy + r * 1.4f, x, cy + r * 1.6f)
                    c.drawPath(path, line)
                }
            }
            4 -> { // turtle: flippers + shell rim
                fill.color = 0xFF9FE0C0.toInt()
                for (sx in floatArrayOf(-1f, 1f)) for (sy in floatArrayOf(-1f, 1f)) {
                    c.save(); c.rotate(sx * sy * 35f, cx + sx * r * 0.9f, cy + sy * r * 0.55f)
                    rect.set(cx + sx * r * 0.9f - r * 0.35f, cy + sy * r * 0.55f - r * 0.18f, cx + sx * r * 0.9f + r * 0.35f, cy + sy * r * 0.55f + r * 0.18f)
                    c.drawOval(rect, fill); c.restore()
                }
                fill.color = dark; c.drawCircle(cx, cy, r * 1.08f, fill)
            }
            5 -> { // axolotl: frilly gills
                fill.color = 0xFFFF9FC8.toInt()
                for (sx in floatArrayOf(-1f, 1f)) for (k in 0 until 3) {
                    c.save(); c.rotate(sx * (-30f + k * 30f), cx + sx * r * 0.8f, cy - r * 0.35f)
                    rect.set(cx + sx * r * 0.8f, cy - r * 0.45f, cx + sx * r * 0.8f + sx * r * 0.55f, cy - r * 0.25f)
                    rect.sort(); c.drawOval(rect, fill); c.restore()
                }
            }
            6 -> { // pufferfish: spikes all around
                line.color = dark; line.strokeWidth = r * 0.08f
                for (k in 0 until 16) {
                    val a = Math.PI * 2 * k / 16
                    c.drawLine(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), cx + (r * 1.3f * cos(a)).toFloat(), cy + (r * 1.3f * sin(a)).toFloat(), line)
                }
            }
            7 -> { // whale: tail fluke + spout
                fill.color = body
                path.reset(); path.moveTo(cx + r * 0.7f, cy + r * 0.2f); path.quadTo(cx + r * 1.3f, cy - r * 0.1f, cx + r * 1.2f, cy - r * 0.6f)
                path.lineTo(cx + r * 1.55f, cy - r * 0.75f); path.quadTo(cx + r * 1.2f, cy - r * 0.35f, cx + r * 1.15f, cy - r * 0.2f); path.close()
                c.drawPath(path, fill)
                fill.color = alpha(glow, 200)
                c.drawCircle(cx - r * 0.2f, cy - r * 1.25f, r * 0.1f, fill)
                c.drawCircle(cx, cy - r * 1.45f, r * 0.12f, fill)
                c.drawCircle(cx + r * 0.2f, cy - r * 1.25f, r * 0.1f, fill)
            }
        }
    }

    private fun oceanFront(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (kind) {
            0 -> { fill.color = alpha(dark, 200); rect.set(cx + r * 0.2f, cy + r * 0.05f, cx + r * 0.75f, cy + r * 0.35f); c.drawOval(rect, fill) } // side fin
            3 -> { fill.color = 0x66FFFFFF; for (k in 0..4) c.drawCircle(cx - r * 0.8f + k * r * 0.4f, cy + r * 0.55f, r * 0.14f, fill) } // frilly rim
        }
        // a couple of bubbles
        line.color = alpha(glow, 200); line.strokeWidth = r * 0.05f
        c.drawCircle(cx + r * 1.05f, cy - r * 1.05f, r * 0.13f, line)
        c.drawCircle(cx + r * 1.3f, cy - r * 1.35f, r * 0.08f, line)
    }

    // ───────── helpers ─────────

    private fun drawArt(c: Canvas, x: Float, y: Float, br: Float) {
        val a = art ?: return
        c.save()
        path.reset(); path.addCircle(x, y, br * 0.97f, Path.Direction.CW); c.clipPath(path)
        val half = if (artIsMono) br * 1.32f else br * 0.95f
        a.setBounds((x - half).toInt(), (y - half).toInt(), (x + half).toInt(), (y + half).toInt())
        if (artIsMono) a.setTint(dark)
        a.draw(c)
        c.restore()
    }

    private fun eye(c: Canvas, x: Float, y: Float, er: Float) {
        fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x, y, er, fill)
        fill.color = 0xFF1A1030.toInt(); c.drawCircle(x + er * 0.15f, y + er * 0.1f, er * 0.62f, fill)
        fill.color = 0xFFFFFFFF.toInt(); c.drawCircle(x + er * 0.35f, y - er * 0.2f, er * 0.22f, fill)
    }

    private fun alpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)
    private fun shade(col: Int, f: Float) = Color.rgb((Color.red(col) * f).toInt(), (Color.green(col) * f).toInt(), (Color.blue(col) * f).toInt())
    private fun tint(col: Int, f: Float) = Color.rgb(
        (Color.red(col) + (255 - Color.red(col)) * f).toInt(),
        (Color.green(col) + (255 - Color.green(col)) * f).toInt(),
        (Color.blue(col) + (255 - Color.blue(col)) * f).toInt()
    )

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    companion object {
        private val pastels = intArrayOf(
            0xFFC9A0FF.toInt(), 0xFFFFB8D9.toInt(), 0xFF9FE0C0.toInt(), 0xFFFFD38A.toInt(), 0xFFA8D8FF.toInt(), 0xFFFFA98A.toInt()
        )

        /** The app's main color, softened to a pastel. Grey/black icons get a pastel picked by seed. */
        fun bodyColorFor(icon: Drawable, seed: Int): Int {
            val n = 32
            val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
            try {
                val copy = icon.constantState?.newDrawable()?.mutate() ?: icon
                copy.setBounds(0, 0, n, n)
                copy.draw(Canvas(bmp))
            } catch (_: Throwable) {
                bmp.recycle(); return pastels[Math.floorMod(seed, pastels.size)]
            }
            var rs = 0.0; var gs = 0.0; var bs = 0.0; var ws = 0.0
            val hsv = FloatArray(3)
            for (y in 0 until n) for (x in 0 until n) {
                val px = bmp.getPixel(x, y)
                val a = (px ushr 24) / 255.0
                if (a < 0.5) continue
                Color.colorToHSV(px, hsv)
                val w = a * (0.05 + hsv[1] * hsv[1]) * (0.2 + hsv[2])
                rs += Color.red(px) * w; gs += Color.green(px) * w; bs += Color.blue(px) * w; ws += w
            }
            bmp.recycle()
            if (ws <= 0.0) return pastels[Math.floorMod(seed, pastels.size)]
            Color.colorToHSV(Color.rgb((rs / ws).toInt(), (gs / ws).toInt(), (bs / ws).toInt()), hsv)
            if (hsv[1] < 0.2f) return pastels[Math.floorMod(seed, pastels.size)]
            hsv[1] = hsv[1].coerceIn(0.35f, 0.65f)
            hsv[2] = hsv[2].coerceIn(0.85f, 1f)
            return Color.HSVToColor(hsv)
        }
    }
}
