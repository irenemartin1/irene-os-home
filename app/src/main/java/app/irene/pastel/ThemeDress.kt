package app.irene.pastel

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dresses each app creature as the theme's own character (from Irene OS):
 * Kawaii = pastel bunny, Bull Market = bull in a suit, Celestial = moon spirit, Luxury = butler,
 * Princess = tiara, Zen = wise turtle, Cyber = ninja, Disco = mirror ball, and so on.
 * back() draws what sits behind the body (ears, horns, wings, petals); front() what sits on top (hats, bows, glasses).
 */
class ThemeDress(private val w: World, private val body: Int) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val rect = RectF()
    private val gold = Color.parseColor("#E9C25A")
    private val white = Color.WHITE
    private val ink = Color.parseColor("#2A2230")
    private fun a(c: Int, al: Int) = World.withAlpha(c, al)
    private val dark = World.mix(body, Color.BLACK, 0.45f)

    /** Mixes the app's own color with the theme's colors so every theme looks like itself. */
    companion object {
        fun bodyFor(w: World, appColor: Int): Int = when (w.key) {
            "productivity" -> World.mix(appColor, Color.parseColor("#d9d9d4"), 0.55f)
            "luxury", "academia", "gallery" -> World.mix(appColor, w.accent, 0.45f)
            "digital", "cyber", "circuit", "disco", "fitness", "bull", "celestial" -> World.mix(appColor, w.accent, 0.3f)
            else -> World.mix(appColor, w.accent, 0.28f)
        } or 0xFF000000.toInt()

        /** Eye style: 0 normal, 1 glowing screen eyes (robots / chips). */
        fun eyeStyle(w: World) = when (w.key) { "digital", "circuit" -> 1; else -> 0 }
    }

    fun back(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (w.key) {
            "digital" -> { // little robot: antenna
                line.color = a(w.fg, 230); line.strokeWidth = r * 0.09f
                c.drawLine(cx, cy - r * 0.95f, cx, cy - r * 1.35f, line)
                fill.color = w.accent; c.drawCircle(cx, cy - r * 1.4f, r * 0.14f, fill)
                fill.color = a(w.accent, 70); c.drawCircle(cx, cy - r * 1.4f, r * 0.28f, fill)
            }
            "nature" -> { // forest elf: pointy ears
                fill.color = body
                for (sx in floatArrayOf(-1f, 1f)) {
                    path.reset(); path.moveTo(cx + sx * r * 0.75f, cy - r * 0.45f)
                    path.lineTo(cx + sx * r * 1.42f, cy - r * 0.85f); path.lineTo(cx + sx * r * 0.88f, cy - r * 0.05f); path.close()
                    c.drawPath(path, fill)
                }
            }
            "productivity" -> { // calm owl: ear tufts
                fill.color = dark
                for (sx in floatArrayOf(-1f, 1f)) {
                    path.reset(); path.moveTo(cx + sx * r * 0.25f, cy - r * 0.9f)
                    path.lineTo(cx + sx * r * 0.85f, cy - r * 1.3f); path.lineTo(cx + sx * r * 0.85f, cy - r * 0.55f); path.close()
                    c.drawPath(path, fill)
                }
            }
            "kawaii" -> { // pastel bunny: long ears
                for (sx in floatArrayOf(-1f, 1f)) {
                    fill.color = body; rect.set(cx + sx * r * 0.42f - r * 0.24f, cy - r * 1.75f, cx + sx * r * 0.42f + r * 0.24f, cy - r * 0.5f); c.drawOval(rect, fill)
                    fill.color = Color.parseColor("#FFB3C9"); rect.inset(r * 0.09f, r * 0.15f); c.drawOval(rect, fill)
                }
            }
            "bull" -> { // bull in a suit: horns
                fill.color = Color.parseColor("#F4EBD0")
                for (sx in floatArrayOf(-1f, 1f)) {
                    path.reset(); path.moveTo(cx + sx * r * 0.55f, cy - r * 0.7f)
                    path.quadTo(cx + sx * r * 1.35f, cy - r * 0.8f, cx + sx * r * 1.25f, cy - r * 1.35f)
                    path.quadTo(cx + sx * r * 1.05f, cy - r * 0.95f, cx + sx * r * 0.35f, cy - r * 0.95f); path.close()
                    c.drawPath(path, fill)
                }
            }
            "celestial" -> { // moon spirit: crescent moon halo + stars
                fill.color = w.accent; c.drawCircle(cx + r * 0.75f, cy - r * 0.85f, r * 0.55f, fill)
                fill.color = w.base; c.drawCircle(cx + r * 1.0f, cy - r * 1.0f, r * 0.48f, fill)
                fill.color = a(white, 230)
                star(c, cx - r * 1.05f, cy - r * 0.9f, r * 0.17f); star(c, cx - r * 1.25f, cy - r * 0.2f, r * 0.1f)
            }
            "romantic" -> { // little cupid: wings
                fill.color = a(white, 235)
                for (sx in floatArrayOf(-1f, 1f)) for (k in 0 until 3) {
                    val ey = cy - r * 0.35f + k * r * 0.28f
                    rect.set(cx + sx * (r * 0.6f), ey - r * 0.2f, cx + sx * (r * (1.45f - k * 0.15f)), ey + r * 0.2f)
                    rect.sort(); c.drawOval(rect, fill)
                }
            }
            "coastal" -> { // mermaid: tail fin
                fill.color = World.mix(body, w.accent, 0.4f)
                path.reset(); path.moveTo(cx + r * 0.5f, cy + r * 0.7f)
                path.quadTo(cx + r * 1.2f, cy + r * 0.8f, cx + r * 1.45f, cy + r * 0.35f)
                path.quadTo(cx + r * 1.25f, cy + r * 0.85f, cx + r * 1.55f, cy + r * 1.15f)
                path.quadTo(cx + r * 1.0f, cy + r * 1.1f, cx + r * 0.3f, cy + r * 0.95f); path.close(); c.drawPath(path, fill)
            }
            "zen" -> { // wise turtle: little legs + head bump
                fill.color = World.mix(body, w.accent, 0.5f)
                for ((dx, dy) in listOf(-0.85f to 0.65f, 0.85f to 0.65f, -0.95f to -0.1f, 0.95f to -0.1f)) {
                    rect.set(cx + dx * r - r * 0.22f, cy + dy * r - r * 0.15f, cx + dx * r + r * 0.22f, cy + dy * r + r * 0.15f); c.drawOval(rect, fill)
                }
            }
            "y2k" -> { // glitter butterfly: big wings
                for (sx in floatArrayOf(-1f, 1f)) {
                    fill.color = a(w.accent, 210); rect.set(cx + sx * r * 0.3f, cy - r * 1.2f, cx + sx * r * 1.5f, cy + r * 0.05f); rect.sort(); c.drawOval(rect, fill)
                    fill.color = a(Color.parseColor("#7FD8FF"), 210); rect.set(cx + sx * r * 0.4f, cy - r * 0.05f, cx + sx * r * 1.25f, cy + r * 0.95f); rect.sort(); c.drawOval(rect, fill)
                    fill.color = a(white, 220)
                    c.drawCircle(cx + sx * r * 1.0f, cy - r * 0.6f, r * 0.08f, fill); c.drawCircle(cx + sx * r * 0.85f, cy + r * 0.45f, r * 0.06f, fill)
                }
            }
            "retro" -> { // disco dancer: big afro
                fill.color = Color.parseColor("#4A2C1A")
                for (k in 0 until 9) { val an = PI + k * PI / 8; c.drawCircle(cx + (r * 0.95f * cos(an)).toFloat(), cy - r * 0.15f + (r * 0.95f * sin(an)).toFloat(), r * 0.38f, fill) }
                c.drawCircle(cx, cy - r * 0.55f, r * 0.75f, fill)
            }
            "academia" -> { // library cat: pointy cat ears
                fill.color = dark
                for (sx in floatArrayOf(-1f, 1f)) {
                    path.reset(); path.moveTo(cx + sx * r * 0.2f, cy - r * 0.85f); path.lineTo(cx + sx * r * 0.85f, cy - r * 1.35f); path.lineTo(cx + sx * r * 0.95f, cy - r * 0.35f); path.close(); c.drawPath(path, fill)
                }
            }
            "circuit" -> { // chip critter: pins all around
                fill.color = gold
                for (k in 0 until 12) {
                    val an = k * PI / 6 + PI / 12
                    c.save(); c.rotate(Math.toDegrees(an).toFloat(), cx, cy)
                    c.drawRect(cx + r * 0.9f, cy - r * 0.07f, cx + r * 1.22f, cy + r * 0.07f, fill); c.restore()
                }
            }
            "pride" -> { // rainbow unicorn: rainbow mane + golden horn
                val bands = intArrayOf(0xFFFF6B6B.toInt(), 0xFFFFA94D.toInt(), 0xFFFFE066.toInt(), 0xFF69DB7C.toInt(), 0xFF4DABF7.toInt(), 0xFFB197FC.toInt())
                for (k in bands.indices) { fill.color = bands[k]; c.drawCircle(cx - r * 0.9f + k * r * 0.05f, cy - r * 0.7f + k * r * 0.3f, r * 0.26f, fill) }
                fill.color = gold
                path.reset(); path.moveTo(cx - r * 0.18f, cy - r * 0.85f); path.lineTo(cx + r * 0.1f, cy - r * 1.6f); path.lineTo(cx + r * 0.22f, cy - r * 0.8f); path.close(); c.drawPath(path, fill)
            }
            "groovy" -> { // groovy daisy: petals all around
                fill.color = white
                for (k in 0 until 10) {
                    val an = k * PI / 5
                    c.save(); c.rotate(Math.toDegrees(an).toFloat(), cx, cy)
                    rect.set(cx + r * 0.7f, cy - r * 0.26f, cx + r * 1.45f, cy + r * 0.26f); c.drawOval(rect, fill); c.restore()
                }
            }
            "glass" -> { // glass fairy: see-through wings
                for (sx in floatArrayOf(-1f, 1f)) {
                    fill.color = a(w.accent, 90); rect.set(cx + sx * r * 0.5f, cy - r * 1.3f, cx + sx * r * 1.45f, cy - r * 0.1f); rect.sort(); c.drawOval(rect, fill)
                    line.color = a(w.fg, 150); line.strokeWidth = r * 0.05f; c.drawOval(rect, line)
                }
            }
            "princess" -> { // princess: flowing hair
                fill.color = Color.parseColor("#F6C36B")
                rect.set(cx - r * 1.1f, cy - r * 1.05f, cx + r * 1.1f, cy + r * 0.8f); c.drawOval(rect, fill)
            }
            "mushroom" -> { // mushroom sprite: little stem feet
                fill.color = Color.parseColor("#F7EBD3")
                rect.set(cx - r * 0.55f, cy + r * 0.7f, cx - r * 0.15f, cy + r * 1.1f); c.drawOval(rect, fill)
                rect.set(cx + r * 0.15f, cy + r * 0.7f, cx + r * 0.55f, cy + r * 1.1f); c.drawOval(rect, fill)
            }
        }
    }

    fun front(c: Canvas, cx: Float, cy: Float, r: Float) {
        when (w.key) {
            "digital" -> { // visor across the eyes
                fill.color = a(w.base, 235); rect.set(cx - r * 0.72f, cy - r * 0.62f, cx + r * 0.72f, cy - r * 0.22f); c.drawRoundRect(rect, r * 0.2f, r * 0.2f, fill)
                line.color = a(w.accent, 200); line.strokeWidth = r * 0.05f; c.drawRoundRect(rect, r * 0.2f, r * 0.2f, line)
            }
            "nature" -> leaf(c, cx + r * 0.15f, cy - r * 1.05f, r * 0.42f, Color.parseColor("#6E8F55"))
            "gallery" -> { // artist beret
                fill.color = Color.parseColor("#B5463B")
                c.save(); c.rotate(-12f, cx, cy - r * 0.9f)
                rect.set(cx - r * 0.85f, cy - r * 1.15f, cx + r * 0.75f, cy - r * 0.62f); c.drawOval(rect, fill)
                line.color = fill.color; line.strokeWidth = r * 0.09f; c.drawLine(cx, cy - r * 1.13f, cx + r * 0.05f, cy - r * 1.3f, line)
                c.restore()
            }
            "productivity" -> glasses(c, cx, cy, r, a(ink, 220))
            "studio" -> { // headphones
                line.color = a(ink, 230); line.strokeWidth = r * 0.12f
                rect.set(cx - r * 0.95f, cy - r * 1.2f, cx + r * 0.95f, cy + r * 0.4f); c.drawArc(rect, 200f, 140f, false, line)
                fill.color = w.accent
                for (sx in floatArrayOf(-1f, 1f)) { rect.set(cx + sx * r * 0.95f - r * 0.2f, cy - r * 0.45f, cx + sx * r * 0.95f + r * 0.2f, cy + r * 0.05f); c.drawRoundRect(rect, r * 0.15f, r * 0.15f, fill) }
            }
            "kawaii" -> { fill.color = Color.parseColor("#FF6B8B"); bow(c, cx + r * 0.55f, cy - r * 0.85f, r * 0.3f) }
            "bull" -> { // gold bow tie
                fill.color = gold; bow(c, cx, cy + r * 0.92f, r * 0.26f)
            }
            "mushroom" -> { // red mushroom cap
                fill.color = w.accent
                rect.set(cx - r * 1.15f, cy - r * 1.35f, cx + r * 1.15f, cy - r * 0.15f); c.drawArc(rect, 180f, 180f, true, fill)
                fill.color = white
                c.drawCircle(cx - r * 0.5f, cy - r * 0.95f, r * 0.13f, fill); c.drawCircle(cx + r * 0.2f, cy - r * 1.1f, r * 0.16f, fill); c.drawCircle(cx + r * 0.7f, cy - r * 0.8f, r * 0.1f, fill)
            }
            "coquette" -> { // big ribbon bow + lace collar
                fill.color = a(white, 230)
                for (k in 0 until 7) c.drawCircle(cx - r * 0.6f + k * r * 0.2f, cy + r * 0.88f - kotlin.math.abs(k - 3) * r * 0.06f, r * 0.11f, fill)
                fill.color = w.accent; bow(c, cx, cy - r * 0.98f, r * 0.42f)
            }
            "celestial" -> { fill.color = w.accent; star(c, cx, cy - r * 0.78f, r * 0.13f) }
            "romantic" -> { fill.color = w.accent; heart(c, cx + r * 0.5f, cy - r * 0.9f, r * 0.25f) }
            "luxury" -> { // butler: black top hat with gold band + gold bow tie
                fill.color = Color.parseColor("#111112")
                c.drawRect(cx - r * 0.45f, cy - r * 1.55f, cx + r * 0.45f, cy - r * 0.8f, fill)
                rect.set(cx - r * 0.75f, cy - r * 0.92f, cx + r * 0.75f, cy - r * 0.72f); c.drawOval(rect, fill)
                fill.color = gold; c.drawRect(cx - r * 0.45f, cy - r * 1.02f, cx + r * 0.45f, cy - r * 0.9f, fill)
                bow(c, cx, cy + r * 0.92f, r * 0.24f)
            }
            "coastal" -> { // seashell hair clip
                fill.color = Color.parseColor("#FFD1C1")
                rect.set(cx + r * 0.25f, cy - r * 1.15f, cx + r * 0.85f, cy - r * 0.55f); c.drawArc(rect, 180f, 180f, true, fill)
                line.color = a(Color.parseColor("#D2694C"), 180); line.strokeWidth = r * 0.04f
                for (k in -2..2) c.drawLine(cx + r * 0.55f, cy - r * 0.85f, cx + r * 0.55f + k * r * 0.12f, cy - r * 1.1f, line)
            }
            "zen" -> { // turtle shell dome on top
                fill.color = World.mix(body, w.strong, 0.55f)
                rect.set(cx - r * 1.02f, cy - r * 1.08f, cx + r * 1.02f, cy - r * 0.05f); c.drawArc(rect, 180f, 180f, true, fill)
                line.color = a(w.base, 170); line.strokeWidth = r * 0.06f
                hexagon(c, cx, cy - r * 0.78f, r * 0.22f); hexagon(c, cx - r * 0.5f, cy - r * 0.5f, r * 0.18f); hexagon(c, cx + r * 0.5f, cy - r * 0.5f, r * 0.18f)
                leaf(c, cx - r * 0.1f, cy - r * 1.2f, r * 0.25f, w.accent)
            }
            "y2k" -> { fill.color = a(white, 240); star(c, cx + r * 0.55f, cy - r * 0.85f, r * 0.16f) }
            "retro" -> { // big 70s sunglasses
                fill.color = a(Color.parseColor("#E0702B"), 210)
                for (sx in floatArrayOf(-1f, 1f)) { rect.set(cx + sx * r * 0.08f, cy - r * 0.62f, cx + sx * r * 0.7f, cy - r * 0.22f); rect.sort(); c.drawRoundRect(rect, r * 0.2f, r * 0.2f, fill) }
            }
            "cyber" -> { // ninja mask + neon headband with tails
                fill.color = a(Color.parseColor("#120024"), 230); rect.set(cx - r * 0.98f, cy - r * 0.66f, cx + r * 0.98f, cy - r * 0.2f); c.drawRect(rect, fill)
                fill.color = w.accent; c.drawRect(cx - r * 0.95f, cy - r * 0.92f, cx + r * 0.95f, cy - r * 0.76f, fill)
                line.color = w.accent; line.strokeWidth = r * 0.1f
                c.drawLine(cx + r * 0.95f, cy - r * 0.84f, cx + r * 1.4f, cy - r * 0.6f, line); c.drawLine(cx + r * 0.95f, cy - r * 0.84f, cx + r * 1.35f, cy - r * 1.05f, line)
            }
            "academia" -> glasses(c, cx, cy, r, gold)
            "princess" -> { // golden tiara with a pink gem
                fill.color = gold
                path.reset(); path.moveTo(cx - r * 0.6f, cy - r * 0.78f)
                path.lineTo(cx - r * 0.55f, cy - r * 1.15f); path.lineTo(cx - r * 0.28f, cy - r * 0.95f); path.lineTo(cx, cy - r * 1.35f)
                path.lineTo(cx + r * 0.28f, cy - r * 0.95f); path.lineTo(cx + r * 0.55f, cy - r * 1.15f); path.lineTo(cx + r * 0.6f, cy - r * 0.78f); path.close()
                c.drawPath(path, fill)
                fill.color = w.accent; c.drawCircle(cx, cy - r * 0.98f, r * 0.1f, fill)
            }
            "circuit" -> { // gold traces on the body
                line.color = a(gold, 200); line.strokeWidth = r * 0.05f
                c.drawLine(cx - r * 0.85f, cy - r * 0.05f, cx - r * 0.55f, cy - r * 0.05f, line); c.drawLine(cx + r * 0.55f, cy - r * 0.05f, cx + r * 0.85f, cy - r * 0.05f, line)
                fill.color = gold; c.drawCircle(cx - r * 0.55f, cy - r * 0.05f, r * 0.06f, fill); c.drawCircle(cx + r * 0.55f, cy - r * 0.05f, r * 0.06f, fill)
            }
            "circus" -> { // ringmaster's striped hat
                fill.color = w.accent
                c.drawRect(cx - r * 0.42f, cy - r * 1.5f, cx + r * 0.42f, cy - r * 0.82f, fill)
                fill.color = white
                c.drawRect(cx - r * 0.14f, cy - r * 1.5f, cx + r * 0.14f, cy - r * 0.82f, fill)
                fill.color = Color.parseColor("#1D6FB8"); rect.set(cx - r * 0.72f, cy - r * 0.92f, cx + r * 0.72f, cy - r * 0.72f); c.drawOval(rect, fill)
                fill.color = gold; c.drawRect(cx - r * 0.42f, cy - r * 1.0f, cx + r * 0.42f, cy - r * 0.9f, fill)
            }
            "disco" -> { // mirror-ball tiles + sparkle
                c.save(); path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(path)
                line.color = a(white, 70); line.strokeWidth = r * 0.035f
                var k = -r
                while (k <= r) { c.drawLine(cx - r, cy + k, cx + r, cy + k, line); c.drawLine(cx + k, cy - r, cx + k, cy + r, line); k += r * 0.25f }
                c.restore()
                fill.color = a(white, 240); star(c, cx + r * 0.75f, cy - r * 0.8f, r * 0.2f)
                fill.color = a(w.inColor, 240); star(c, cx - r * 0.9f, cy + r * 0.6f, r * 0.12f)
            }
            "fitness" -> { // sweatband + lightning bolt
                fill.color = w.accent; c.drawRect(cx - r * 0.95f, cy - r * 0.92f, cx + r * 0.95f, cy - r * 0.72f, fill)
                fill.color = a(ink, 230)
                path.reset(); path.moveTo(cx + r * 0.05f, cy - r * 0.92f); path.lineTo(cx - r * 0.1f, cy - r * 0.8f); path.lineTo(cx + r * 0.02f, cy - r * 0.8f); path.lineTo(cx - r * 0.06f, cy - r * 0.72f); path.lineTo(cx + r * 0.14f, cy - r * 0.84f); path.lineTo(cx + r * 0.02f, cy - r * 0.84f); path.close()
                c.drawPath(path, fill)
            }
            "tropical" -> { // toucan beak + hibiscus
                fill.color = Color.parseColor("#FFB020")
                path.reset(); path.moveTo(cx - r * 0.2f, cy - r * 0.3f); path.quadTo(cx + r * 0.9f, cy - r * 0.45f, cx + r * 1.15f, cy - r * 0.05f)
                path.quadTo(cx + r * 0.5f, cy + r * 0.05f, cx - r * 0.2f, cy - r * 0.05f); path.close(); c.drawPath(path, fill)
                fill.color = Color.parseColor("#E8452E"); c.drawCircle(cx + r * 1.08f, cy - r * 0.08f, r * 0.1f, fill)
                fill.color = Color.parseColor("#FF5C8A")
                for (k in 0 until 5) { val an = k * 2 * PI / 5; c.drawCircle(cx - r * 0.6f + (r * 0.18f * cos(an)).toFloat(), cy - r * 0.95f + (r * 0.18f * sin(an)).toFloat(), r * 0.16f, fill) }
                fill.color = Color.parseColor("#FFE066"); c.drawCircle(cx - r * 0.6f, cy - r * 0.95f, r * 0.07f, fill)
            }
            "groovy" -> { fill.color = Color.parseColor("#FFC93C"); c.drawCircle(cx + r * 0.55f, cy - r * 0.85f, r * 0.14f, fill) }
            "glass" -> { // stained-glass leading lines
                c.save(); path.reset(); path.addCircle(cx, cy, r, Path.Direction.CW); c.clipPath(path)
                line.color = a(w.fg, 120); line.strokeWidth = r * 0.06f
                c.drawLine(cx - r, cy - r * 0.2f, cx + r, cy - r * 0.05f, line); c.drawLine(cx - r * 0.3f, cy - r, cx - r * 0.05f, cy - r * 0.15f, line); c.drawLine(cx + r * 0.35f, cy - r, cx + r * 0.1f, cy - r * 0.1f, line)
                c.restore()
                line.color = a(w.fg, 150); line.strokeWidth = r * 0.06f; c.drawCircle(cx, cy, r, line)
            }
            "pride" -> {}
        }
    }

    // ───────── little shapes ─────────

    private fun glasses(c: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        line.color = col; line.strokeWidth = r * 0.07f
        c.drawCircle(cx - r * 0.36f, cy - r * 0.42f, r * 0.27f, line); c.drawCircle(cx + r * 0.36f, cy - r * 0.42f, r * 0.27f, line)
        c.drawLine(cx - r * 0.1f, cy - r * 0.45f, cx + r * 0.1f, cy - r * 0.45f, line)
    }

    private fun bow(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y); path.lineTo(x - s * 1.3f, y - s * 0.75f); path.lineTo(x - s * 1.3f, y + s * 0.75f); path.close()
        path.moveTo(x, y); path.lineTo(x + s * 1.3f, y - s * 0.75f); path.lineTo(x + s * 1.3f, y + s * 0.75f); path.close()
        c.drawPath(path, fill)
        val keep = fill.color; fill.color = World.mix(keep, Color.BLACK, 0.2f); c.drawCircle(x, y, s * 0.35f, fill); fill.color = keep
    }

    private fun star(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y - s); path.quadTo(x, y, x + s, y); path.quadTo(x, y, x, y + s); path.quadTo(x, y, x - s, y); path.quadTo(x, y, x, y - s); path.close()
        c.drawPath(path, fill)
    }

    private fun heart(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset(); path.moveTo(x, y + s * 0.8f)
        path.cubicTo(x - s * 1.3f, y - s * 0.1f, x - s * 0.6f, y - s * 1.1f, x, y - s * 0.35f)
        path.cubicTo(x + s * 0.6f, y - s * 1.1f, x + s * 1.3f, y - s * 0.1f, x, y + s * 0.8f); path.close()
        c.drawPath(path, fill)
    }

    private fun leaf(c: Canvas, x: Float, y: Float, s: Float, col: Int) {
        fill.color = col
        path.reset(); path.moveTo(x - s, y + s * 0.3f); path.quadTo(x - s * 0.2f, y - s * 0.9f, x + s, y - s * 0.3f); path.quadTo(x, y + s * 0.6f, x - s, y + s * 0.3f); path.close()
        c.drawPath(path, fill)
    }

    private fun hexagon(c: Canvas, x: Float, y: Float, s: Float) {
        path.reset()
        for (k in 0 until 6) { val an = k * PI / 3; val px = x + (s * cos(an)).toFloat(); val py = y + (s * sin(an)).toFloat(); if (k == 0) path.moveTo(px, py) else path.lineTo(px, py) }
        path.close(); c.drawPath(path, line)
    }
}
